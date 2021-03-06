/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.service.impl;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRMetric;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;



/**
 * Tests the persistence of the TSDR Model in default JPA store
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 */

public class TsdrH2PersistenceServiceImplTest {


    TsdrJpaServiceImpl tsdrJpaService = null;
    TsdrH2PersistenceServiceImpl tsdrH2PersistenceService = null;
    //we need to create the EntityManager ourselves and pass it to the service
    EntityManagerFactory emf = null;
    EntityManager em = null;

    @Before
    public void init(){
        tsdrJpaService = new TsdrJpaServiceImpl();
        tsdrH2PersistenceService = new TsdrH2PersistenceServiceImpl();
        //we need to create the EntityManager ourselves and pass it to the service
        emf = Persistence
            .createEntityManagerFactory("metric");
        em = emf.createEntityManager();
        tsdrJpaService.setEntityManager(em);
        tsdrH2PersistenceService.setJpaService(tsdrJpaService);


    }

    @Test
    public void testStore() throws Exception {
        em.getTransaction().begin();

        RecordKeys recordKeys = new RecordKeysBuilder()
            .setKeyName("recordKeyName")
            .setKeyValue("node_table_flow").build();

        List<RecordKeys> recordKeysList= new ArrayList<RecordKeys>();
        recordKeysList.add(recordKeys);
        String timeStamp = ""+System.currentTimeMillis();

        TSDRMetricRecordBuilder tsdrMetricBuilder = new TSDRMetricRecordBuilder();
        TSDRMetricRecord tsdrMetrics = tsdrMetricBuilder.setMetricName("METRIC_NAME")
            .setMetricValue(new BigDecimal(Double.parseDouble("64")))
            .setNodeID("openflow:dummy")
            .setRecordKeys(recordKeysList)
            .setTimeStamp(Long.parseLong(timeStamp))
            .setTSDRDataCategory(DataCategory.FLOWSTATS).build();

        tsdrH2PersistenceService.store(
            tsdrMetrics);
        em.getTransaction().commit();

        //now let us try to get the saved metric
        List<TSDRMetricRecord>metricList = tsdrJpaService.getMetricsFilteredByCategory(DataCategory.FLOWSTATS.name(),0,Long.MAX_VALUE);
        Assert.assertEquals(1, metricList.size());
        Assert.assertEquals("METRIC_NAME", metricList.get(0).getMetricName());
        Assert.assertEquals(64.0,metricList.get(0).getMetricValue().doubleValue(),0.02);
        Assert.assertEquals("openflow:dummy",metricList.get(0).getNodeID());
        String mID = FormatUtil.getTSDRMetricKey(metricList.get(0));
        Assert.assertEquals("[NID=openflow:dummy][DC=FLOWSTATS][MN=METRIC_NAME][RK=recordKeyName:node_table_flow]",mID);;
        Assert.assertEquals(timeStamp, metricList.get(0).getTimeStamp().toString());
        Assert.assertEquals(DataCategory.FLOWSTATS.toString(), (metricList.get(0)).getTSDRDataCategory().toString());

    }

    @Test
    //simple test to see having fixed format strings as output works
    public void testFormatString (){
        long x = 12356;
        long y = 1;
        long z = 123456789;
        String resX = "          12356";
        String resY = "              1";
        String resZ = "      123456789";

        Assert.assertEquals(resX,FormatUtil.getFixedFormatString(String.valueOf(x), 15));
        Assert.assertEquals(resY,FormatUtil.getFixedFormatString(String.valueOf(y), 15));
        Assert.assertEquals(resZ,FormatUtil.getFixedFormatString(String.valueOf(z), 15));
    }

