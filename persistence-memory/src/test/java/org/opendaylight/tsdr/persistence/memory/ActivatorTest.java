/*
 * Copyright (c) 2017 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.memory;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Map;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.tsdr.persistence.memory.Activator;
import org.opendaylight.tsdr.spi.util.ConfigFileUtil;

import com.google.common.collect.ImmutableMap;

public class ActivatorTest {

    private static class AlwaysEnabledActivator extends Activator {
        @Override
        protected Map<String,String> loadConfig() throws IOException {
            return new ImmutableMap.Builder<String,String>()
                        .put(ConfigFileUtil.METRIC_PERSISTENCE_PROPERTY, Boolean.TRUE.toString())
                        .build();
        }
    };

    @Test
    public void initAndDestroy() throws Exception {
        // Mock the necessary components
        DependencyManager manager = mock(DependencyManager.class);
        Component component = mock(Component.class, RETURNS_DEEP_STUBS);
        Activator activator = Mockito.spy(new AlwaysEnabledActivator());
        Mockito.doReturn(component).when(activator).createComponent();

        // Initialize the activator, verify that we added some component, and destroy
        activator.init(null, manager);
        verify(manager, times(1)).add(any());
        activator.destroy(null, manager);
    }
 
    @Test(expected=IOException.class)
    public void loadConfig() throws IOException {
        // The configuration file is not accessible in the test
        // context, so we expect this to throw an IOException
        new Activator().loadConfig();
    }
}
