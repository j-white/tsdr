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
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;

public interface TsdrLogPersistenceService extends TsdrPersistenceService {

    /**
     * Store TSDRMetricRecord.
     * @param logRecord - an instane of tsdr log record
     */
    void store(TSDRLogRecord logRecord);

    /**
     * Returns the TSDRLogRecords based on category, startTime, and endTime
     * @param tsdrLogKey - The tsdr log key, can be also just Data Category
     * @param startTime - The starting time
     * @param endTime - The end time
     * @return - A list of log records
     */
    List<TSDRLogRecord> getTSDRLogRecords(String tsdrLogKey, long startTime, long endTime);

}
