/*
 * Copyright (c) 2016 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.newts;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.tsdr.spi.persistence.TsdrPersistenceService;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.tsdr.spi.util.TsdrPersistenceServiceUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.RecordKey;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
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
 * @author Jesse White (jesse@opennms.org)
 **/
public class TSDRNewtsPersistenceServiceImpl implements TsdrPersistenceService {
    private static final Logger LOG = LoggerFactory.getLogger(TSDRNewtsPersistenceServiceImpl.class);

    // TODO: Make this configurable
    private static final String RESOURCE_PREFIX = "snmp:fs:NODES:";
    private static final String RESOURCE_SUFFIX = ":stats";
    private static final Context context = Context.DEFAULT_CONTEXT;
    private final String keyspace;
    private final String host;
    private final int port;

    private static final EscapableResourceIdSplitter splitter = new EscapableResourceIdSplitter();
    private CassandraSearcher searcher;
    private CassandraSampleRepository sampleRepository;
    private CassandraSession session;

    public TSDRNewtsPersistenceServiceImpl() {
        String envHost = System.getenv("NEWTS_HOST");
        String envPort = System.getenv("NEWTS_PORT");
        if (envHost != null) {
            this.host = envHost;
        } else {
            this.host = "127.0.0.1";
        }

        if (envPort != null) {
            this.port = Integer.valueOf(envPort);
        } else {
            this.port = 9042;
        }

        this.keyspace = "newts";
        register();
    }

    public TSDRNewtsPersistenceServiceImpl(String keyspace, String host, int port) {
        this.keyspace = Objects.requireNonNull(keyspace, "keyspace");
        this.host = Objects.requireNonNull(host, "localhost");
        this.port = port;
        register();
    }

