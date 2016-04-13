/*
 * Copyright (c) 2016 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.newts;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Sample;

/**
 * @author Jesse White (jesse@opennms.org)
 **/
public class TSDRNewtsPersistenceServiceImplUnitTest {

    @Test
    public void testGetNewtsResourceId() {
        long now = System.currentTimeMillis();
        TSDRMetricRecord m = TSDRNewtsPersistenceServiceImplTest.createMetricRecord(now);
        assertEquals("snmp:fs:NODES:test_1:EXTERNAL:_1:stats", TSDRNewtsPersistenceServiceImpl.getNewtsResourceId(m));
    }

    @Test
    public void testGetMetricType() {
        checkType(DataCategory.FLOWTABLESTATS, "ActiveFlows", MetricType.GAUGE);
        checkType(DataCategory.FLOWTABLESTATS, "PacketLookup", MetricType.COUNTER);
        checkType(DataCategory.FLOWTABLESTATS, "PacketMatch", MetricType.COUNTER);
        checkType(DataCategory.PORTSTATS, "*", MetricType.COUNTER);
        checkType(DataCategory.FLOWSTATS, "*", MetricType.COUNTER);

        // Should default to GAUGE for unknown types
        checkType(DataCategory.SYSLOG, "", MetricType.GAUGE);
    }

    private static void checkType(DataCategory cat, String metric, MetricType type) {
        assertEquals(type, TSDRNewtsPersistenceServiceImpl.getMetricType(cat, metric));
    }

    @Test
    public void canConvertToAndFromSample() {
        // Create a metric record
        long now = System.currentTimeMillis();
        TSDRMetricRecord m = TSDRNewtsPersistenceServiceImplTest.createMetricRecord(now);
        // Convert it to a sample
        Sample s = TSDRNewtsPersistenceServiceImpl.toSample(m);
        // Convert the sample back to a record
        TSDRMetricRecord mm = TSDRNewtsPersistenceServiceImpl.toMetricRecord(s.getResource(), s);
        // Verify
        assertEquals(m, mm);
    }
}
