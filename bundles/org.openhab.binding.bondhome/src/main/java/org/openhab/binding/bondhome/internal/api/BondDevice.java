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
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.Expose;

/**
 * This POJO represents the "params" returned in a "firstBeat"
 *
 * The incoming JSON looks like this:
 *
 * {"name": "My Fan", "type": "CF", "template": "A1", "location": "Kitchen", "actions": {"_": "7fc1e84b"}, "properties": {"_": "84cd8a43"}, "state": {"_": "ad9bcde4"}, "commands": {"_": "ad9bcde4" }}
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class BondDevice {
    // The name associated with the device in the bond app
    @Expose(serialize = true, deserialize = true)
    public String name = "deviceName";
    // The device type
    @Expose(serialize = true, deserialize = true)
    public BondDeviceType type;
    // The remote control template being used
    @Expose(serialize = true, deserialize = true)
    public String template = "template";
    // The current hash of the actions object
    @Expose(serialize = false, deserialize = true)
    public @Nullable BondActionList actions;
    // The current hash of the properties object
    @Expose(serialize = false, deserialize = true)
    public @Nullable BondDeviceProperties properties;
    // The current hash of the state object
    @Expose(serialize = false, deserialize = true)
    public @Nullable BondDeviceState state;
    // The current hash of the commands object - only applies to a bridge
    @Expose(serialize = false, deserialize = true)
    public BondCommand commands = "commands";
}
