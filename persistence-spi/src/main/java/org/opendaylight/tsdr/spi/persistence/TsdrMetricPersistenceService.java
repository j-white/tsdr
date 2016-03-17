/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Cisco Systems,  Inc. and others.  All rights reserved.
 * Copyright (c) 2016 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.persistence;

import java.util.List;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;

public interface TsdrMetricPersistenceService extends TsdrPersistenceService {

    /**
     * Store TSDRMetricRecord.
     * @param metricRecord - An instance of tsdr metric record
     */
    void store(TSDRMetricRecord metricRecord);

    /**
     * Returns the list of metrics based on startDateTime and endDateTime
     * If startDateTime OR(/AND)  endDateTime is not specified returns the recent
     * predefined N metrics
     *
     * @param tsdrMetricKey -- The tsdr metric key, can also be just Data Category,
     * @param startDateTime  --The start time in milis
     * @param endDateTime   -- The end time in milis
     * @return - List of persistence store dependents records
     */
    //format of tsdrMetricKey is "[NID=<node id>][DC=<data category>][MN=<metric name>][RK=<a list or record keys>][TS=<timestamp - for hbase>]"
    List<TSDRMetricRecord> getTSDRMetricRecords(String tsdrMetricKey,long startDateTime, long endDateTime);

}