    private void storeTSDRMetric (Map<String,String> recordKeyValues,
                                  Map<String,String>metricNameValues,
                                  String node,
                                  DataCategory dataCategory,String timestamp){




        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        for(String key: recordKeyValues.keySet()){
            RecordKeys recordKey = new RecordKeysBuilder()
                .setKeyName(key)
                .setKeyValue(recordKeyValues.get(key)).build();
            recordKeys.add(recordKey);
        }

        for(String metricName:metricNameValues.keySet()) {
            em.getTransaction().begin();
            TSDRMetricRecordBuilder builder = new TSDRMetricRecordBuilder();

            TSDRMetric tsdrMetric = builder.setMetricName(metricName)
                .setMetricValue(new BigDecimal(Double.parseDouble(metricNameValues.get(metricName))))
                .setNodeID(node)
                .setRecordKeys(recordKeys)
                .setTSDRDataCategory(dataCategory)
                .setTimeStamp(new Long(timestamp)).build();
            tsdrH2PersistenceService.store((TSDRMetricRecord)
                tsdrMetric);
            em.getTransaction().commit();
        }

    }
    private void validateResults (DataCategory dc, String metricName,Double metricValue,String nodeId,Map<String,String> metricDetailsExpected,String timeStamp){

        //now let us try to get the saved metric
        List<TSDRMetricRecord>metricList = tsdrJpaService.getMetricsFilteredByCategory(dc.name(),0,Long.MAX_VALUE);
        Assert.assertEquals(1, metricList.size());
        Assert.assertEquals(metricName, metricList.get(0).getMetricName());
        Assert.assertEquals(metricValue, metricList.get(0).getMetricValue().doubleValue(), 0.02);
        Assert.assertEquals(nodeId, metricList.get(0).getNodeID());

        String metricDetails = FormatUtil.getTSDRMetricKey(metricList.get(0));

        if(metricDetails!=null){
            for(String key:metricDetailsExpected.keySet()){
                Assert.assertTrue(metricDetails.contains(key));
                Assert.assertTrue(metricDetails.contains(metricDetailsExpected.get(key)));
                Assert.assertFalse(metricDetails.contains("table2"));

            }
        }
        Assert.assertEquals(""+timeStamp, ""+metricList.get(0).getTimeStamp());
        Assert.assertEquals(dc.toString(), (metricList.get(0)).getTSDRDataCategory().toString());

    }
    @Test
    public void testFlowStatistics() {
        final Map mapRecord = new HashMap<String,String>();
        mapRecord.put("recordKeyName", "node_table_flow");
        final Map metricNameValues = new HashMap<String,String>();
        metricNameValues.put("METRIC_NAME", "64");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "openflow:dummy", DataCategory.FLOWSTATS, timeStamp);

