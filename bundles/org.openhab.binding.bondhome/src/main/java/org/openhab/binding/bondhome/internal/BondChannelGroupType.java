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

import static org.openhab.binding.bondhome.internal.BondHomeBindingConstants.*;

import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;

/**
 * This enum represents the possible device actions
 *
 * @author Sara Geleskie Damiano - Initial contribution
 *
 */
public enum BondChannelGroupType {

    common(new ChannelGroupTypeUID(BINDING_ID, "common"), "Common"),

    ceilingFan(new ChannelGroupTypeUID(BINDING_ID, "ceilingFan"), "Ceiling Fan"),

    light(new ChannelGroupTypeUID(BINDING_ID, "light"), "Light"),

    upLight(new ChannelGroupTypeUID(BINDING_ID, "upLight"), "Up Light"),

    downLight(new ChannelGroupTypeUID(BINDING_ID, "downLight"), "Down Light"),

    fireplace(new ChannelGroupTypeUID(BINDING_ID, "fireplace"), "Fireplace"),

    shade(new ChannelGroupTypeUID(BINDING_ID, "shade"), "Motorized Shades");

    ChannelGroupTypeUID groupTypeUid;
    String label;

    private BondChannelGroupType(ChannelGroupTypeUID groupTypeUid, String label) {
        this.groupTypeUid = groupTypeUid;
        this.label = label;
    }

    /**
     * @return the group type UID
     */
    public ChannelGroupTypeUID getGroupTypeUID() {
        return groupTypeUid;
    }

    /**
     * @return the group type UID
     */
    public String getGroupTypeId() {
        return groupTypeUid.getId();
    }

    /**
     * @return the group type label
     */
    public String getGroupTypeLabel() {
        return label;
    }
}
