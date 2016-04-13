/*
 * Copyright (c) 2016 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.newts;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.dataset.cql.FileCQLDataSet;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opennms.newts.cassandra.AbstractCassandraTestCase;
import org.opennms.newts.cassandra.Schema;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;

/**
 * FIXME: This should be an integration test.
 *
 * @author Jesse White (jesse@opennms.org)
 **/
public class TSDRNewtsPersistenceServiceImplTest extends AbstractCassandraTestCase {
    public static final int CASSANDRA_TTL = 86400;

    private TSDRNewtsPersistenceServiceImpl impl;

    @Before
    public void setUp() throws Exception {
        // Hack for libsigar-amd64-linux-1.6.4.so
        System.setProperty("java.library.path", "/home/jesse/Downloads/out");
        super.setUp();
        impl = new TSDRNewtsPersistenceServiceImpl(CASSANDRA_KEYSPACE, CASSANDRA_HOST, CASSANDRA_PORT);
    }

    @Test
    public void canStoreAndGetMetrics() throws InterruptedException {
        // Create a new record
        Long now = System.currentTimeMillis();
        TSDRMetricRecord metricRecord = createMetricRecord(now);

        // Verify that there are no existing records
        String metricKey = FormatUtil.getTSDRMetricKey(metricRecord);
        assertEquals(0, impl.getTSDRMetricRecords(metricKey, now - 1000, now + 1000).size());

        // Store the record
        impl.store(metricRecord);

        // TODO: Use awaitility
        // Wait for the samples to flush
        Thread.sleep(10000);

        // Retrieve the record and compare it to the original record
        List<TSDRMetricRecord> metricRecords = impl.getTSDRMetricRecords(metricKey, now - 1000, now + 1000);
        assertEquals(Lists.newArrayList(metricRecord), metricRecords);
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

    @Override
    protected String getSchemaResource() {
        throw new IllegalStateException();
    }

    @Override
    public CQLDataSet getDataSet() {
        try {
            final Schema searchSchema = new org.opennms.newts.cassandra.search.Schema();
            final Schema samplesSchema = new org.opennms.newts.persistence.cassandra.Schema();
            final List<Schema> schemas = Lists.newArrayList(searchSchema, samplesSchema);

            //  Concatenate the schema strings
            String schemasString = "";
            for (Schema schema : schemas) {
                schemasString += CharStreams.toString(new InputStreamReader(schema.getInputStream()));
            }

            // Replace the placeholders
            schemasString = schemasString.replace(KEYSPACE_PLACEHOLDER, CASSANDRA_KEYSPACE);

            // Split the resulting script back into lines
            String lines[] = schemasString.split("\\r?\\n");

            // Remove duplicate CREATE KEYSPACE statements;
            StringBuffer sb = new StringBuffer();
            boolean foundCreateKeyspace = false;
            boolean skipNextLine = false;
            for (String line : lines) {
                if (line.startsWith("CREATE KEYSPACE")) {
                    if (!foundCreateKeyspace) {
                        foundCreateKeyspace = true;
                        sb.append(line);
                        sb.append("\n");
                    } else {
                        skipNextLine = true;
                    }
                } else if (skipNextLine) {
                    skipNextLine = false;
                } else {
                    sb.append(line);
                    sb.append("\n");
                }
            }

            // Write the results to disk
            File schemaFile = File.createTempFile("schema-", ".cql", new File("target"));
            schemaFile.deleteOnExit();
            Files.write(sb.toString(), schemaFile, Charsets.UTF_8);
            return new FileCQLDataSet(schemaFile.getAbsolutePath(), false, true, CASSANDRA_KEYSPACE);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
