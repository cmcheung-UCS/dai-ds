// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory;

import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.network_listener.NetworkListenerConfig;
import com.intel.dai.network_listener.NetworkListenerCore;
import com.intel.logging.Logger;
import com.intel.perflogging.BenchmarkHelper;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import com.intel.xdg.XdgConfigFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Description of class NetworkAdapterInventoryBase.
 */
abstract class AdapterInventoryNetworkBase {
    AdapterInventoryNetworkBase(Logger logger, DataStoreFactory factory, AdapterInformation info,
                                String benchmarkingFile, long maxBurstSeconds) {
        log_ = logger;
        factory_ = factory;
        adapter_ = info;
        benchmarking_ = new BenchmarkHelper(info.getType(), benchmarkingFile, maxBurstSeconds);
    }

    static InputStream getConfigStream(String baseFilename) throws FileNotFoundException {
        XdgConfigFile xdg = new XdgConfigFile("ucs");
        InputStream result = xdg.Open(baseFilename);
        if(result == null)
            throw new FileNotFoundException("Failed to locate or open '" + baseFilename + "'");
        return result;
    }

    protected boolean execute(NetworkListenerCore adapterCore) {
        Runtime.getRuntime().addShutdownHook(new Thread(adapterCore::shutDown));
        return adapterCore.run() == 0;
    }

    boolean entryPoint(InputStream configStream) throws IOException, ConfigIOParseException {
        NetworkListenerConfig config = new NetworkListenerConfig(adapter_, log_);
        config.loadFromStream(configStream);
        NetworkListenerCore adapterCore = new NetworkListenerCore(log_, config, factory_, benchmarking_);
        return execute(adapterCore);
    }

    void postInitialize() { }

    /**
     * Ingests HW inventory data into database.  Note that if postgres already contains inventory data
     * only a patching of the inventory data is performed.
     */
    void preInitialize(InputStream configStream) throws IOException, ConfigIOParseException {
        log_.info("Starting InventoryUpdateThread ...");
        if (configStream == null) {
            log_.error("configStream is null.  preInitialize exiting.");
            return;
        }

        NetworkListenerConfig config = new NetworkListenerConfig(adapter_, log_);
        config.loadFromStream(configStream);
        log_.info("Starting InventoryUpdateThread");
        Thread t = new Thread(new InventoryUpdateThread(log_, config));
        t.start();  // background updates of HW inventory
    }

    private final AdapterInformation adapter_;
    private final Logger log_;
    private final DataStoreFactory factory_;
    private final BenchmarkHelper benchmarking_;
    static final String ADAPTER_TYPE = "INVENTORY";
}
