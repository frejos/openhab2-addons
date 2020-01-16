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
package org.openhab.binding.bondhome.internal;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link BondHomeBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class BondHomeBindingConstants {

    private static final String BINDING_ID = "bondhome";

    /**
     * List of all Thing Type UIDs.
     */
    public static final ThingTypeUID THING_TYPE_BOND_BRIDGE = new ThingTypeUID(BINDING_ID, "bondBridge");
    public static final ThingTypeUID THING_TYPE_BOND_FAN = new ThingTypeUID(BINDING_ID, "bondFan");
    public static final ThingTypeUID THING_TYPE_BOND_SHADES = new ThingTypeUID(BINDING_ID, "bondShades");
    public static final ThingTypeUID THING_TYPE_BOND_FIREPLACE = new ThingTypeUID(BINDING_ID, "bondFireplace");
    public static final ThingTypeUID THING_TYPE_BOND_GENERIC = new ThingTypeUID(BINDING_ID, "bondGenericThing");

    /**
     * The supported thing types.
     */
    public static final Set<ThingTypeUID> SUPPORTED_DEVICE_TYPES = Stream
            .of(THING_TYPE_BOND_FAN, THING_TYPE_BOND_SHADES, THING_TYPE_BOND_FIREPLACE, THING_TYPE_BOND_GENERIC)
            .collect(Collectors.toSet());

    public static final Set<ThingTypeUID> SUPPORTED_BRIDGE_TYPES = Collections.singleton(THING_TYPE_BOND_BRIDGE);

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Stream
            .of(THING_TYPE_BOND_BRIDGE, THING_TYPE_BOND_FAN, THING_TYPE_BOND_SHADES, THING_TYPE_BOND_FIREPLACE, THING_TYPE_BOND_GENERIC)
            .collect(Collectors.toSet());

    /**
     * List of all Channel ids
     */
    public static final String CHANNEL_POWER_STATE = "power";
    public static final String CHANNEL_FAN_SPEED = "fanSpeed";
    public static final String CHANNEL_FAN_LIGHT_STATE = "fanLightState";
    public static final String CHANNEL_LIGHT_BRIGHTNESS = "fanLightBrightness";
    public static final String CHANNEL_FAN_UP_LIGHT_STATE = "fanUpLightState";
    public static final String CHANNEL_UP_LIGHT_BRIGHTNESS = "fanUpLightBrightness";
    public static final String CHANNEL_FAN_DOWN_LIGHT_STATE = "fanDownLightState";
    public static final String CHANNEL_DOWN_LIGHT_BRIGHTNESS = "fanDownLightBrightness";
    public static final String CHANNEL_FLAME = "flame";
    public static final String CHANNEL_FAN_STATE = "fanState";
    public static final String CHANNEL_OPEN_CLOSE = "open";

    /**
     * Configuration arguments
     */
    public static final String CONFIG_BOND_ID = "bondId";
    public static final String CONFIG_DEVICE_ID = "deviceId";
    public static final String CONFIG_IP_ADDRESS = "bondIpAddress";
    public static final String CONFIG_LOCAL_TOKEN = "localToken";

    /**
     * Constants
     */
    public static final int BOND_BPUP_PORT = 30007;
}
