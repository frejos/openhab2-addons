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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link FlumeDeviceType} represents the possible device types returned by the Flume API.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 *
 */

@NonNullByDefault
public enum FlumeDeviceType {
    @SerializedName("1")
    FlumeBridge(THING_TYPE_FLUME_BRIDGE),

    @SerializedName("2")
    FlumeSensor(THING_TYPE_FLUME_SENSOR);

    private ThingTypeUID deviceTypeUid;

    private FlumeDeviceType(final ThingTypeUID deviceTypeUid) {
        this.deviceTypeUid = deviceTypeUid;
    }

    /**
     * Gets the device type name for request deviceType
     *
     * @return the deviceType name
     */
    public ThingTypeUID getThingTypeUID() {
        return deviceTypeUid;
    }
}