    private void register() {
        TsdrPersistenceServiceUtil.setTsdrPersistenceService(this);
        LOG.info("Initialized Newts store with keyspace={}, host={} and port={}.",
                keyspace, host, port);
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
    public static void addIndicesToAttributes(String resourceId, Map<String, String> attributes) {
        final List<String> els = splitter.splitIdIntoElements(resourceId);
        final int N = els.size();
        for (int i = 0; i < N; i++) {
            final String id = splitter.joinElementsToId(els.subList(0, i+1));
            attributes.put("_idx" + i, String.format("(%s,%d)", id, N));
        }
    }

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
    protected static String getNewtsResourceId(TSDRMetricRecord m) {
        List<String> pathElements = Lists.newArrayList();
        pathElements.add(m.getNodeID().replace(":", "_")); // OpenNMS chokes on ':'
        pathElements.add(m.getTSDRDataCategory().toString());
        pathElements.add(getResourceIndex(m.getTSDRDataCategory(), m.getRecordKeys()));

        return RESOURCE_PREFIX + splitter.joinElementsToId(pathElements) + RESOURCE_SUFFIX;
    }

    @VisibleForTesting
    @SuppressWarnings("incomplete-switch")
    protected static MetricType getMetricType(DataCategory dataCategory, String metricName) {
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

    /*
     * 
     * OpenNMS stores samples in resources named like:
     *   snmp:fs:NODES:ny-cassandra-1:dskIndex:dev-shm:net-snmp-disk
     *
     * Sample TSDRMetricRecord object:
     *   nodeId[openflow:7]
     *   dataCategory[FLOWTABLESTATS],
     *   metricName[PacketLookup]
     *   metricValue=[0]
     *   timestamp[1456416295101],
     *   recordKeys[Node=openflow:7,Table=122]
     *
     * Resource ID format:
     *   ${prefix}${nodeId}:${dataCategory}:${dataCategorySpecificIndex}${suffix}
     *
     * Where:
     *    prefix = "snmp:fs:NODES:"
     *
     */
    @VisibleForTesting
    protected static Sample toSample(TSDRMetricRecord m) {
        // Calculate the Newts Resource ID
        String resourceId = getNewtsResourceId(m);

        // Determine the metric type
        MetricType metricType = getMetricType(m.getTSDRDataCategory(), m.getMetricName());

        // Add the metric key as a resource attribute
        Map<String, String> attributes = Maps.newHashMap();
        attributes.put("metricKey", FormatUtil.getTSDRMetricKey(m));
        // Add additional indices used by OpenNMS
        addIndicesToAttributes(resourceId, attributes);

        // Create the resource and sample
        Resource resource = new Resource(resourceId, Optional.of(attributes));
        return new Sample(
                Timestamp.fromEpochMillis(m.getTimeStamp()),
                context,
                resource,
                m.getMetricName(),
                metricType,
                ValueType.compose(m.getMetricValue(), metricType));
    }

    @VisibleForTesting
    protected static TSDRMetricRecord toMetricRecord(Resource r, Sample s) {
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

    @Override
    public void store(TSDRMetricRecord m) {
        sampleRepository.insert(Collections.singleton(toSample(m)));
    }

    @Override
    public void store(TSDRLogRecord m) {
        // Persisting log records is not currently supported by Newts, silently ignore these requests
    }

    @Override
    public void store(List<TSDRRecord> metricRecordList) {
        List<Sample> samples = Lists.newArrayList();
        for (TSDRRecord record : metricRecordList) {
            if (record instanceof TSDRMetricRecord) {
                samples.add(toSample((TSDRMetricRecord)record));
                store((TSDRMetricRecord)record);
            }
            // Silently ignore other types of records
         }
        sampleRepository.insert(samples);
    }

    @Override
    public void start(int timeout) {
        MetricRegistry registry = new MetricRegistry();
        ContextConfigurations contextConfigurations = new ContextConfigurations();

        CassandraSession session = new CassandraSession(
                keyspace,
                host,
                port,
                "NONE",
                "admin",
                "admin");
        ResourceMetadataCache cache = new GuavaResourceMetadataCache(8096, registry);
        CassandraIndexer indexer = new CassandraIndexer(
                session,
                86400,
                cache,
                registry,
                false,
                new EscapableResourceIdSplitter(),
                contextConfigurations);
        searcher = new CassandraSearcher(session, registry, contextConfigurations);
        SampleProcessor indexerProcessor = new CassandraIndexerSampleProcessor(indexer);
        SampleProcessorService sampleProcessorService = new SampleProcessorService(4, Sets.newHashSet(indexerProcessor));
        sampleRepository = new CassandraSampleRepository(
                session,
                7*24*60*60*1000,
                registry,
                sampleProcessorService,
                contextConfigurations);
    }
 
    @Override
    public void stop(int timeout) {
        try {
            session.shutdown().get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Failed to shutdown Cassandra session.", e);
        }
        session = null;
        searcher = null;
        sampleRepository = null;
    }

    @Override
    public void purgeTSDRRecords(DataCategory category, Long retentionTime){
        LOG.info("Purging records with category {} earlier than {}.", category.name(), new Date(retentionTime));
    }

    @Override
    public void purgeAllTSDRRecords(Long retentionTime) {
        for (DataCategory dataCategory : DataCategory.values()) {
            purgeTSDRRecords(dataCategory, retentionTime);
        }
    }

    @Override
    public List<TSDRMetricRecord> getTSDRMetricRecords(String tsdrMetricKey, long startTime, long endTime) {
        LOG.info("getTSDRMetricRecords({}, {}, {})", tsdrMetricKey, startTime, endTime);

        // Find all of the resources tagged with the given metric key
        TermQuery tq = new TermQuery(new Term("metricKey", tsdrMetricKey));
        BooleanQuery q = new BooleanQuery();
        q.add(tq, Operator.OR);

        Set<Resource> resources = Sets.newHashSet();
        SearchResults searchResults = searcher.search(context, q, true);
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

    @Override
    public List<TSDRLogRecord> getTSDRLogRecords(String tsdrMetricKey, long startTime, long endTime) {
        return Collections.emptyList();
    }
}
