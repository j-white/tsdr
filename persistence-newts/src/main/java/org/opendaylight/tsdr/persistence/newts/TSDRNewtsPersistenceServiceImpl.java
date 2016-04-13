/*
 * Copyright (c) 2016 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.newts;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.tsdr.spi.persistence.TSDRMetricPersistenceService;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.RecordKey;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opennms.newts.api.Context;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleProcessor;
import org.opennms.newts.api.SampleProcessorService;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;
import org.opennms.newts.api.search.BooleanQuery;
import org.opennms.newts.api.search.Operator;
import org.opennms.newts.api.search.SearchResults;
import org.opennms.newts.api.search.SearchResults.Result;
import org.opennms.newts.api.search.Term;
import org.opennms.newts.api.search.TermQuery;
import org.opennms.newts.cassandra.CassandraSession;
import org.opennms.newts.cassandra.CassandraSessionImpl;
import org.opennms.newts.cassandra.ContextConfigurations;
import org.opennms.newts.cassandra.search.CassandraIndexer;
import org.opennms.newts.cassandra.search.CassandraIndexerSampleProcessor;
import org.opennms.newts.cassandra.search.CassandraSearcher;
import org.opennms.newts.cassandra.search.EscapableResourceIdSplitter;
import org.opennms.newts.cassandra.search.GuavaResourceMetadataCache;
import org.opennms.newts.cassandra.search.ResourceMetadataCache;
import org.opennms.newts.persistence.cassandra.CassandraSampleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * TSDR Persistence Strategy for Newts
 *
 * The current implementation of the strategy stores samples
 * the same way that OpenNMS does, so we can take advantage
 * of the graphing tools available there.
 *
 * In particular, OpenNMS stores samples in resources named like:
 *   snmp:fs:NODES:ny-cassandra-1:dskIndex:dev-shm:net-snmp-disk
 *
 * A sample TSDRMetricRecord object looks like:
 *   nodeId[openflow:7]
 *   dataCategory[FLOWTABLESTATS],
 *   metricName[PacketLookup]
 *   metricValue=[0]
 *   timestamp[1456416295101],
 *   recordKeys[Node=openflow:7,Table=122]
 *
 * So, we use the following format for resource IDs:
 *   ${prefix}${nodeId}:${dataCategory}:${dataCategorySpecificIndex}${suffix}
 *
 * where prefix = "snmp:fs:NODES:" and suffix = ":stats"
 *
 * Current issues include:
 *
 * A) Need to differentiate between counters and gauges - see getMetricType()
 *
 *     Possible workaround: Store everything as Gauges and calculate the rates on demand.
 *
 * B) Need to add a category specific index - see getResourceIndex()
 *
 *     Possible workaround: Add support for driving the resource id calculation using a script
 *     Possible workaround: Add all of the record keys, and use a regular expression in the
 *                          resource type to parse them
 * !!Possible workaround!!: Add the record keys as "string attributes" and reference
 *                          those in the resource labels i.e. resourceLabel="${diskIODevice}"
 *
 * @author Jesse White (jesse@opennms.org)
 **/
public class TSDRNewtsPersistenceServiceImpl implements TSDRMetricPersistenceService {
    private static final Logger LOG = LoggerFactory.getLogger(TSDRNewtsPersistenceServiceImpl.class);

    private static final EscapableResourceIdSplitter splitter = new EscapableResourceIdSplitter();

    private final NewtsConfig newtsConfig;

    private CassandraSearcher searcher;
    private CassandraSampleRepository sampleRepository;

    public TSDRNewtsPersistenceServiceImpl() {
        this(new NewtsConfig(), true);
    }

