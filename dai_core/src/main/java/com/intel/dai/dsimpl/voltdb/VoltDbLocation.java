// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.intel.dai.dsapi.Location;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.exceptions.BadInputException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;

import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.IOException;

/**
 * Handle VoltDB queries to retrieve location from a given node hostname
 * and viceversa.
 */
public class VoltDbLocation implements Location {

    public VoltDbLocation(Logger log, String[] servers) {
        bmcPattern_ = Pattern.compile("(..*)(bmc)$");
        log_ = log;
        locationMap_ = new HashMap<String,String>();
        hostMap_ = new HashMap<String,String>();
        ipMap_ = new HashMap<String,String>();
        macMap_ = new HashMap<String,String>();
        servers_ = servers;
    }

    public void initialize() {
        if(servers_ != null)
            VoltDbClient.initializeVoltDbClient(servers_);
        voltDb_ = getClient();
        try {
            reloadCache();
        } catch(DataStoreException e) {
            log_.exception(e);
        }
    }

    @Override
    public String getLocationFromHostname(String host) {
        // needed to get location from a bmc hostname
        Matcher m = bmcPattern_.matcher(host);
        if (m.matches())
            return getLocationFromHostname(m.group(1));
        return hostMap_.getOrDefault(host, "");
    }

    @Override
    public String getHostnameFromLocation(String lctn) {
        return locationMap_.getOrDefault(lctn, "");
    }

    /**
     * Get the location from the IP address specified.
     *
     * @param ip The IPv4 address to lookup.
     * @return String with the location or null if not found.
     */
    @Override
    public String getLocationFromIP(String ip) {
        return ipMap_.get(ip);
    }

    /**
     * Get the location from the MAC address specified.
     *
     * @param mac The MAC address to lookup.
     * @return String with the location or null if not found.
     */
    @Override
    public String getLocationFromMAC(String mac) {
        return macMap_.get(mac);
    }

    @Override
    public void reloadCache() throws DataStoreException {
        try {
            updateFromProcedure("ComputeNodeLocationInformation");
            updateFromProcedure("ServiceNodeLocationInformation");
            updateSystemLabel();
        }
        catch (IOException | ProcCallException  e) {
            log_.exception(e);
        }
    }

    @Override
    public PropertyMap getSystemLocations() {
        PropertyMap system = new PropertyMap();
        system.put("node_locations", new PropertyMap(hostMap_));
        system.put("system", system_);
        return system;
    }

    public String getSystemLabel() {
        return system_;
    }

    protected Client getClient() {
        return VoltDbClient.getVoltClientInstance();
    }

    private void updateSystemLabel()
            throws IOException, ProcCallException, DataStoreException {
        ClientResponse response = voltDb_.callProcedure("MachineDescription");
        if (response.getStatus() != ClientResponse.SUCCESS) {
            String errMsg = String.format(
                    "Unable to retrieve node information from the data store. Client response status: %s",
                    response.getStatus());
            log_.error(errMsg);
            throw new DataStoreException(errMsg);
        }
        VoltTable vt = response.getResults()[0];
        if (vt.advanceRow())
            system_ = vt.getString("Description");
    }

    private void updateFromProcedure(String procedure)
            throws IOException, ProcCallException, DataStoreException {
        ClientResponse response = voltDb_.callProcedure(procedure);
        if (response.getStatus() != ClientResponse.SUCCESS) {
            String errMsg = String.format(
                    "Unable to retrieve node information from the data store. Client response status: %s",
                    response.getStatus());
            log_.error(errMsg);
            throw new DataStoreException(errMsg);
        }
        VoltTable vt = response.getResults()[0];
        for (int i = 0; i < vt.getRowCount(); i++) {
            vt.advanceRow();
            locationMap_.put(vt.getString("Lctn"), vt.getString("HostName"));
            locationMap_.put(vt.getString("Lctn") + "-BMC", vt.getString("BmcHostName"));
            hostMap_.put(vt.getString("HostName"), vt.getString("Lctn"));
            hostMap_.put(vt.getString("BmcHostName"), vt.getString("Lctn") + "-BMC");
            ipMap_.put(vt.getString("IpAddr"), vt.getString("Lctn"));
            macMap_.put(vt.getString("MacAddr"), vt.getString("Lctn"));
        }
    }

    public Set<String> getLocationsFromNodes(Set<String> nodes) throws BadInputException {
        Set<String> locations = new HashSet<>();
        for(String node : nodes){
            if (hostMap_.containsKey(node)) {
                locations.add(hostMap_.get(node));
                continue;
            }

            if(locationMap_.containsKey(node)) {
                locations.add(node);
                continue;
            }

            log_.error(node+ " is not a valid location or hostname of a node");
            throw new BadInputException(node + " is a Bad input. It isn't a location or hostname of a node");
        }
        return locations;
    }

    public Set<String> getNodesFromLocations(Set<String> locations) throws BadInputException {
        Set<String> nodes = new HashSet<>();
        for(String lctn : locations){
            if (locationMap_.containsKey(lctn)) {
                nodes.add(locationMap_.get(lctn));
                continue;
            }

            if(hostMap_.containsKey(lctn)) {
                nodes.add(lctn);
                continue;
            }

            log_.error(lctn+ " is not a valid location or hostname of a node");
            throw new BadInputException(lctn + " is a Bad input. It isn't a location or hostname of a node");
        }
        return nodes;
    }

    private Client voltDb_;
    private Logger log_;
    private Map<String, String> locationMap_;
    private Map<String, String> hostMap_;
    private Map<String, String> ipMap_;
    private Map<String, String> macMap_;
    private Pattern bmcPattern_;
    private String system_;
    private String[] servers_;
}