        validateResults(DataCategory.FLOWSTATS, "METRIC_NAME", 64.0, "openflow:dummy", mapRecord, timeStamp);

    }


    @Test
    public void testFlowTableStatisticsPacketsMatched() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.FLOW_TABLE_KEY_NAME, "table1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("PacketsMatched", "20000000");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.FLOWTABLESTATS, timeStamp);

        validateResults(DataCategory.FLOWTABLESTATS, "PacketsMatched", 20000000.0, "node1", mapRecord, timeStamp);
    }


    @Test
    public void testFlowTableStatisticsActiveFlows() {

        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.FLOW_TABLE_KEY_NAME, "table1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("ActiveFlows", "20000000");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.FLOWTABLESTATS, timeStamp);

        validateResults(DataCategory.FLOWTABLESTATS, "ActiveFlows", 20000000.0, "node1", mapRecord, timeStamp);

    }

    @Test
    public void testFlowTableStatisticsPacketsLookedup() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.FLOW_TABLE_KEY_NAME, "table1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("PacketsLookedup", "20000000");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.FLOWTABLESTATS, timeStamp);


        validateResults(DataCategory.FLOWTABLESTATS, "PacketsLookedup", 20000000.0, "node1",mapRecord, timeStamp);

    }

    @Test
    public void testPortStatisticsCollisionCount() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.INTERNFACE_KEY_NAME, "port1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("CollisionCount", "2000");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.PORTSTATS, timeStamp);


        validateResults(DataCategory.PORTSTATS, "CollisionCount", 2000.0, "node1", mapRecord, timeStamp);

    }

    @Test
    public void testPortStatisticsReceiveCRSError() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.INTERNFACE_KEY_NAME, "port1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("ReceiveCRCError", "2000");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.PORTSTATS, timeStamp);

        validateResults(DataCategory.PORTSTATS, "ReceiveCRCError", 2000.0, "node1",mapRecord, timeStamp);

    }

    @Test
    public void testPortStatisticsReceivedDrops() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.INTERNFACE_KEY_NAME, "port1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("ReceivedDrops", "2000");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.PORTSTATS, timeStamp);


        validateResults(DataCategory.PORTSTATS, "ReceivedDrops", 2000.0, "node1",mapRecord, timeStamp);
    }



    @Test
    public void testPortStatisticsReceivedErrors() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.INTERNFACE_KEY_NAME, "port1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("ReceivedErrors", "2000");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.PORTSTATS, timeStamp);


        validateResults(DataCategory.PORTSTATS, "ReceivedErrors", 2000.0, "node1",mapRecord, timeStamp);
    }


    @Test
    public void testPortStatisticsReceiveFrameErrors() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.INTERNFACE_KEY_NAME, "port1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("ReceiveFrameErrors", "2000");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.PORTSTATS, timeStamp);

        validateResults(DataCategory.PORTSTATS, "ReceiveFrameErrors", 2000.0, "node1",mapRecord, timeStamp);
    }


    @Test
    public void testPortStatisticsReceiveOverRunError() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.INTERNFACE_KEY_NAME, "port1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("ReceiveOverRunError", "2000");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.PORTSTATS, timeStamp);
        validateResults(DataCategory.PORTSTATS, "ReceiveOverRunError", 2000.0, "node1",mapRecord, timeStamp);
    }


    @Test
    public void testPortStatisticsTransmitDrops() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.INTERNFACE_KEY_NAME, "port1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("TransmitDrops", "2000");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1",DataCategory.PORTSTATS, timeStamp);

        validateResults(DataCategory.PORTSTATS, "TransmitDrops", 2000.0, "node1",mapRecord, timeStamp);
    }

    @Test
    public void testPortStatisticsTransmitErrors() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.INTERNFACE_KEY_NAME, "port1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("TransmitErrors", "2000");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.PORTSTATS, timeStamp);

        validateResults(DataCategory.PORTSTATS, "TransmitErrors", 2000.0, "node1",mapRecord, timeStamp);
    }


    @Test
    public void testPortStatisticsReceivedPackets() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.INTERNFACE_KEY_NAME, "port1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("ReceivedPackets", "4000");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.PORTSTATS, timeStamp);

        validateResults(DataCategory.PORTSTATS, "ReceivedPackets", 4000.0, "node1",mapRecord, timeStamp);
    }


    @Test
    public void testPortStatisticsTransmitPackets() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.INTERNFACE_KEY_NAME, "port1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("TransmittedPackets", "4000");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.PORTSTATS, timeStamp);

        validateResults(DataCategory.PORTSTATS, "TransmittedPackets", 4000.0, "node1",mapRecord, timeStamp);
    }


    @Test
    public void testPortStatisticsReceivedBytes() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.INTERNFACE_KEY_NAME, "port1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("ReceivedBytes", "20000000");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.PORTSTATS, timeStamp);

        validateResults(DataCategory.PORTSTATS, "ReceivedBytes", 20000000.0, "node1",mapRecord, timeStamp);
    }



    @Test
    public void testPortStatisticsTransmittedBytes() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.INTERNFACE_KEY_NAME, "port1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("TransmittedBytes", "20000000");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.PORTSTATS, timeStamp);

        validateResults(DataCategory.PORTSTATS, "TransmittedBytes", 20000000.0, "node1",mapRecord, timeStamp);
    }



    @Test
    public void testPortStatisticsDurationInSeconds() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.INTERNFACE_KEY_NAME, "port1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("DurationInSeconds", "20");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.PORTSTATS, timeStamp);

        validateResults(DataCategory.PORTSTATS, "DurationInSeconds", 20.0, "node1",mapRecord, timeStamp);
    }


    @Test
    public void testPortStatisticsDurationInNanoSeconds() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.INTERNFACE_KEY_NAME, "port1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("DurationInNanoSeconds", "2000000");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.PORTSTATS, timeStamp);

        validateResults(DataCategory.PORTSTATS, "DurationInNanoSeconds", 2000000.0, "node1",mapRecord, timeStamp);
    }


    @Test
    public void testQueueStatisticsTransmissionErrors() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.QUEUE_KEY_NAME,"queue1");
        mapRecord.put(TSDRConstants.INTERNFACE_KEY_NAME, "port1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("TransmissionErrors", "3000");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.QUEUESTATS, timeStamp);

        validateResults(DataCategory.QUEUESTATS, "TransmissionErrors", 3000.0, "node1", mapRecord, timeStamp);
    }


    @Test
    public void testQueueStatisticsTransmissionBytes() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.QUEUE_KEY_NAME,"queue1");
        mapRecord.put(TSDRConstants.INTERNFACE_KEY_NAME, "port1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("TransmissionBytes", "3000");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.QUEUESTATS, timeStamp);

        validateResults(DataCategory.QUEUESTATS, "TransmissionBytes", 3000.0, "node1", mapRecord, timeStamp);
    }

    @Test
    public void testQueueStatisticsTransmissionPackets() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.QUEUE_KEY_NAME,"queue1");
        mapRecord.put(TSDRConstants.INTERNFACE_KEY_NAME, "port1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("TransmissionBytes", "3000");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.QUEUESTATS, timeStamp);


        validateResults(DataCategory.QUEUESTATS, "TransmissionBytes", 3000.0, "node1", mapRecord, timeStamp);
    }

    @Test
    public void testFlowMeterStatisticsPacketCount() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.METER_KEY_NAME,"meter1");
        mapRecord.put(TSDRConstants.GROUP_KEY_NAME, "group1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("PacketCount", "40");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.FLOWMETERSTATS, timeStamp);

        validateResults(DataCategory.FLOWMETERSTATS, "PacketCount", 40.0, "node1", mapRecord, timeStamp);
    }

    @Test
    public void testFlowMeterStatisticsByteCount() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.METER_KEY_NAME,"meter1");
        mapRecord.put(TSDRConstants.GROUP_KEY_NAME, "group1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("ByteCount", "40");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.FLOWMETERSTATS, timeStamp);


        validateResults(DataCategory.FLOWMETERSTATS, "ByteCount", 40.0, "node1", mapRecord, timeStamp);
    }

    @Test
    public void testFlowMeterStatisticsRefCount() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.METER_KEY_NAME,"meter1");
        mapRecord.put(TSDRConstants.GROUP_KEY_NAME, "group1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("RefCount", "40");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.FLOWMETERSTATS, timeStamp);

        validateResults(DataCategory.FLOWMETERSTATS, "RefCount", 40.0, "node1", mapRecord, timeStamp);
    }

    @Test
    public void testGroupStatisticsPacketCount() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.GROUP_KEY_NAME,"group1");
        mapRecord.put(TSDRConstants.BUCKET_KEY_NAME, "bucket1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("PacketCount", "40");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.FLOWGROUPSTATS, timeStamp);

        validateResults(DataCategory.FLOWGROUPSTATS, "PacketCount", 40.0, "node1",mapRecord, timeStamp);

    }


    @Test
    public void testGroupStatisticsByteCount() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.GROUP_KEY_NAME,"group1");
        mapRecord.put(TSDRConstants.BUCKET_KEY_NAME, "bucket1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("ByteCount", "40");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.FLOWGROUPSTATS, timeStamp);
	      validateResults(DataCategory.FLOWGROUPSTATS, "ByteCount", 40.0, "node1",mapRecord, timeStamp);

    }

    @Test
    public void testGroupStatisticsRefCount() {
        final Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.GROUP_KEY_NAME,"group1");
        mapRecord.put(TSDRConstants.BUCKET_KEY_NAME, "bucket1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        final Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("RefCount", "40");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.FLOWGROUPSTATS, timeStamp);

        validateResults(DataCategory.FLOWGROUPSTATS, "RefCount", 40.0, "node1", mapRecord, timeStamp);

    }

    private Date getOldDate (int howManyDaysOld){
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date myDate = new Date(System.currentTimeMillis());
        Calendar cal = Calendar.getInstance();
        cal.setTime(myDate);
        cal.add(Calendar.DATE, -1 * howManyDaysOld);
        return cal.getTime();
    }

    @Test
    public void testPurgeBasedOnRetentionDate() {
        Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.GROUP_KEY_NAME,"group1");
        mapRecord.put(TSDRConstants.BUCKET_KEY_NAME, "bucket1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("RefCount", "40");
        //get old 10 days old
        Long tenDaysBack = new Long(getOldDate(10).getTime());
        Long twentyDaysBack = new Long(getOldDate(20).getTime());

        //here we are creating FLOWGROUPSTATS records
        //here we are creating record that has 10 days old time stamp
        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.FLOWGROUPSTATS, tenDaysBack.toString());

        //here we are creating record that has 20 days old time stamp
        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.FLOWGROUPSTATS, twentyDaysBack.toString());


        //here we are creating FLOWMETERSTATS records
        mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.METER_KEY_NAME,"meter1");
        mapRecord.put(TSDRConstants.GROUP_KEY_NAME, "group1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        metricNameValues = new HashMap<String, String>();
        metricNameValues.put("RefCount", "40");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.FLOWMETERSTATS, timeStamp);

        //we are purging oldest FLOWGROUPSTATS
        // @TODO - For Basheer
        if(true) return;
        purgeTSDRMetric(DataCategory.FLOWGROUPSTATS, getOldDate(19));


        validateDeleteResults(new DataCategory[]{DataCategory.FLOWGROUPSTATS, DataCategory.FLOWMETERSTATS},
            DataCategory.FLOWGROUPSTATS,
            new String[]{tenDaysBack.toString(),timeStamp.toString()},
            "RefCount",40.0, new int[]{1,1});


    }

    private void validateDeleteResults(DataCategory[] allCategories,DataCategory purgeCategory,
                                       String[] nonPurgeTime,String metricName,Double metricValue,int expectedRowsForEachCategory[]) {
        //now let us try to get the saved metric

        //After calling purge there should be just one record
        List<TSDRMetricRecord>metricList = tsdrJpaService.getMetricsFilteredByCategory(purgeCategory.name(),0,0);
        Assert.assertEquals(expectedRowsForEachCategory[0], metricList.size());
        if(expectedRowsForEachCategory[0] != 0) {
            Assert.assertEquals(metricName, metricList.get(0).getMetricName());
            Assert.assertEquals(metricValue, metricList.get(0).getMetricValue().doubleValue(), 0.02);
            Assert.assertEquals(nonPurgeTime[0], String.valueOf(metricList.get(0).getTimeStamp()));
        }
        //There shouldn't be any impact to other DataCategory

        metricList = tsdrJpaService.getMetricsFilteredByCategory(allCategories[1].name(),0,0);
        Assert.assertEquals(expectedRowsForEachCategory[1], metricList.size());
        Assert.assertEquals(metricName, metricList.get(0).getMetricName());
        Assert.assertEquals(metricValue, metricList.get(0).getMetricValue().doubleValue(), 0.02);
        Assert.assertEquals(nonPurgeTime[1], String.valueOf(metricList.get(0).getTimeStamp()));

    }

    private void purgeTSDRMetric(DataCategory dc, Date twentyDaysBack) {
        em.getTransaction().begin();
        tsdrH2PersistenceService.purgeTSDRRecords(dc, twentyDaysBack.getTime());
        em.getTransaction().commit();

    }

    private void purgeAllTSDRMetric(Date twentyDaysBack) {
        em.getTransaction().begin();
        tsdrH2PersistenceService.purgeAllTSDRRecords(twentyDaysBack.getTime());
        em.getTransaction().commit();

    }

    @Test
    public void testPurgeAllBasedOnRetentionDate() {
        Map mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.GROUP_KEY_NAME,"group1");
        mapRecord.put(TSDRConstants.BUCKET_KEY_NAME, "bucket1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        Map metricNameValues = new HashMap<String, String>();
        metricNameValues.put("RefCount", "40");
        //get old 10 days old
        Long tenDaysBack = new Long(getOldDate(10).getTime());
        Long twentyDaysBack = new Long(getOldDate(20).getTime());

        //here we are creating FLOWGROUPSTATS records
        //here we are creating record that has 10 days old time stamp
        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.FLOWGROUPSTATS, tenDaysBack.toString());

        //here we are creating record that has 20 days old time stamp
        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.FLOWGROUPSTATS, twentyDaysBack.toString());


        //here we are creating FLOWMETERSTATS records
        mapRecord = new HashMap<String, String>();
        mapRecord.put(TSDRConstants.METER_KEY_NAME,"meter1");
        mapRecord.put(TSDRConstants.GROUP_KEY_NAME, "group1");
        mapRecord.put("Node", "node1");
        mapRecord.put("Table", "table1");
        metricNameValues = new HashMap<String, String>();
        metricNameValues.put("RefCount", "40");
        String timeStamp = (new Long((new Date()).getTime())).toString();

        storeTSDRMetric(mapRecord, metricNameValues, "node1", DataCategory.FLOWMETERSTATS,timeStamp);

        //we are purging oldest FLOWGROUPSTATS -- older than 5 days everything should be purged from
        //FLOWGROUPSTATS category but there should be one row for FLOWMETERSTATS
        // @TODO - For Basheer
        if(true) return;
        purgeAllTSDRMetric(getOldDate(5));


        validateDeleteResults(new DataCategory[]{DataCategory.FLOWGROUPSTATS, DataCategory.FLOWMETERSTATS},
            DataCategory.FLOWGROUPSTATS,
            new String[]{tenDaysBack.toString(),timeStamp.toString()},
            "RefCount",40.0,new int[]{0,1});


    }


}

