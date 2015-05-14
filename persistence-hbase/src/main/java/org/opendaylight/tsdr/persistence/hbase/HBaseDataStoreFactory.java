/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

/**
 *
 *
 * This class creates HBase Data Store.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Feb 24, 2015
 *
 * Modified: May 4, 2015
 *
 * @author <a href="mailto:hariharan_sethuraman@dell.com">Hariharan Sethuraman</a>
 *
 */

import java.util.Properties;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HBaseDataStoreFactory {

    private static final Logger log = LoggerFactory.getLogger(HBaseDataStoreFactory.class);
    private static HBaseDataStore datastore = null;
    private static String hbasePropsFilename = "hbase-configuration.properties";

    /**
     * Default constructor
     */
    private HBaseDataStoreFactory(){
        super();
    }

    /**
     * To obtain or create the HBase Data Store.
     * @return HBaseDataStore
     */
    public static HBaseDataStore getHBaseDataStore(){
        //load XML and initialize HBase data store
        if ( datastore == null){
            HBaseDataStoreContext context = initialize_datastore_context();
            datastore = new HBaseDataStore(context);
        }
        return datastore;
    }

    /*
     * Setter for UT purpose which is invoked from the factory
     */
    static void setHBaseDataStoreIfAbsent(HBaseDataStore hbaseDataStore){
        if(datastore == null){
            initialize_datastore_context();//just for UT purpose
            datastore = hbaseDataStore;
        }
    }

    /**
     * Initialize the data store context by reading from an XML
     * configuration file.
     * @return HBaseDataStoreContext
    */
    private static HBaseDataStoreContext initialize_datastore_context(){
        HBaseDataStoreContext context = new HBaseDataStoreContext();
        Properties properties = new Properties();
        InputStream inputStream = HBaseDataStoreContext.class.getClassLoader().getResourceAsStream(hbasePropsFilename);

        try{
            properties.load(inputStream);
        } catch(Exception e){
            log.error("Exception while loading the hbase-configuration.properties stream", e);
        }

        if(inputStream == null || !properties.propertyNames().hasMoreElements()){
            log.error("Properties stream is null or properties failed to load, check the file=" + hbasePropsFilename +" exists in classpath");
            log.warn("Initializing HbaseDataStoreContext default values");
            context.setPoolSize(20);
            context.setZookeeperClientport("2181");
            context.setZookeeperQuorum("localhost");
            context.setAutoFlush(false);
            context.setWriteBufferSize(512);
            if(inputStream != null){
                try{
                    inputStream.close();
                } catch(Exception e){
                    log.error("Exception while closing the hbase-configuration.properties stream", e);
                }
            }
            return context;
        }

        log.info("Loading properties from hbase-configuration.properties");

        context.setPoolSize(Integer.valueOf(properties.getProperty("poolsize")));
        context.setZookeeperClientport(properties.getProperty("zoo.keeper.client.port"));
        context.setZookeeperQuorum(properties.getProperty("zoo.keeper.quorum"));
        context.setAutoFlush(Boolean.valueOf(properties.getProperty("autoflush")));
        context.setWriteBufferSize(Integer.valueOf(properties.getProperty("writebuffersize")));

        try{
            inputStream.close();
        } catch(Exception e){
            log.error("Exception while closing the hbase-configuration.properties stream", e);
        }
        return context;
    }
}
