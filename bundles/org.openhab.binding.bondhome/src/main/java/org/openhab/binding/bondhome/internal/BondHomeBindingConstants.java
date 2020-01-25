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

    public static final String BINDING_ID = "bondhome";

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

    /**
     * Configuration arguments
     */
    public static final String CONFIG_BOND_ID = "bondId";
    public static final String CONFIG_DEVICE_ID = "deviceId";
    public static final String CONFIG_IP_ADDRESS = "bondIpAddress";
    public static final String CONFIG_LOCAL_TOKEN = "localToken";

    /**
     * Device Properties
     */
    public static final String PROPERTIES_DEVICE_NAME = "deviceName";
    public static final String PROPERTIES_MAX_SPEED = "max_speed";
    public static final String PROPERTIES_TRUST_STATE = "trust_state";
    public static final String PROPERTIES_ADDRESS = "addr";
    public static final String PROPERTIES_RF_FREQUENCY = "freq";

    /**
     * Constants
     */
    public static final int BOND_BPUP_PORT = 30007;
    public static final int BOND_API_TIMEOUT_MS = 3000;
    public static final String API_ERR_HTTP_401_UNAUTHORIZED = "You need authentication credentials to continue";
    public static final String API_ERR_HTTP_404_NOTFOUND = "Resource not found";
    public static final String API_ERR_HTTP_500_SERVERERR = "Something unexpected happened";
}
