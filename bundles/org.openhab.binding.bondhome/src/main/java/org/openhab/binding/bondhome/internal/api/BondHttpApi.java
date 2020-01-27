/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.bondhome.internal.api;

import static org.openhab.binding.bondhome.internal.BondHomeBindingConstants.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.HttpMethod;

import org.apache.commons.lang.Validate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.io.net.http.HttpUtil;
import org.openhab.binding.bondhome.internal.handler.BondBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * {@link BondHttpApi} wraps the Bond REST API and provides various low
 * level function to access the device api (not cloud api).
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class BondHttpApi {
    private final Logger logger = LoggerFactory.getLogger(BondHttpApi.class);
    private final BondBridgeHandler bridgeHandler;
    private final String thingName = "";
    private Gson gson = new Gson();

    public BondHttpApi(BondBridgeHandler bridgeHandler) {
        this.bridgeHandler = bridgeHandler;
    }

    /**
     * Gets version information about the Bond bridge
     *
     * @return the {@link BondSysVersion}
     * @throws IOException
     */
    @Nullable
    public BondSysVersion getBridgeVersion() throws IOException {
        String json = request("/v2/sys/version");
        logger.debug("BondHome device info : {}", json);
        return gson.fromJson(json, BondSysVersion.class);
    }

    /**
     * Gets a list of the attached devices
     *
     * @return an array of device id's
     * @throws IOException
     */
    @Nullable
    public List<String> getDevices() throws IOException {
        List<String> list = new ArrayList<>();
        String json = request("/v2/devices/");
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(json);
        JsonObject obj = element.getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> entries = obj.entrySet();
        for (Map.Entry<String, JsonElement> entry : entries) {
            if (!entry.getKey().equals("_")) {
                list.add(entry.getKey());
            }
        }
        return list;
    }

    /**
     * Gets basic device information
     *
     * @param deviceId The ID of the device
     * @return the {@link org.openhab.binding.bondhome.internal.api.BondDevice}
     * @throws IOException
     */
    @Nullable
    public BondDevice getDevice(String deviceId) throws IOException {
        String json = request("/v2/devices/" + deviceId);
        logger.debug("BondHome device info : {}", json);
        return gson.fromJson(json, BondDevice.class);
    }

    /**
     * Gets the current state of a device
     *
     * @param deviceId The ID of the device
     * @return the {@link org.openhab.binding.bondhome.internal.api.BondDeviceState}
     * @throws IOException
     */
    @Nullable
    public synchronized BondDeviceState getDeviceState(String deviceId) throws IOException {
        String json = request("/v2/devices/" + deviceId + "/state");
        logger.debug("BondHome device state : {}", json);
        return gson.fromJson(json, BondDeviceState.class);
    }

    /**
     * Gets the current properties of a device
     *
     * @param deviceId The ID of the device
     * @return the {@link org.openhab.binding.bondhome.internal.api.BondDeviceProperties}
     * @throws IOException
     */
    @Nullable
    public synchronized BondDeviceProperties getDeviceProperties(String deviceId) throws IOException {
        String json = request("/v2/devices/" + deviceId + "/properties");
        logger.debug("BondHome device properties : {}", json);
        return gson.fromJson(json, BondDeviceProperties.class);
    }

    /**
     * Executes a device action
     *
     * @param deviceId The ID of the device
     * @param actionId The Bond action
     * @return the {@link org.openhab.binding.bondhome.internal.api.BondDeviceProperties}
     */
    public synchronized void executeDeviceAction(String deviceId, BondDeviceAction action, @Nullable Integer argument) {
        String url = "http://" + bridgeHandler.getBridgeIpAddress() + "/v2/devices/" + deviceId + "/actions/"
                + action.getActionId();
        String payload = "{}";
        if (argument != null) {
            payload = "{\"argument\":" + String.valueOf(argument) + "}";
        }
        InputStream content = new ByteArrayInputStream(payload.getBytes());
        try {
            logger.debug("HTTP PUT to {} with content {}", url, payload);

            Properties headers = new Properties();
            headers.put("BOND-Token", bridgeHandler.getBridgeToken());

            String httpResponse = HttpUtil.executeUrl(HttpMethod.PUT, url, headers, content, "application/json",
                    BOND_API_TIMEOUT_MS);
            logger.debug("HTTP response from {}: {}", thingName, httpResponse);
        } catch (IOException ignored) {
            logger.warn("Unable to execute device action!");
        }
    }

    /**
     * Submit GET request and return response, check for invalid responses
     *
     * @param uri: URI (e.g. "/settings")
     */
    private String request(String uri) throws IOException {
        String httpResponse = "ERROR";
        String url = "http://" + bridgeHandler.getBridgeIpAddress() + uri;
        try {
            logger.debug("HTTP GET for to {}", url);

            Properties headers = new Properties();
            headers.put("BOND-Token", bridgeHandler.getBridgeToken());

            httpResponse = HttpUtil.executeUrl(HttpMethod.GET, url, headers, null, "", BOND_API_TIMEOUT_MS);
            Validate.notNull(httpResponse, "httpResponse must not be null");
            // all api responses are returning the result in Json format. If we are getting
            // something else it must
            // be an error message, e.g. http result code
            if (httpResponse.contains(API_ERR_HTTP_401_UNAUTHORIZED)) {
                throw new IOException(
                        API_ERR_HTTP_401_UNAUTHORIZED + ", set/correct local token in the thing/binding config");
            }
            if (httpResponse.contains(API_ERR_HTTP_404_NOTFOUND)) {
                throw new IOException(
                        API_ERR_HTTP_404_NOTFOUND + ", set/correct device ID in the thing/binding config");
            }
            if (!httpResponse.startsWith("{") && !httpResponse.startsWith("[")) {
                throw new IOException("Unexpected http response: " + httpResponse);
            }

            logger.debug("HTTP response from {}: {}", thingName, httpResponse);
            return httpResponse;
        } catch (IOException e) {
            if (e.getMessage().contains("Timeout")) {
                throw new IOException("Bond API call failed: Timeout (" + BOND_API_TIMEOUT_MS + " ms)");
            } else {
                throw new IOException("Bond API call failed: " + e.getMessage() + ", url=" + url);
            }
        }
    }
}
