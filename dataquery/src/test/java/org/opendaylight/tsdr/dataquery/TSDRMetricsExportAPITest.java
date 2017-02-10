/*
 * Copyright (c) 2017 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.dataquery;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.opendaylight.controller.config.yang.config.TSDR_dataquery.impl.TSDRDataqueryModule;
import org.opendaylight.tsdr.dataquery.rest.query.TSDRMetricsExportAPI;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.TsdrMetricDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdrmetrics.output.Metrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdrmetrics.output.MetricsBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

public class TSDRMetricsExportAPITest {

    @Test
    public void get() {
        // Create a metric with a record key
        final RecordKeys tableRecordKey = new RecordKeysBuilder()
                .setKeyName("Table")
                .setKeyValue("122")
                .build();
        final Metrics metric = new MetricsBuilder()
                .setMetricName("m")
                .setNodeID("openflow:7")
                .setTSDRDataCategory(DataCategory.FLOWTABLESTATS)
                .setTimeStamp(1486743322L)
                .setMetricValue(new BigDecimal(2.5))
                .setRecordKeys(Lists.newArrayList(tableRecordKey))
                .build();

        // Perform the GET
        final String response = getResponseForMetrics(metric);

        // Verify
        assertEquals("m{Category=\"FLOW_TABLE_STATS\",Node=\"openflow:7\",Table=\"122\"} 2.5 1486743322\n", response);
    }

    @Test
    public void getFailsWith503WhenRpcFails() throws ExecutionException, InterruptedException {
        final TsdrMetricDataService metricService = mock(TsdrMetricDataService.class, RETURNS_DEEP_STUBS);
        when(metricService.getTSDRMetrics(any()).get().isSuccessful()).thenReturn(false);
        TSDRDataqueryModule.metricDataService = metricService;

        TSDRMetricsExportAPI api = new TSDRMetricsExportAPI();
        Response response = api.get(null, null);
        assertEquals(503, response.getStatus());
    }

    private String getResponseForMetrics(Metrics... metrics) {
        // Mock the metric service
        final TsdrMetricDataService metricService = mock(TsdrMetricDataService.class, RETURNS_DEEP_STUBS);
        try {
            when(metricService.getTSDRMetrics(any()).get().isSuccessful()).thenReturn(true);
            when(metricService.getTSDRMetrics(any()).get().getResult().getMetrics()).thenReturn(Lists.newArrayList(metrics));
        } catch (InterruptedException | ExecutionException e) {
            throw Throwables.propagate(e);
        }
        TSDRDataqueryModule.metricDataService = metricService;

        // Issue a simple GET
        TSDRMetricsExportAPI api = new TSDRMetricsExportAPI();
        Response response;
        try {
            response = api.get(null, null);
        } catch (InterruptedException | ExecutionException e) {
            throw Throwables.propagate(e);
        }

        // Validate the response headers
        assertEquals(200, response.getStatus());
        assertEquals(TSDRMetricsExportAPI.CONTENT_TYPE, ((MediaType)response.getMetadata()
                .getFirst("Content-type")).toString());
        // Return the body
        return (String)response.getEntity();
    }

    @Test
    public void metricsWithInvalidNamesAreIgnored() throws InterruptedException, ExecutionException {
        assertEquals("", getResponseForMetrics(new MetricsBuilder()
                .setMetricName("#$%#")
                .setTimeStamp(1486743322L)
                .setMetricValue(new BigDecimal(2.5))
                .build()));
    }

    @Test
    public void attributesWithInvalidNamesAreIgnored() {
        // Create a metric with a record key
        final RecordKeys tableRecordKey = new RecordKeysBuilder()
                .setKeyName("Table")
                // Here we also add special characters to the value, to make sure that
                // these are properly escaped
                .setKeyValue("\"12'")
                .build();
        final RecordKeys ignoredRecordKey = new RecordKeysBuilder()
                .setKeyName(":#!")
                .setKeyValue("ignored")
                .build();
        final Metrics metric = new MetricsBuilder()
                .setMetricName("m")
                .setNodeID("openflow:7")
                .setTSDRDataCategory(DataCategory.FLOWTABLESTATS)
                .setTimeStamp(1486743322L)
                .setMetricValue(new BigDecimal(2.5))
                .setRecordKeys(Lists.newArrayList(tableRecordKey, ignoredRecordKey))
                .build();
        assertEquals("m{Category=\"FLOW_TABLE_STATS\",Node=\"openflow:7\",Table=\"\\\"12\\u0027\"} 2.5 1486743322\n",
                getResponseForMetrics(metric));
    }

    @Test
    public void toPrometheusMetricValue() {
        // Whole number
        assertMetricValueEquals("42", new MetricsBuilder()
                .setMetricValue(new BigDecimal(42))
                .build());

        // Floating point
        assertMetricValueEquals("6.67408", new MetricsBuilder()
                .setMetricValue(new BigDecimal(6.67408))
                .build());

        // Nan
        assertMetricValueEquals(TSDRMetricsExportAPI.NAN_VALUE, new MetricsBuilder()
                .build());

        // +Inf
        assertMetricValueEquals(TSDRMetricsExportAPI.POSITIVE_INF_VALUE, new MetricsBuilder()
                .setMetricValue(new BigDecimal(Double.MAX_VALUE).add(new BigDecimal(Double.MAX_VALUE)))
                .build());

        // -Inf
        assertMetricValueEquals(TSDRMetricsExportAPI.NEGATIVE_INF_VALUE, new MetricsBuilder()
                .setMetricValue(new BigDecimal(-Double.MAX_VALUE).add(new BigDecimal(-Double.MAX_VALUE)))
                .build());
    }

    private static void assertMetricValueEquals(String expectedValueString, Metrics metric) {
        assertEquals(expectedValueString, TSDRMetricsExportAPI.toPrometheusMetricValue(metric));
    }

    @Test
    public void toPrometheusMetric() {
        // Metric with no time-stamp
        assertMetricEquals("x 1\n", new MetricsBuilder()
                .setMetricName("x")
                .setMetricValue(new BigDecimal(1))
                .build());

        // Metric with time-stamp
        assertMetricEquals("y 2.5 1486743320\n", new MetricsBuilder()
                .setMetricName("y")
                .setTimeStamp(1486743320L)
                .setMetricValue(new BigDecimal(2.5))
                .build());

        // Metric with node id, category and record keys
        RecordKeys nodeRecordKey = new RecordKeysBuilder()
                .setKeyName("Node")
                .setKeyValue("openflow:7")
                .build();

        RecordKeys tableRecordKey = new RecordKeysBuilder()
                .setKeyName("Table")
                .setKeyValue("122")
                .build();

        assertMetricEquals("z{Category=\"FLOW_TABLE_STATS\",Node=\"openflow:7\",Table=\"122\"} 1.3 1486743321\n", new MetricsBuilder()
                .setMetricName("z")
                .setNodeID("openflow:7")
                .setTSDRDataCategory(DataCategory.FLOWTABLESTATS)
                .setTimeStamp(1486743321L)
                .setMetricValue(new BigDecimal(1.3))
                .setRecordKeys(Lists.newArrayList(nodeRecordKey, tableRecordKey))
                .build());   
    }

    private static void assertMetricEquals(String expectedString, Metrics metric) {
        assertEquals(expectedString, TSDRMetricsExportAPI.toPrometheusMetric(metric));
    }
}
