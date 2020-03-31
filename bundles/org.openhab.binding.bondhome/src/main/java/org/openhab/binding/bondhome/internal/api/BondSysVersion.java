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

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * This POJO represents the version information of the bond bridge
 *
 * The incoming JSON looks like this:
 *
 * {"target": "snowbird", "fw_ver": "v2.5.2", "fw_date": "Fri Feb 22 14:13:25
 * -03 2019", "make": "Olibra LLC", "model": "model", "branding_profile":
 * "O_SNOWBIRD", "uptime_s": 380, "_": "c342ae74"}
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class BondSysVersion {
    // The current state hash
    @SerializedName("_")
    @Expose(serialize = false, deserialize = true)
    public String hash = API_HASH;

    @Expose(serialize = true, deserialize = true)
    public String target = BOND_BRIDGE_TARGET;

    @Expose(serialize = true, deserialize = true)
    public String fw_ver = BOND_LAST_KNOWN_FIRMWARE;

    @Expose(serialize = true, deserialize = true)
    public String fw_date = BOND_LAST_KNOWN_FIRMWARE_DATE;

    @Expose(serialize = true, deserialize = true)
    public String make = BOND_BRIDGE_MAKE;

    @Expose(serialize = true, deserialize = true)
    public String model = BOND_BRIDGE_MODEL;

    @Expose(serialize = true, deserialize = true)
    public String branding_profile = BOND_BRIDGE_BRANDING;

    @Expose(serialize = true, deserialize = true)
    public String bondid = API_MISSING_BOND_ID;

    @Expose(serialize = true, deserialize = true)
    public Boolean upgrade_http = true;

    @Expose(serialize = true, deserialize = true)
    public int api;

    @Expose(serialize = true, deserialize = true)
    public int uptime_s;
}
