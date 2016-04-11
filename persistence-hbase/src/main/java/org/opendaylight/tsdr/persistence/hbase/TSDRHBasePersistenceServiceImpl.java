/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import org.apache.hadoop.hbase.TableNotFoundException;
import org.opendaylight.tsdr.spi.persistence.TSDRBinaryPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRLogPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRMetricPersistenceService;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.binary.data.rev160325.storetsdrbinaryrecord.input.TSDRBinaryRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.TSDRMetric;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

/**
 * This class provides HBase implementation of TSDRPersistenceService.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Feb 24, 2015
 *
 *
 * Revision: April 2, 2015
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed </a>
 *    --- Introduction of getMetrics in persistence SPI
 *
 * Revision: Dec 10, 2015
 * @author <a href="mailto:saichler@gmail.com">Sharon Aicler</a>
 *
 *
 */
public class TSDRHBasePersistenceServiceImpl  implements TSDRLogPersistenceService,TSDRMetricPersistenceService, TSDRBinaryPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(TSDRHBasePersistenceServiceImpl.class);
    public ScheduledFuture future;

    /**
     * Constructor.
     */
    public TSDRHBasePersistenceServiceImpl(){
        log.debug("Entering start(timeout)");
        //create the HTables used in TSDR.
        CreateTableTask createTableTask = new CreateTableTask();
        future = SchedulerService.getInstance().scheduleTask(createTableTask);
        log.debug("Exiting start(timeout)");
        log.info("TSDR HBase Data Store is initialized.");
    }


    /*
    *  This overloaded constructor version is added for UT purpose.
    *  Refrain from calling it(except from UT)
    */
    public TSDRHBasePersistenceServiceImpl(HBaseDataStore hbaseDataStore, ScheduledFuture sfuture){
       HBaseDataStoreFactory.setHBaseDataStoreIfAbsent(hbaseDataStore);
       future = sfuture;
    }
    /*
    *  This overloaded constructor version is added for UT purpose.
    *  Refrain from calling it(except from UT)
    */
    public TSDRHBasePersistenceServiceImpl(HBaseDataStore hbaseDataStore){
        HBaseDataStoreFactory.setHBaseDataStoreIfAbsent(hbaseDataStore);
     }

    /**
     * Store TSDRMetricRecord.
     */
    @Override
    public void storeMetric(TSDRMetricRecord metrics){
        log.debug("Entering store(TSDRMetricRecord)");
        //convert TSDRRecord to HBaseEntities
        try{
            HBaseEntity entity = convertToHBaseEntity(metrics);
            if ( entity == null){
                log.debug("the entity is null when converting TSDRMetricRecords into hbase entity");
            return;
            }
            HBaseDataStoreFactory.getHBaseDataStore().create(entity);
        } catch(TableNotFoundException e){
              TriggerTableCreatingTask();
        }
         log.debug("Exiting store(TSDRMetricRecord)");
     }

    /**
     * Store a list of TSDRMetricRecord.
    */
    @Override
    public void storeMetric(List<TSDRMetricRecord> recordList){
        log.debug("Entering store(List<TSDRRecord>)");

        //tableName, entityList Map
        Map<String, List<HBaseEntity>> entityListMap = new HashMap<String, List<HBaseEntity>>();


        List<HBaseEntity> entityList = new ArrayList<HBaseEntity>();
        if ( recordList != null && recordList.size() != 0){
            try{
                for(TSDRRecord record: recordList){
                    HBaseEntity entity = null;
                    if ( record instanceof TSDRMetricRecord){
                        entity = convertToHBaseEntity((TSDRMetricRecord)record);
                    }else if ( record instanceof TSDRLogRecord){
                        entity = convertToHBaseEntity((TSDRLogRecord)record);
                    }
                    if ( entity == null){
                        log.debug("the entity is null when converting TSDRMetricRecords into hbase entity");
                        return;
                    }
                    String tableName = entity.getTableName();
                    if ( entityListMap.get(tableName) == null){
                        entityListMap.put(tableName, new ArrayList<HBaseEntity>());
                    }
                    entityListMap.get(tableName).add(entity);
                    entityList.add(entity);
                }
                Set<String> keys = entityListMap.keySet();
                Iterator<String> iter = keys.iterator();
                while ( iter.hasNext()){
                    String tableName = iter.next();
                    HBaseDataStoreFactory.getHBaseDataStore().create(entityListMap.get(tableName));

                }

            } catch(TableNotFoundException e){
                 TriggerTableCreatingTask();
            }
        }
        log.debug("Exiting store(List<TSDRRecord>)");
    }

    /**
     * Store a list of TSDRMetricRecord.
     */
    @Override
    public void storeLog(List<TSDRLogRecord> recordList){
        log.debug("Entering store(List<TSDRRecord>)");

        //tableName, entityList Map
        Map<String, List<HBaseEntity>> entityListMap = new HashMap<String, List<HBaseEntity>>();


        List<HBaseEntity> entityList = new ArrayList<HBaseEntity>();
        if ( recordList != null && recordList.size() != 0){
            try{
                for(TSDRRecord record: recordList){
                    HBaseEntity entity = null;
                    if ( record instanceof TSDRMetricRecord){
                        entity = convertToHBaseEntity((TSDRMetricRecord)record);
                    }else if ( record instanceof TSDRLogRecord){
                        entity = convertToHBaseEntity((TSDRLogRecord)record);
                    }
                    if ( entity == null){
                        log.debug("the entity is null when converting TSDRMetricRecords into hbase entity");
                        return;
                    }
                    String tableName = entity.getTableName();
                    if ( entityListMap.get(tableName) == null){
                        entityListMap.put(tableName, new ArrayList<HBaseEntity>());
                    }
                    entityListMap.get(tableName).add(entity);
                    entityList.add(entity);
                }
                Set<String> keys = entityListMap.keySet();
                Iterator<String> iter = keys.iterator();
                while ( iter.hasNext()){
                    String tableName = iter.next();
                    HBaseDataStoreFactory.getHBaseDataStore().create(entityListMap.get(tableName));

                }

            } catch(TableNotFoundException e){
                TriggerTableCreatingTask();
            }
        }
        log.debug("Exiting store(List<TSDRRecord>)");
    }

    /**
     * Start TSDRHBasePersistenceService.
    }*/

    /**
     * Stop TSDRHBasePersistenceService.
     */
    public void stop(int timeout) {
       log.debug("Entering stop(timeout)");
        closeConnections();
        log.debug("Exiting stop(timeout)");
    }

    @Override
    /**
     * Retrieve a list of TSDRMetricRecords from HBase data store based on the
     * specified data category, startTime, and endTime.
     */
    public List<TSDRMetricRecord> getTSDRMetricRecords(String tsdrMetricKey, long startTime, long endTime){
        final List<TSDRMetricRecord> resultRecords = new ArrayList<TSDRMetricRecord>();
        final List<String> substringFilterList = new ArrayList<>(4);

        if ( tsdrMetricKey == null ){
            log.error("The tsdr metric key is null");
            return resultRecords;
        }

        //This is getting all data from the hbase table
        List<HBaseEntity> resultEntities = null;
        if(FormatUtil.isDataCategoryKey(tsdrMetricKey) 
                || FormatUtil.isDataCategory(tsdrMetricKey)) {
            String dataCategory = FormatUtil.isDataCategoryKey(tsdrMetricKey)?
                    FormatUtil.getDataCategoryFromTSDRKey(tsdrMetricKey):tsdrMetricKey;
            resultEntities = HBaseDataStoreFactory.getHBaseDataStore()
                    .getDataByTimeRange(dataCategory, startTime, endTime);
            for (HBaseEntity e : resultEntities) {
                resultRecords.add(getTSDRMetricRecord(e));
            }
            return resultRecords;
        }else{

            // A valid tsdr metric key does need to contain all the keys but DOES NOT need to contain all the values.
            // e.g. [NID=][DC=PORTSTATS][MN=][RK=] is a valid metric key
            if(!FormatUtil.isValidTSDRKey(tsdrMetricKey)){
                log.error("TSDR Key {} is not in the correct format",tsdrMetricKey);
                return resultRecords;
            }

            String dataCategory = FormatUtil.getDataCategoryFromTSDRKey(tsdrMetricKey);

            //The data category is a mandatory key for hbase as it defines the table name.
            if(!FormatUtil.isDataCategory(dataCategory)){
                log.error("Data Category is unknown {}",dataCategory);
                return resultRecords;
            }

            //Add filter for node id
            String nodeID = FormatUtil.getNodeIdFromTSDRKey(tsdrMetricKey);
            if(!nodeID.isEmpty()){
                substringFilterList.add("[NID=" + nodeID + "]");
            }

            //Add filter for metric name
            String metricName = FormatUtil.getMetriNameFromTSDRKey(tsdrMetricKey);
            if(!metricName.isEmpty()){
                substringFilterList.add("[MN=" + metricName + "]");
            }

            //Add filter for record keys
            List<RecordKeys> recKeys = FormatUtil.getRecordKeysFromTSDRKey(tsdrMetricKey);
            String recKeyString = "[RK=";
            if(!recKeys.isEmpty()){
                for(RecordKeys recKey:recKeys){
                    recKeyString = recKeyString + recKey.getKeyName() + ":" + recKey.getKeyValue() + ",";
                }
                recKeyString = recKeyString.substring(0,recKeyString.length() -1) + "]";
                substringFilterList.add(recKeyString);
            }

            resultEntities = null;
            resultEntities = HBaseDataStoreFactory.getHBaseDataStore().getDataByTimeRange(dataCategory,substringFilterList, startTime, endTime);
            for (HBaseEntity e : resultEntities) {
                resultRecords.add(getTSDRMetricRecord(e));
            }
            return resultRecords;
        }
    }
    @Override
    /**
     * Retrieve a list of TSDRLogRecords from HBase data store based on the
     * specified data tsdrLogKey, startTime, and endTime.
     */
    public List<TSDRLogRecord> getTSDRLogRecords(String tsdrLogKey, long startTime, long endTime){
        List<HBaseEntity> resultEntities = new ArrayList<>();
        final List<TSDRLogRecord> resultRecords = new ArrayList<TSDRLogRecord>(resultEntities.size());
        final List<String> substringFilterList = new ArrayList<>(4);

        if ( tsdrLogKey == null ){
            log.error("The data tsdrLogKey is not supported");
            return resultRecords;
        }

        //the tsdr log key is just the data category
        if(FormatUtil.isDataCategoryKey(tsdrLogKey) 
                || FormatUtil.isDataCategory(tsdrLogKey)) {
            String dataCategory = FormatUtil.isDataCategoryKey(tsdrLogKey)?
                    FormatUtil.getDataCategoryFromTSDRKey(tsdrLogKey):tsdrLogKey;
            resultEntities = HBaseDataStoreFactory.getHBaseDataStore()
                    .getDataByTimeRange(dataCategory, startTime, endTime);
            for (HBaseEntity e : resultEntities) {
                resultRecords.add(getTSDRLogRecord(e));
            }
            return resultRecords;
        }else{
            // A valid tsdr metric key does need to contain all the keys but DOES NOT need to contain all the values.
            // e.g. [NID=][DC=PORTSTATS][MN=][RK=] is a valid metric key
            if(!FormatUtil.isValidTSDRLogKey(tsdrLogKey)){
                log.error("TSDR Key {} is not in the correct format",tsdrLogKey);
                return resultRecords;
            }

            String dataCategory = FormatUtil.getDataCategoryFromTSDRKey(tsdrLogKey);

            //The data category is a mandatory key for hbase as it defines the table name.
            if(!FormatUtil.isDataCategory(dataCategory)){
                log.error("Data Category is unknown {}",dataCategory);
                return resultRecords;
            }

            //Add filter for node id
            String nodeID = FormatUtil.getNodeIdFromTSDRKey(tsdrLogKey);
            if(!nodeID.isEmpty()){
                substringFilterList.add("[NID=" + nodeID + "]");
            }

            //Add filter for record keys
            List<RecordKeys> recKeys = FormatUtil.getRecordKeysFromTSDRKey(tsdrLogKey);
            String recKeyString = "[RK=";
            if(!recKeys.isEmpty()){
                for(RecordKeys recKey:recKeys){
                    recKeyString = recKeyString + recKey.getKeyName() + ":" + recKey.getKeyValue() + ",";
                }
                recKeyString = recKeyString.substring(0,recKeyString.length() -1) + "]";
                substringFilterList.add(recKeyString);
            }

            resultEntities = HBaseDataStoreFactory.getHBaseDataStore().getDataByTimeRange(dataCategory, substringFilterList, startTime, endTime);
        }
        for (HBaseEntity e : resultEntities) {
            resultRecords.add(getTSDRLogRecord(e));
        }
        return resultRecords;
    }

    @Override
    public void purge(DataCategory category, long retention_time){
         try{
             HBaseDataStoreFactory.getHBaseDataStore().deleteByTimestamp(category.name(), retention_time);
         }catch ( IOException ioe){
             log.error("Error purging TSDR records in HBase data store {}", ioe);
         }
         return;
    }

    @Override
    public void purge(long retention_time){
         DataCategory[] categories = DataCategory.values();
         for ( int i = 0; i < categories.length; i++ ){
            DataCategory category = categories[i];
            purge(category, retention_time);
         }
         return;
    }
    /**
     * Trigger CreateTableTask".
    */

    public void TriggerTableCreatingTask()
    {
         synchronized(future){
             if(future.isDone() || future.isCancelled()){
                 log.info("Triggering CreateTableTask");
                 CreateTableTask createTableTask = new CreateTableTask();
                 Long interval = HBaseDataStoreContext.getPropertyInLong(HBaseDataStoreContext.HBASE_COMMON_PROP_CREATE_TABLE_RETRY_INTERVAL);
                 future = SchedulerService.getInstance().scheduleTaskAtFixedRate(createTableTask, 0L, interval);
             }
         }
    }
    /**
     * convert TSDRMetricRecord to HBaseEntity.
     * @param metrics
     * @return HBaseEntity
    */
    private HBaseEntity convertToHBaseEntity(TSDRMetricRecord metrics){
        log.debug("Entering convertToHBaseEntity(TSDRMetricRecord)");
        HBaseEntity entity = new HBaseEntity();

        TSDRMetric metricData = metrics;

        if ( metricData != null){
             DataCategory dataCategory = metricData.getTSDRDataCategory();
             if (dataCategory != null){
                 entity = HBasePersistenceUtil.getEntityFromMetricStats(metricData, dataCategory);
             }
        }
        log.debug("Exiting convertToHBaseEntity(TSDRMetricRecord)");
        return entity;
     }
    /**
     * convert TSDRMetricRecord to HBaseEntity.
     * @param logRecord
     * @return HBaseEntity
    */
    public HBaseEntity convertToHBaseEntity(TSDRLogRecord logRecord){
        log.debug("Entering convertToHBaseEntity(TSDRLogRecord)");
        HBaseEntity entity = new HBaseEntity();

        TSDRLogRecord logData = logRecord;

        if ( logData != null){
             DataCategory dataCategory = logData.getTSDRDataCategory();
             if (dataCategory != null){
                 entity = HBasePersistenceUtil.getEntityFromLogRecord(logData, dataCategory);
             }
        }
        log.debug("Exiting convertToHBaseEntity(TSDRLogRecord)");
        return entity;
     }

    /**
     * Close connections to the data store.
     */
    public void closeConnections(){
        log.debug("Entering closeConnections()");
        List<String> tableNames = HBasePersistenceUtil.getTSDRHBaseTables();
        for ( String tableName: tableNames){
            HBaseDataStoreFactory.getHBaseDataStore().closeConnection(tableName);
        }
        log.debug("Exiting closeConnections()");
        return;
    }

    /**
     * Create TSDR Tables.
     * @throws Exception - an exception.
     */
    public void createTables() throws Exception{
        log.debug("Entering createTables()");
        List<String> tableNames = HBasePersistenceUtil.getTSDRHBaseTables();
        for ( String tableName: tableNames){
            HBaseDataStoreFactory.getHBaseDataStore().createTable(tableName);
        }
        log.debug("Exiting createTables()");
        return;
    }

    private  void flushCommit(String tableName){
        HBaseDataStoreFactory.getHBaseDataStore().flushCommit(tableName);
    }

    public void flushCommit(Set<String> tableNames){
        log.debug("Entering flushing commits");
        for ( String tableName: tableNames){
            flushCommit(tableName);
        }
        log.debug("Exiting flushing commits");
    }


    @Override
    public void storeLog(TSDRLogRecord logRecord) {
        //convert TSDRLogRecord to HBaseEntities
        try{
            HBaseEntity entity = convertToHBaseEntity(logRecord);
            if ( entity == null){
                log.debug("the entity is null when converting TSDRMetricRecords into hbase entity");
            return;
            }
            HBaseDataStoreFactory.getHBaseDataStore().create(entity);
        } catch(TableNotFoundException e){
               TriggerTableCreatingTask();
        }
         log.debug("Exiting store(TSDRMetricRecord)");
    }

    private static final TSDRMetricRecord getTSDRMetricRecord(HBaseEntity entity){
        TSDRMetricRecordBuilder tsdrMetricRecordBuilder = new TSDRMetricRecordBuilder();
        tsdrMetricRecordBuilder.setMetricName(FormatUtil.getMetriNameFromTSDRKey(entity.getRowKey()));
        tsdrMetricRecordBuilder.setMetricValue(new BigDecimal(Double.parseDouble(entity.getColumns().get(0).getValue())));
        tsdrMetricRecordBuilder.setNodeID(FormatUtil.getNodeIdFromTSDRKey(entity.getRowKey()));
        tsdrMetricRecordBuilder.setRecordKeys(FormatUtil.getRecordKeysFromTSDRKey(entity.getRowKey()));
        tsdrMetricRecordBuilder.setTimeStamp(FormatUtil.getTimeStampFromTSDRKey(entity.getRowKey()));
        tsdrMetricRecordBuilder.setTSDRDataCategory(DataCategory.valueOf(entity.getTableName()));
        return tsdrMetricRecordBuilder.build();
    }

    private static final TSDRLogRecord getTSDRLogRecord(HBaseEntity entity){
        TSDRLogRecordBuilder tsdrLogRecordBuilder = new TSDRLogRecordBuilder();
        tsdrLogRecordBuilder.setTSDRDataCategory(DataCategory.valueOf(entity.getTableName()));
        tsdrLogRecordBuilder.setTimeStamp(FormatUtil.getTimeStampFromTSDRKey(entity.getRowKey()));
        tsdrLogRecordBuilder.setRecordKeys(FormatUtil.getRecordKeysFromTSDRKey(entity.getRowKey()));
        tsdrLogRecordBuilder.setNodeID(FormatUtil.getNodeIdFromTSDRKey(entity.getRowKey()));
        tsdrLogRecordBuilder.setIndex(-1);
        tsdrLogRecordBuilder.setRecordAttributes(null);
        String fullText = null;
        for ( HBaseColumn column: entity.getColumns()){
            if (column.getColumnQualifier().equalsIgnoreCase(TSDRHBaseDataStoreConstants.LOGRECORD_FULL_TEXT)){
                fullText = column.getValue();
                break;
            }
        }
        tsdrLogRecordBuilder.setRecordFullText(fullText);
        return tsdrLogRecordBuilder.build();
    }

    @Override
    public void storeBinary(TSDRBinaryRecord binaryRecord) {
        //@TODO - Add code to support binary store
    }

    @Override
    public void storeBinary(List<TSDRBinaryRecord> recordList) {
        //@TODO - Add code to support binary store
    }

    @Override
    public List<TSDRBinaryRecord> getTSDRBinaryRecords(String tsdrBinaryKey, long startTime, long endTime) {
        //@TODO - Add code to collect binary records
        return null;
    }
}
