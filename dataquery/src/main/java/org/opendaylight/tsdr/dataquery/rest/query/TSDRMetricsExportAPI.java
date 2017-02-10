/*
 * Copyright (c) 2017 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.dataquery.rest.query;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.opendaylight.controller.config.yang.config.TSDR_dataquery.impl.TSDRDataqueryModule;
import org.opendaylight.tsdr.dataquery.rest.nbi.TSDRNBIRestAPI;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRMetricsInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRMetricsInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRMetricsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdrmetrics.output.Metrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Objects;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Exposes the metrics using Prometheus' text-based format allowing
 * these to be scraped by external systems.
 *
 * This endpoint can be used with any of the available data stores, but
 * was designed specifically to work with the in-memory data store.
 *
 * @see <a href="https://prometheus.io/docs/instrumenting/exposition_formats/">Prometheus Exposition Formats</a>
 * @author Jesse White (jesse@opennms.ca)
 */
@Path("/metrics-export")
public class TSDRMetricsExportAPI {

    /**
     * Include the Prometheus format protocol version in the content type
     */
    public static final String CONTENT_TYPE = "text/plain; version=0.0.4";

    public static final long DEFAULT_INTERVAL_IN_SECONDS = TimeUnit.MINUTES.toMillis(5);

    // Mapping of metric properties to attributes
    private static final String NODE_ID_KEY = "Node";
    private static final String CATEGORY_NAME_KEY = "Category";

    // Value constants
    public static final String NAN_VALUE = "Nan";
    public static final String POSITIVE_INF_VALUE = "+Inf";
    public static final String NEGATIVE_INF_VALUE = "-Inf";

    // Regular expressions used to validate the metric and label names
    private static final Pattern METRIC_NAME_PATTERN = Pattern.compile("[a-zA-Z_:]([a-zA-Z0-9_:])*");
    private static final Pattern LABEL_NAME_PATTERN = Pattern.compile("[a-zA-Z_]([a-zA-Z0-9_])*");

    @GET
    @Path("/")
    @Produces(CONTENT_TYPE)
    public Response get(@QueryParam("from") String from,
                        @QueryParam("until") String until) throws ExecutionException, InterruptedException {
        final Long endTime = TSDRNBIRestAPI.getTimeFromString(until);
        final Long startTime = from != null ? TSDRNBIRestAPI.getTimeFromString(from) : endTime - DEFAULT_INTERVAL_IN_SECONDS;
        // Issue a query without specifying the category (or metric key), requesting all known metrics
        final GetTSDRMetricsInput input = new GetTSDRMetricsInputBuilder()
                .setStartTime(startTime)
                .setEndTime(endTime)
                .build();
        final Future<RpcResult<GetTSDRMetricsOutput>> future = TSDRDataqueryModule.metricDataService.getTSDRMetrics(input);
        if(!future.get().isSuccessful()){
            return Response.status(503).build();
        }

        // Convert the metrics to use the Prometheus text-based format: one metric per line
        final String payload = future.get().getResult().getMetrics().stream()
            // Exclude any metrics with invalid names
            .filter(m -> m.getMetricName() != null && METRIC_NAME_PATTERN.matcher(m.getMetricName()).matches())
            .map(m -> toPrometheusMetric(m))
            .sorted()
            .collect(Collectors.joining());

        // Render the response, specifying the protocol version in the content-type
        return Response.status(200)
                .type(CONTENT_TYPE)
                .entity(payload)
                .build();
    }

    public static String toPrometheusMetric(Metrics metric) {
        // Build a string that looks like:
        // metric_name{key="value"} value timestamp
        final StringBuilder sb = new StringBuilder();
        sb.append(metric.getMetricName());
        sb.append(toPrometheusMetricAttributes(metric));
        sb.append(" ");
        sb.append(toPrometheusMetricValue(metric));
        if (metric.getTimeStamp() != null) {
            sb.append(" ");
            sb.append(metric.getTimeStamp());
        }
        sb.append("\n"); // Every metric must end with a newline
        return sb.toString();
    }

    public static String toPrometheusMetricAttributes(Metrics metric) {
        final Map<String, String> attributes = new TreeMap<>();
        if (metric.getNodeID() != null) {
            attributes.put(NODE_ID_KEY, metric.getNodeID());
        }
        if (metric.getTSDRDataCategory() != null) {
            attributes.put(CATEGORY_NAME_KEY, metric.getTSDRDataCategory().getName());
        }
        if (metric.getRecordKeys() != null) {
            for (RecordKeys recordKey : metric.getRecordKeys()) {
                // Exclude any labels with invalid names
                if (!LABEL_NAME_PATTERN.matcher(recordKey.getKeyName()).matches()) {
                    continue;
                }
                attributes.put(recordKey.getKeyName(), recordKey.getKeyValue());
            }
        }
        if (attributes.size() < 1) {
            // Use an empty string when no attributes are set
            return "";
        }
        final Gson gson = new GsonBuilder().create();
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Entry<String, String> entry : attributes.entrySet()) {
            if (sb.length() > 1) {
                sb.append(",");
            }
            sb.append(entry.getKey());
            sb.append("=");
            // Leverage GSON to perform any necessary escaping
            sb.append(gson.toJson(entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    public static String toPrometheusMetricValue(Metrics metric) {
        if (metric.getMetricValue() == null) {
            return NAN_VALUE;
        }

        final double value = metric.getMetricValue().doubleValue();
        if (Objects.equal(Double.POSITIVE_INFINITY, value)) {
            return POSITIVE_INF_VALUE;
        } else if (Objects.equal(Double.NEGATIVE_INFINITY, value)) {
            return NEGATIVE_INF_VALUE;
        } else if (Double.isNaN(value)) {
            return NAN_VALUE;
        } else if (value % 1.0 == 0) {
            // Avoid trailing 0s
            return String.format("%.0f", value);
        } else {
            return Double.toString(value);
        }
    }
}