    public TSDRNewtsPersistenceServiceImpl(NewtsConfig newtsConfig, boolean start) {
        this.newtsConfig = Objects.requireNonNull(newtsConfig);

        if (!start) {
            return;
        }
        
        MetricRegistry registry = new MetricRegistry();
        ContextConfigurations contextConfigurations = new ContextConfigurations();
        CassandraSession session = new CassandraSessionImpl(
                newtsConfig.getKeyspace(),
                newtsConfig.getHost(),
                newtsConfig.getPort(),
                newtsConfig.getCompression(),
                newtsConfig.getUser(),
                newtsConfig.getPassword(),
                newtsConfig.getSsl());
        ResourceMetadataCache cache = new GuavaResourceMetadataCache(newtsConfig.getCacheSize(), registry);
        CassandraIndexer indexer = new CassandraIndexer(
                session,
                newtsConfig.getTTL(),
                cache,
                registry,
                newtsConfig.isHierarchicalIndexingEnabled(),
                new EscapableResourceIdSplitter(),
                contextConfigurations);
        searcher = new CassandraSearcher(session, registry, contextConfigurations);
        SampleProcessor indexerProcessor = new CassandraIndexerSampleProcessor(indexer);
        SampleProcessorService sampleProcessorService = new SampleProcessorService(4, Sets.newHashSet(indexerProcessor));
        sampleRepository = new CassandraSampleRepository(
                session,
                newtsConfig.getTTL(),
                registry,
                sampleProcessorService,
                contextConfigurations);
        
        LOG.info("Initialized Newts store with keyspace={}, host={} and port={}.",
                newtsConfig.getKeyspace(), newtsConfig.getHost(), newtsConfig.getPort());
    }

    @Override
    public void storeMetric(TSDRMetricRecord metricRecord) {
        storeMetric(Lists.newArrayList(metricRecord));
    }

    @Override
    public void storeMetric(List<TSDRMetricRecord> recordList) {
        List<Sample> samples = Lists.newArrayList();
        for (TSDRMetricRecord record : recordList) {
            samples.add(toSample(record));
         }
        sampleRepository.insert(samples);
    }

    @Override
    public void purge(long retentionTime) {
        for (DataCategory dataCategory : DataCategory.values()) {
            purge(dataCategory, retentionTime);
        }
    }

    @Override
    public void purge(DataCategory category, long retentionTime) {
        LOG.info("Purging records with category {} earlier than {}.", category.name(), new Date(retentionTime));
        // TODO: Search for category, and delete
    }

    @Override
    public List<TSDRMetricRecord> getTSDRMetricRecords(String tsdrMetricKey, long startTime, long endTime) {
        LOG.info("getTSDRMetricRecords({}, {}, {})", tsdrMetricKey, startTime, endTime);

        // Find all of the resources tagged with the given metric key
        TermQuery tq = new TermQuery(new Term("metricKey", tsdrMetricKey));
        BooleanQuery q = new BooleanQuery();
        q.add(tq, Operator.OR);

        Set<Resource> resources = Sets.newHashSet();
        SearchResults searchResults = searcher.search(newtsConfig.getContext(), q, true);
        for (Result searchResult : searchResults) {
            resources.add(searchResult.getResource());
        }

        // Now gather the samples from the resources we found, and convert the samples to metric records
        List<TSDRMetricRecord> metricRecords = Lists.newArrayList();
        for (Resource resource : resources) {
            Timestamp start = Timestamp.fromEpochMillis(startTime);
            Timestamp end = Timestamp.fromEpochMillis(endTime);
            Results<Sample> results = sampleRepository.select(Context.DEFAULT_CONTEXT, resource, Optional.of(start), Optional.of(end));
            for (Row<Sample> row : results.getRows()) {
                for (Sample sample : row.getElements()) {
                    metricRecords.add(toMetricRecord(resource, sample));
                }
            }
            if (metricRecords.size() >= TSDRConstants.MAX_RESULTS_FROM_LIST_METRICS_COMMAND) {
                // Don't bother fetching more samples if we have already reached the limit
                break;
            }
        }

        // Return the records, limiting the list in size
        return metricRecords.subList(0, Math.min(TSDRConstants.MAX_RESULTS_FROM_LIST_METRICS_COMMAND, metricRecords.size()));
    }

