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
package org.openhab.binding.flumewatermonitor.internal;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link FlumeWaterMonitorBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class FlumeWaterMonitorBindingConstants {

    private static final String BINDING_ID = "flumewatermonitor";
    public static final String CURRENT_BINDING_VERSION = "v0.01.19";

    // List of all Thing Type UIDs
    /**
     * The flume account for API access
     */
    public static final ThingTypeUID THING_TYPE_FLUME_ACCOUNT = new ThingTypeUID(BINDING_ID, "flumeAccount");
    /**
     * A flume radio-to-WiFi bridge.
     *
     * @note This **DOES NOT** function as a "bridge" in the sense of an OpenHab
     *       bridge thing. Because all API access to Flume data is through the
     *       account on the cloud, the account itself operates as an OpenHab bridge
     *       and the Flume bridge is not used.
     */
    public static final ThingTypeUID THING_TYPE_FLUME_BRIDGE = new ThingTypeUID(BINDING_ID, "flumeBridge");
    /**
     * A flume water sensor.
     */
    public static final ThingTypeUID THING_TYPE_FLUME_SENSOR = new ThingTypeUID(BINDING_ID, "flumeSensor");

    /**
     * The supported thing types.
     */
    public static final Set<ThingTypeUID> SUPPORTED_DEVICE_TYPES = Collections.singleton(THING_TYPE_FLUME_SENSOR);

    public static final Set<ThingTypeUID> SUPPORTED_BRIDGE_TYPES = Collections.singleton(THING_TYPE_FLUME_ACCOUNT);

    // List of all Channel ids
    public static final String CHANNEL_USAGE = "waterUse";
    public static final String CHANNEL_WATER_ON = "isWaterOn";
    public static final String CHANNEL_BATTERY = "batteryLevel";

    // -------------- Configuration arguments ----------------
    /**
     * Flume account user name.
     */
    public static final String CONFIG_USER_NAME = "flumeUser";
    public static final String MISSING_USER_NAME = "abc123";

    /**
     * Flume account password.
     */
    public static final String CONFIG_PASSWORD = "flumePass";
    public static final String MISSING_PASSWORD = "abc123";

    /**
     * Flume API access client ID.
     */
    public static final String CONFIG_CLIENT_ID = "clientId";
    public static final String MISSING_CLIENT_ID = "abc123";

    /**
     * Flume API access client secret.
     */
    public static final String CONFIG_CLIENT_SECRET = "clientSecret";
    public static final String MISSING_CLIENT_SECRET = "abc123";

    /**
     * Device ID for a single Flume sensor
     */
    public static final String CONFIG_DEVICE_ID = "deviceId";
    public static final long MISSING_DEVICE_ID = 1;

    /**
     * Interval to refresh water use
     */
    public static final String CONFIG_WATER_USE_INTERVAL = "waterUseInterval";
    public static final int DEFAULT_WATER_USE_INTERVAL_MINUTES = 1;

    /**
     * Interval to refresh device status (ie, battery voltage)
     */
    public static final String CONFIG_DEVICE_STATUS_INTERVAL = "deviceStatusInterval";
    public static final int DEFAULT_DEVICE_STATUS_INTERVAL_MINUTES = 1;

    // -------------- Default values ----------------
    /**
     * How long before active discovery times out.
     */
    public static final int DISCOVERY_TIMEOUT_SECONDS = 2;

    // -------------- Constants Used ----------------

    /**
     * All requests for data values must be given a request id. Multiple requests
     * for values can be made within the same HTTP API call - in this case the
     * result will contain the values grouped by the request id. For simplicity in
     * this binding, we will only make one request at a time and always use this
     * request id.
     */
    public static final String FLUME_QUERY_REQUEST_ID = "openHabRequest";
    /**
     * When requesting JWT access tokens, we need the grant type, which must be "password".
     */
    public static final String FLUME_TOKEN_GRANT_TYPE = "password";
    /**
     * The endpoint for all API calls (except for authentication),
     */
    public static final String FLUME_API_ENDPOINT = "https://api.flumetech.com/";

    // -------------- Device Properties ----------------

    /**
     * The current version of this binding.
     */
    public static final String PROPERTY_BINDING_VERSION = "bindingVersion";
}
