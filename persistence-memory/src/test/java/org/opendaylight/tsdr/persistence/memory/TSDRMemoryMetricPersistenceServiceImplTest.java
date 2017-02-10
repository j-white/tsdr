/*
 * Copyright (c) 2017 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.memory;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.tsdr.persistence.memory.TSDRMemoryMetricPersistenceServiceImpl.ALL_METRIC_RECORDS_KEY;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

import org.junit.Test;
import org.opendaylight.tsdr.spi.persistence.TSDRMetricPersistenceService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;

public class TSDRMemoryMetricPersistenceServiceImplTest {

    final TSDRMetricPersistenceService store = new TSDRMemoryMetricPersistenceServiceImpl();

    /**
     * Complete metric lifecycle test.
     */
    @Test
    public void storeGetAndPurge() {
        final long then = new Date().getTime();

        // Initially the store should be empty
        assertEquals(0, store.getTSDRMetricRecords(ALL_METRIC_RECORDS_KEY, 0, then).size());

        // Purge should run without any exception on empty store
        store.purge(then);
        store.purge(DataCategory.EXTERNAL, then);

        // Now store a metric
        TSDRMetricRecord metric = new TSDRMetricRecordBuilder()
                .setMetricName("x")
                .setMetricValue(new BigDecimal(42))
                .setTimeStamp(1L)
                .build();
        store.storeMetric(metric);

        // Retrieve that same metric we just stored
        assertEquals(metric, store.getTSDRMetricRecords(ALL_METRIC_RECORDS_KEY, 0, then).iterator().next());

        // Update the metric
        metric = new TSDRMetricRecordBuilder()
                .setMetricName("x")
                .setMetricValue(new BigDecimal(43))
                .setTimeStamp(2L)
                .build();
        store.storeMetric(Collections.singletonList(metric));

        // Retrieve that same (updated) metric we just stored
        assertEquals(metric, store.getTSDRMetricRecords(ALL_METRIC_RECORDS_KEY, 0, then).iterator().next());

        // The store should contain a single metric
        assertEquals(1, store.getTSDRMetricRecords(ALL_METRIC_RECORDS_KEY, 0, then).size());

        // Now purge, and we should be empty again
        store.purge(then);
        assertEquals(0, store.getTSDRMetricRecords(ALL_METRIC_RECORDS_KEY, 0, then).size());
    }

    /**
     * Verifies that metrics without timestamps (i.e. null values) are not kept
     * in the memory-based store.
     */
    @Test
    public void metricsWithoutTimestampAreIgnored() {
        // Initially the store should be empty
        assertEquals(0, store.getTSDRMetricRecords(ALL_METRIC_RECORDS_KEY, 0, System.currentTimeMillis()).size());

        // Attempt to store a metric without a timestamp
        TSDRMetricRecord metric = new TSDRMetricRecordBuilder()
                .setMetricName("x")
                .setMetricValue(new BigDecimal(43))
                .build();
        store.storeMetric(metric);

        // The store should remain empty
        assertEquals(0, store.getTSDRMetricRecords(ALL_METRIC_RECORDS_KEY, 0, System.currentTimeMillis()).size());

        // Attempt to store a metric with a timestamp
        metric = new TSDRMetricRecordBuilder()
                .setMetricName("y")
                .setMetricValue(new BigDecimal(44))
                .setTimeStamp(1L)
                .build();
        store.storeMetric(metric);

        // The store should contain a single metric
        assertEquals(1, store.getTSDRMetricRecords(ALL_METRIC_RECORDS_KEY, 0, System.currentTimeMillis()).size());
    }

}
