/*
 * Copyright (c) 2016 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.newts;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opennms.newts.cassandra.NewtsInstance;

import com.google.common.collect.Lists;

/**
 * Verifies that records can be written to and read from an
 * a running instance of Cassandra with the Newts schema.
 *
 * @author Jesse White (jesse@opennms.org)
 **/
public class TSDRNewtsPersistenceServiceImplIT {
    @Rule
    public NewtsInstance newtsInstance = new NewtsInstance();

    private TSDRNewtsPersistenceServiceImpl impl;

    @Before
    public void setUp() throws Exception {
        NewtsConfig newtsConfig = new NewtsConfig();
        newtsConfig.setHost(newtsInstance.getHost());
        newtsConfig.setPort(newtsInstance.getPort());
        newtsConfig.setKeyspace(newtsInstance.getKeyspace());
        impl = new TSDRNewtsPersistenceServiceImpl(newtsConfig);
    }

    @Test
    public void canStoreAndGetMetrics() throws InterruptedException {
        // Create a new record
        Long now = System.currentTimeMillis();
        TSDRMetricRecord metricRecord = TSDRNewtsPersistenceServiceImplTest.createMetricRecord(now);

        // Verify that there are no existing records
        String metricKey = FormatUtil.getTSDRMetricKey(metricRecord);
        assertEquals(0, impl.getTSDRMetricRecords(metricKey, now - 1000, now + 1000).size());

        // Store the record
        impl.store(metricRecord);

        // Retrieve the records
        List<TSDRMetricRecord> expectedMetricRecords =  Lists.newArrayList(metricRecord);
        // The records may not be immediately flushed, so we poll until they are found
        await().atMost(2, MINUTES).pollInterval(5, SECONDS)
            .until(getTSDRMetricRecords(metricKey, now - 1000, now + 1000), is(expectedMetricRecords));
    }

    public Callable<List<TSDRMetricRecord>> getTSDRMetricRecords(final String metricKey, final long startTime, final long endTime) {
        return new Callable<List<TSDRMetricRecord>>() {
            @Override
            public List<TSDRMetricRecord> call() throws Exception {
                return impl.getTSDRMetricRecords(metricKey, startTime, endTime);
            }
        };
    }
}
