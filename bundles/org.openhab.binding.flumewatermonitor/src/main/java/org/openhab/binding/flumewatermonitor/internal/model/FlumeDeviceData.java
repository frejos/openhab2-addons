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
package org.openhab.binding.flumewatermonitor.internal.model;

import static org.openhab.binding.flumewatermonitor.internal.FlumeWaterMonitorBindingConstants.*;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link FlumeDeviceData} represents the device information retured by a
 * fetch device or fetch devices request.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */

@NonNullByDefault
public class FlumeDeviceData implements FlumeDataInterface {

    /**
     * The type of token - should always be "bearer".
     */
    @SerializedName("id")
    public long deviceId = MISSING_DEVICE_ID;

    /**
     * The email address associated with the user
     */
    @SerializedName("type")
    public FlumeDeviceType deviceType = FlumeDeviceType.FlumeSensor;

    /**
     * The numeric location id
     */
    @SerializedName("location_id")
    public int locationId;

    /**
     * Repeat of the user id used to request devices
     */
    @SerializedName("user_id")
    public long userId;

    /**
     * The bridge ID associated with a flume sensor.
     * This will be null if the device is a bridge.
     */
    @SerializedName("bridge_id")
    public long bridgeId;

    /**
     * A boolean indicating that a sensor is properly oriented on the water pipes. This will be null or omitted if the
     * device is a bridge.
     */
    @SerializedName("oriented")
    public boolean oriented;

    /**
     * The last time the Flume cloud "saw" a device.
     */
    @SerializedName("last_seen")
    public Date lastSeen = new Date();

    /**
     * A boolean indicating that a device is properly connected to the Flume cloud.
     */
    @SerializedName("connected")
    public boolean connected;

    /**
     * A text description of the current battery level of a Flume sensor. This will be omitted or null if the device is
     * a Flume bridge (which must be plugged into the wall).
     */
    @SerializedName("battery_level")
    public @Nullable String batteryLevel;

    /**
     * A text description of the product, probably "flume-1"
     */
    @SerializedName("product")
    public @Nullable String product;
}
