/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Cisco Systems,  Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.persistence;

import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;

/**
 * This interface provides commons APIs implemented by the different persistence services types.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Feb 24, 2015
 *
 * Modified: Mar 18, 2015
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 *
 * Modified: Oct 7, 2015
 *
 * @author <a href="mailto:saichler@cisco.com">Sharon Aicler</a>
 *
 */
public interface TsdrPersistenceService {

    /**
     * Starts the persistence service
     *
     * @param timeout -- indicates the time given for the starting of persistence,
     * after which the caller will consider it non-funcational
     */
    void start(int timeout);

    /**
     * Stops the persistence service
     *
     * @param timeout -- indicates the time given for the stopping of persistence,
     * after which the caller will assume its stopped
     */
    void stop(int timeout);

    /**
     * Purges the data from TSDR data store.
     * @param category -- the category of the data.
     * @param timestamp -- the retention time.
     */

    //TODO: change from Long to long if there is no specific reason for using Long.
    //TODO:change name of method to purge
    void purgeTSDRRecords(DataCategory category, Long timestamp);

    /**
     * Purges all the data from TSDR data store older than the
     * retention timestamp
     * @param timestamp - The time stamp
     */
    //TODO:change name to purgeAll and Long to long
    void purgeAllTSDRRecords(Long timestamp);
}
