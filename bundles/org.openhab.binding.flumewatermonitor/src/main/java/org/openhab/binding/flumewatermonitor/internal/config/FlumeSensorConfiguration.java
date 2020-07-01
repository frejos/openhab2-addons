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
package org.openhab.binding.flumewatermonitor.internal.config;

import static org.openhab.binding.flumewatermonitor.internal.FlumeWaterMonitorBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link FlumeSensorConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class FlumeSensorConfiguration {

    /**
     * The long number that identifies a Flume device.
     */
    public long deviceId = MISSING_DEVICE_ID;

    /**
     * The frequency to ask for water use data updates.
     */
    public int waterUseInterval = DEFAULT_WATER_USE_INTERVAL_MINUTES;

    /**
     * The frequency to ask for device status changes - only used for battery level.
     */
    public int deviceStatusInterval = DEFAULT_DEVICE_STATUS_INTERVAL_MINUTES;
}
