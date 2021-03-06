/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.tsdr.controller.metrics.collector;

import org.opendaylight.tsdr.collectors.cmc.ControllerMetricCollector;
import org.opendaylight.tsdr.collectors.cmc.CpuDataCollector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TSDRCMCModule extends org.opendaylight.controller.config.yang.config.tsdr.controller.metrics.collector.AbstractTSDRCMCModule
        implements AutoCloseable{

    private static final Logger logger = LoggerFactory.getLogger(TSDRCMCModule.class);
    private TsdrCollectorSpiService collectorSPIService = null;
    private ControllerMetricCollector collectorImpl = null;

    public TSDRCMCModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TSDRCMCModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.tsdr.controller.metrics.collector.TSDRCMCModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        super.customValidation();
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        collectorImpl = new ControllerMetricCollector(this, CpuDataCollector.getCpuDataCollector());
        logger.info("Controller Metrics Collector started!");
        return this;
    }

    @Override
    public void close() throws Exception {
        collectorImpl.interrupt();
        logger.info("Controller Metrics Collector stopped!");
    }

    public TsdrCollectorSpiService getTSDRCollectorSPIService(){
        getDataBrokerDependency();
        if(collectorSPIService==null){
            collectorSPIService = getRpcRegistryDependency().getRpcService(TsdrCollectorSpiService.class);
        }
        return this.collectorSPIService;
    }
}
