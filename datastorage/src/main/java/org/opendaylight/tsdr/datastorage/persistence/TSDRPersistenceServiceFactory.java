/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage.persistence;


import org.opendaylight.tsdr.spi.persistence.TsdrLogPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TsdrMetricPersistenceService;
import org.opendaylight.tsdr.spi.util.TsdrPersistenceServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to create a TSDRPersistence Service with a specified or configured
 * Persistence data store.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Feb 24, 2015
 *
 * Revision: March 05, 2015
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 *
 */
public class TSDRPersistenceServiceFactory {
    private static final Logger log = LoggerFactory.getLogger(TSDRPersistenceServiceFactory.class);

    private static TsdrMetricPersistenceService metricPersistenceService = null;
    private static TsdrLogPersistenceService logPersistenceService = null;

    /**
     * Default constructor
    */
    private TSDRPersistenceServiceFactory(){
        super();
    }


    /**
     * Obtain the TSDR Persistence Data Store
     * @return
     */
    public static TsdrMetricPersistenceService getTSDRMetricPersistenceDataStore( ){
        log.debug("Entering getTSDRMetricPersistenceDataStore()");
        if(metricPersistenceService == null){
            metricPersistenceService = TsdrPersistenceServiceUtil.getTsdrMetricPersistenceService();
            if(metricPersistenceService == null) {
                log.error("metricPersistenceService is found to be null");
            }
        }

        log.debug("Exiting getTSDRMetricPersistenceDataStore()");
        return metricPersistenceService;
    }


    /**
     * Obtain the TSDR Persistence Data Store
     * @return
     */
    // JW: TODO: Refactor 
    public static TsdrLogPersistenceService getTSDRLogPersistenceDataStore( ){
        log.debug("Entering getTSDRLogPersistenceDataStore()");
        if(logPersistenceService == null){
            logPersistenceService = TsdrPersistenceServiceUtil.getTsdrLogPersistenceService();
            if(logPersistenceService == null) {
                log.error("logPersistenceService is found to be null");
            }
        }

        log.debug("Exiting getTSDRLogPersistenceDataStore()");
        return logPersistenceService;
    }
}
