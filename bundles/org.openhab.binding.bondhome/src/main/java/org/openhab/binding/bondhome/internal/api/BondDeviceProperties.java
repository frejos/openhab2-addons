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

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.annotations.Expose;

/**
 * This POJO represents the "params" returned in a "firstBeat"
 *
 * The incoming JSON looks like this:
 *
 * {"max_speed": 3, "trust_state": false, "addr": "10101", "freq": 434300, "bps": 3000, "zero_gap": 30}
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class BondDeviceProperties {
    // The maximum speed of a fan
    @Expose(serialize = true, deserialize = true)
    public int max_speed;
    // Whether or not to "trust" that the device state remembered by the bond bridge is
    // correct for toggle switches
    @Expose(serialize = true, deserialize = true)
    public boolean trust_state;
    // The device address
    @Expose(serialize = true, deserialize = true)
    public int addr;
    // The fan radio frequency
    @Expose(serialize = true, deserialize = true)
    public int freq;
    // The fan speed
    @Expose(serialize = true, deserialize = true)
    public int bps;
    // The fan timer value
    @Expose(serialize = true, deserialize = true)
    public int zero_gap;
}