    @VisibleForTesting
    protected Sample toSample(TSDRMetricRecord m) {
        // Calculate the Newts Resource ID
        String resourceId = getNewtsResourceId(m);

        // Determine the metric type
        MetricType metricType = getMetricType(m.getTSDRDataCategory(), m.getMetricName());

        // Add the metric key as a resource attribute
        Map<String, String> attributes = Maps.newHashMap();
        attributes.put("metricKey", FormatUtil.getTSDRMetricKey(m));
        if (newtsConfig.isOpenNMSIndexingEnabled()) {
            // Add additional indices used by OpenNMS
            addIndicesToAttributes(resourceId, attributes);
        }

        // Create the resource and sample
        Resource resource = new Resource(resourceId, Optional.of(attributes));
        return new Sample(
                Timestamp.fromEpochMillis(m.getTimeStamp()),
                newtsConfig.getContext(),
                resource,
                m.getMetricName(),
                metricType,
                ValueType.compose(m.getMetricValue(), metricType));
    }

    @VisibleForTesting
    protected TSDRMetricRecord toMetricRecord(Resource r, Sample s) {
        Map<String, String> attributes = r.getAttributes().or(new HashMap<String, String>(0));
        String metricKey = attributes.get("metricKey");
        TSDRMetricRecordBuilder b = new TSDRMetricRecordBuilder();
        b.setNodeID(FormatUtil.getNodeIdFromTSDRKey(metricKey));
        b.setTimeStamp(s.getTimestamp().asMillis());
        b.setMetricName(s.getName());
        b.setMetricValue(new BigDecimal(s.getValue().doubleValue()));
        b.setTSDRDataCategory(DataCategory.valueOf(FormatUtil.getDataCategoryFromTSDRKey(metricKey)));
        b.setRecordKeys(FormatUtil.getRecordKeysFromTSDRKey(metricKey));
        return b.build();
    }

    /*
     * Extends the attribute map with indices used by the {@link org.opennms.netmgt.dao.support.NewtsResourceStorageDao}.
     *
     * A resource path of the form [a, b, c, d] will be indexed with:
     * <ul>
     * <li> _idx1: (a, 4)
     * <li> _idx2: (a:b, 4)
     * <li> _idx3: (a:b:c, 4)
     */
    private static void addIndicesToAttributes(String resourceId, Map<String, String> attributes) {
        final List<String> els = splitter.splitIdIntoElements(resourceId);
        final int N = els.size();
        for (int i = 0; i < N; i++) {
            final String id = splitter.joinElementsToId(els.subList(0, i+1));
            attributes.put("_idx" + i, String.format("(%s,%d)", id, N));
        }
    }

    @VisibleForTesting
    protected static String getResourceIndex(DataCategory dataCategory, List<RecordKeys> recordKeys) {
        Map<String, String> indexedRecordKeys = Maps.newHashMap();
        for (RecordKey recordKey : recordKeys) {
            indexedRecordKeys.put(recordKey.getKeyName(), recordKey.getKeyValue());
        }

        switch (dataCategory) {
        case FLOWSTATS:
            return String.format("%s_%s", indexedRecordKeys.get("Table"), indexedRecordKeys.get("Flow"));
        case FLOWTABLESTATS:
            return indexedRecordKeys.get("Table");
        case PORTSTATS:
            return indexedRecordKeys.get("NodeConnector");       
        default:
            LOG.warn("Unsupported data category {} with record keys: {}", dataCategory, recordKeys);
            return "_1";
        }
    }

    @VisibleForTesting
    protected String getNewtsResourceId(TSDRMetricRecord m) {
        List<String> pathElements = Lists.newArrayList();
        pathElements.add(m.getNodeID().replace(":", "_")); // OpenNMS chokes on ':'
        pathElements.add(m.getTSDRDataCategory().toString());
        pathElements.add(getResourceIndex(m.getTSDRDataCategory(), m.getRecordKeys()));
        return  newtsConfig.getResourcePrefix() + splitter.joinElementsToId(pathElements) + newtsConfig.getResourceSuffix();
    }

    @VisibleForTesting
    @SuppressWarnings("incomplete-switch")
    protected MetricType getMetricType(DataCategory dataCategory, String metricName) {
        switch (dataCategory) {
        case FLOWSTATS:
            return MetricType.COUNTER;
        case FLOWTABLESTATS:
            switch (metricName) {
            case "ActiveFlows": return MetricType.GAUGE;
            case "PacketLookup": return MetricType.COUNTER;
            case "PacketMatch": return MetricType.COUNTER;
            }
        case PORTSTATS:
            return MetricType.COUNTER;
        }
        return MetricType.GAUGE;
    }
}
