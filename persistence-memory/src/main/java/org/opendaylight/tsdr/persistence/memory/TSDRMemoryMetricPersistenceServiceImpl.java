/*
 * Copyright (c) 2017 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.memory;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.opendaylight.tsdr.spi.persistence.TSDRMetricPersistenceService;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;

/**
 * A "persistence" store used to keep the last metric for every key in memory.
 *
 * When used in conjunction with the '/metrics-export' REST end-point, this allows
 * external systems to scrape the metrics and use their own graphing, alerting
 * and storage mechanisms.
 *
 * @author Jesse White (jesse@opennms.ca)
 */
public class TSDRMemoryMetricPersistenceServiceImpl implements TSDRMetricPersistenceService {

    private final Map<String, TSDRMetricRecord> metricsByKey = new ConcurrentHashMap<>();
 
    public static final String ALL_METRIC_RECORDS_KEY = null;

    @Override
    public void storeMetric(TSDRMetricRecord metricRecord) {
        if (metricRecord.getTimeStamp() == null) {
            // Ignore records with no timestamp
            return;
        }
        final String metricKey = FormatUtil.getTSDRMetricKey(metricRecord);
        final TSDRMetricRecord existingMetricRecord = metricsByKey.get(metricKey);
        // Only store the record if it's more recent then the existing record
        if (existingMetricRecord == null
                || metricRecord.getTimeStamp() >= existingMetricRecord.getTimeStamp()) {
            metricsByKey.put(metricKey, metricRecord);
        }
    }

    @Override
    public void storeMetric(List<TSDRMetricRecord> recordList) {
        recordList.stream().forEach(record -> storeMetric(record));
    }

    @Override
    public List<TSDRMetricRecord> getTSDRMetricRecords(String tsdrMetricKey, long startDateTime, long endDateTime) {
        final Collection<TSDRMetricRecord> candidates;
        if (Objects.equals(ALL_METRIC_RECORDS_KEY, tsdrMetricKey)) {
            // If no key is set, use all known records
            candidates = metricsByKey.values();
        } else {
            final TSDRMetricRecord metricRecord = metricsByKey.get(tsdrMetricKey);
            if (metricRecord != null) {
                candidates = Collections.singleton(metricRecord);
            } else {
                candidates = Collections.emptySet();
            }
        }
        return candidates.stream()
                    .filter(m -> m.getTimeStamp() >= startDateTime)
                    .filter(m -> m.getTimeStamp() <= endDateTime)
                    .collect(Collectors.toList());
    }

    @Override
    public void purge(long timestamp) {
        purge(m -> m.getTimeStamp() <= timestamp);
    }

    @Override
    public void purge(DataCategory category, long timestamp) {
        purge(m -> Objects.equals(m.getTSDRDataCategory(), category),
              m -> m.getTimeStamp() <= timestamp);
    }

    @SafeVarargs
    private final void purge(Predicate<TSDRMetricRecord>... predicates) {
        final Iterator<Map.Entry<String, TSDRMetricRecord>> it = metricsByKey.entrySet().iterator();
        while (it.hasNext()) {
            final TSDRMetricRecord metricRecord = it.next().getValue();
            for (Predicate<TSDRMetricRecord> predicate : predicates) {
                if (!predicate.test(metricRecord)) {
                    continue;
                }
            }
            // All predicates passed, remove the value
            it.remove();
        } 
    }
}
