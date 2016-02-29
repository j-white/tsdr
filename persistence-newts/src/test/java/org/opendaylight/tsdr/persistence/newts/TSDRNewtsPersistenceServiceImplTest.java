/*
 * Copyright (c) 2016 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.newts;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Sample;

/**
 * @author Jesse White (jesse@opennms.org)
 **/
public class TSDRNewtsPersistenceServiceImplTest {

    private TSDRNewtsPersistenceServiceImpl impl = new TSDRNewtsPersistenceServiceImpl(new NewtsConfig(), false);

    @Test
    public void testGetNewtsResourceId() {
        TSDRMetricRecord m = createMetricRecord();
        assertEquals("test_1:EXTERNAL:_1", impl.getNewtsResourceId(m));
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

    private void checkType(DataCategory cat, String metric, MetricType type) {
        assertEquals(type, impl.getMetricType(cat, metric));
    }

    @Test
    public void canConvertToAndFromSample() {
        // Create a metric record
        TSDRMetricRecord m = createMetricRecord();
        // Convert it to a sample
        Sample s = impl.toSample(m);
        // Convert the sample back to a record
        TSDRMetricRecord mm = impl.toMetricRecord(s.getResource(), s);
        // Verify
        assertEquals(m, mm);
    }

    public static TSDRMetricRecord createMetricRecord() {
        return createMetricRecord(System.currentTimeMillis());
    }

    public static TSDRMetricRecord createMetricRecord(Long timeStamp){
        TSDRMetricRecordBuilder b = new TSDRMetricRecordBuilder();
        b.setNodeID("test:1");
        b.setTimeStamp(timeStamp);
        b.setMetricName("Test");
        b.setMetricValue(new BigDecimal(11D));
        b.setTSDRDataCategory(DataCategory.EXTERNAL);
        List<RecordKeys> recs = new ArrayList<>();
        RecordKeysBuilder rb = new RecordKeysBuilder();
        rb.setKeyValue("Test1");
        rb.setKeyName("Test2");
        recs.add(rb.build());
        b.setRecordKeys(recs);
        return b.build();
    }
}
