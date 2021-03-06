/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.model;

import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;

/**
 * This class stores the constants that are shared among TSDR components.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Feb 24, 2015
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 *
 */
public class TSDRConstants {
    public TSDRConstants(){
    }
    /**
     * Keys used in a TSDR Record.
    */
    public static final String FLOW_TABLE_KEY_NAME = "TableID";
    public static final String FLOW_KEY_NAME = "FlowID";
    public static final String INTERNFACE_KEY_NAME = "PortID";
    public static final String QUEUE_KEY_NAME = "QueueID";
    public static final String GROUP_KEY_NAME = "GroupID";
    public static final String METER_KEY_NAME = "MeterID";
    public static final String BUCKET_KEY_NAME = "BucketID";
    public static final String ID_SPLIT = ".";
    public static final String ID_SPLIT_ARG = "_";

    /**
     * Default timeout for persistence service start and stop
     */

    public static final int STOP_PERSISTENCE_SERVICE_TIMEOUT = 1000;
    public static final int START_PERSISTENCE_SERVICE_TIMEOUT = 1000;

    /**
     * Default max number of results to return from ListMetrics command
     */
    public static final int MAX_RESULTS_FROM_LIST_METRICS_COMMAND = 1000;


    /**
     * constants for metrics categories
     */
    /*Why do we need this???? we have DataCategory.name() **
    public static final String FLOW_STATS_CATEGORY_NAME = "FlowStats";
    public static final String FLOW_TABLE_STATS_CATEGORY_NAME = "FlowTableStats";;
    public static final String PORT_STATS_CATEGORY_NAME = "PortStats";
    public static final String QUEUE_STATS_CATEGORY_NAME = DataCategory.QUEUESTATS.name();
    public static final String FLOW_GROUP_STATS_CATEGORY_NAME = "FlowGroupStats";
    public static final String FLOW_METER_STATS_CATEGORY_NAME = "FlowMeterStats";
    public static final String SYSLOG_CATEGORY_NAME = "SysLog";
    public static final String NETFLOW_CATEGORY_NAME = "NetFlow";
    public static final String SNMPINTERFACE_CATEGORY_NAME = "SNMPInterfaces";
    public static final String EXTERNAL = "External";
    */
    public static final String ROWKEY_SPLIT = "_";
}
