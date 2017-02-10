/*
 * Copyright (c) 2017 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.memory;

import java.io.IOException;
import java.util.Map;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.opendaylight.tsdr.spi.persistence.TSDRMetricPersistenceService;
import org.opendaylight.tsdr.spi.util.ConfigFileUtil;
import org.osgi.framework.BundleContext;

import com.google.common.annotations.VisibleForTesting;

public class Activator extends DependencyActivatorBase {

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        final TSDRMemoryMetricPersistenceServiceImpl impl = new TSDRMemoryMetricPersistenceServiceImpl();
        Map<String,String> props = loadConfig();
        if(ConfigFileUtil.isMetricPersistenceEnabled(props)) {
            manager.add(createComponent().setInterface(
                    new String[]{TSDRMetricPersistenceService.class.getName()}, null)
                    .setImplementation(impl));
        }
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
    }

    @VisibleForTesting
    protected Map<String,String> loadConfig() throws IOException {
        return ConfigFileUtil.loadConfig(ConfigFileUtil.MEMORY_STORE_CONFIG_FILE);
    }
}
