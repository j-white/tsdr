/*
 * Copyright (c) 2016 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.newts;

import java.util.Map;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.opendaylight.tsdr.spi.persistence.TSDRMetricPersistenceService;
import org.opendaylight.tsdr.spi.util.ConfigFileUtil;
import org.osgi.framework.BundleContext;

public class Activator extends DependencyActivatorBase {

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        final TSDRNewtsPersistenceServiceImpl impl = new TSDRNewtsPersistenceServiceImpl();
        Map<String,String> props = ConfigFileUtil.loadConfig(ConfigFileUtil.NEWTS_STORE_CONFIG_FILE);
        if(ConfigFileUtil.isMetricPersistenceEnabled(props)) {
            manager.add(createComponent().setInterface(
                    new String[]{TSDRMetricPersistenceService.class.getName()}, null)
                    .setImplementation(impl));
        }
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
        // pass
    }
}