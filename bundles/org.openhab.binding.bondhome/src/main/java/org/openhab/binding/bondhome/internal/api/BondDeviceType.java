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

/**
 * This enum represents the possible color modes for WiZ bulbs. The bulbs come
 * in three types - full color with tunable white, tunable white, and dimmable
 * with set white. The full color and tunable white bulbs operate EITHER in
 * color mode OR in tunable white mode.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 *
 */
public enum BondDeviceType {
    ceilingFan("CF"),
    fireplace("FP"),
    motorizedShades("MS"),
    genericDevice("GX");

    private String deviceType;

    private BondDeviceType(final String deviceType) {
        this.deviceType = deviceType;
    }

    /**
     * Gets the device type name for request deviceType
     *
     * @return the deviceType name
     */
    public String getDeviceType() {
        return deviceType;
    }
}
