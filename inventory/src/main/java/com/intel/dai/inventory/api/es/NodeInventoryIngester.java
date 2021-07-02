// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.inventory.api.es;

import com.google.gson.Gson;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.HWInvDbApi;
import com.intel.dai.dsapi.pojo.Dimm;
import com.intel.dai.dsapi.pojo.FruHost;
import com.intel.dai.dsapi.pojo.NodeInventory;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;

import java.util.List;
import java.util.Map;

public class NodeInventoryIngester {
    private final Logger log_;
    protected HWInvDbApi onlineInventoryDatabaseClient_;                // voltdb
    private final static Gson gson = new Gson();
    private long numberNodeInventoryJsonIngested = 0;

    NodeInventoryIngester(DataStoreFactory factory, Logger log) {
        log_ = log;
        onlineInventoryDatabaseClient_ = factory.createHWInvApi();
        onlineInventoryDatabaseClient_.initialize();
    }

    void constructAndIngestNodeInventoryJson(FruHost fruHost) throws DataStoreException {
        log_.info("Constructing node inventory from %s", fruHost.hostname);
        NodeInventory nodeInventory = new NodeInventory(fruHost);

        Map<String, String> dimmJsons = onlineInventoryDatabaseClient_.getDimmJsonsOnFruHost(fruHost.mac);
        for (String location : dimmJsons.keySet()) {
            addDimmJsonsToFruHostJson(nodeInventory, location, dimmJsons.get(location));
        }

        numberNodeInventoryJsonIngested += onlineInventoryDatabaseClient_.ingest(nodeInventory);
    }

    void addDimmJsonsToFruHostJson(NodeInventory nodeInventory, String location, String json) {
        log_.debug("  Adding %s => %s", location, json);
        switch (location) {
            case "CPU0_DIMM_A1":
                nodeInventory.CPU0_DIMM_A1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_B1":
                nodeInventory.CPU0_DIMM_B1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_C1":
                nodeInventory.CPU0_DIMM_C1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_D1":
                nodeInventory.CPU0_DIMM_D1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_E1":
                nodeInventory.CPU0_DIMM_E1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_F1":
                nodeInventory.CPU0_DIMM_F1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_G1":
                nodeInventory.CPU0_DIMM_G1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_H1":
                nodeInventory.CPU0_DIMM_H1 = gson.fromJson(json, Dimm.class);
                break;

            case "CPU1_DIMM_A1":
                nodeInventory.CPU1_DIMM_A1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_B1":
                nodeInventory.CPU1_DIMM_B1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_C1":
                nodeInventory.CPU1_DIMM_C1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_D1":
                nodeInventory.CPU1_DIMM_D1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_E1":
                nodeInventory.CPU1_DIMM_E1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_F1":
                nodeInventory.CPU1_DIMM_F1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_G1":
                nodeInventory.CPU1_DIMM_G1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_H1":
                nodeInventory.CPU1_DIMM_H1 = gson.fromJson(json, Dimm.class);
                break;

            default:
                log_.error("Unknown location %s", location);
        }
    }

    void ingestInitialNodeInventoryHistory() {
        List<FruHost> fruHosts = onlineInventoryDatabaseClient_.enumerateFruHosts();
        if (fruHosts == null) {
            log_.error("fruHosts is null");
            return;
        }

        for (FruHost fruHost : fruHosts) {
            try {
                constructAndIngestNodeInventoryJson(fruHost);
            } catch (DataStoreException e) {
                log_.error("DataStoreException: %s", e.getMessage());
            }
        }
    }

    public long getNumberNodeInventoryJsonIngested() {
        return numberNodeInventoryJsonIngested;
    }
}