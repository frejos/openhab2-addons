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

import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentBuilder;
import org.eclipse.smarthome.core.types.StateOption;

/**
 * This enum represents the possible device actions
 *
 * @author Sara Geleskie Damiano - Initial contribution
 *
 */
public enum BondChannelType {

    power(ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "power"), "Power", "Switch")
            .withDescription("Controls device power").withCategory("Switch").isAdvanced(false).build()),

    timer(ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "timer"), "Timer", "Number")
            .withDescription("Starts a timer for s seconds. If power if off, device is implicitly turned on")
            .withCategory("Time").isAdvanced(false).build()),

    fanSpeed(ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "fanSpeed"), "Speed", "Number")
            .withDescription("Sets fan speed").withCategory("Heating").isAdvanced(false).build()),

    breezeState(ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "breezeState"), "Breeze", "Switch")
            .withDescription("Enables or disables breeze mode").isAdvanced(false).build()),

    breezeMean(ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "breezeMean"), "Mean Breeze Speed", "Dimmer")
            .withDescription(
                    "Sets the average speed in breeze mode. 0 = minimum average speed (calm), 100 = maximum average speed (storm)")
            .isAdvanced(false).build()),

    breezeVariability(ChannelTypeBuilder
            .state(new ChannelTypeUID(BINDING_ID, "breezeVariability"), "Mean Breeze Speed", "Dimmer")
            .withDescription(
                    "Sets the variability of the speed in breeze mode. 0 = minimum variation (steady), 100 = maximum variation (gusty)")
            .isAdvanced(false).build()),

    direction(ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "direction"), "Direction", "String")
            .withDescription(
                    "Changes the fan direction. forward or reverse. The forward and reverse modes are sometimes called Summer and Winter")
            .withStateDescription(
                    StateDescriptionFragmentBuilder.create().withOption(new StateOption("summer", "Summer/Forward"))
                            .withOption(new StateOption("winter", "Winter/Reverse")).build().toStateDescription())
            .isAdvanced(false).build()),

    light(ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "light"), "Light", "Switch")
            .withDescription("Turns the light on the ceiling fan on or off").withCategory("Light").isAdvanced(false)
            .build()),

    brightness(ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "brightness"), "Brightness", "Dimmer")
            .withDescription("Adjusts the brightness of the fan light").withCategory("Light").isAdvanced(false)
            .build()),

    dimmerStartStop(ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "dimmerStartStop"), "Light", "Switch")
            .withDescription(
                    "Starts or stops changing the brightness of the ceiling fan light. The direction of dimming cannot be controlled.")
            .withCategory("Light").isAdvanced(false).build()),

    dimmerIncr(ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "dimmerIncr"), "Light", "Switch")
            .withDescription("Starts or stops increasing the brightness of the ceiling fan light.")
            .withCategory("Light").isAdvanced(false).build()),

    dimmerDcr(ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "dimmerDcr"), "Light", "Switch")
            .withDescription("Starts or stops decreasing the brightness of the ceiling fan light.")
            .withCategory("Light").isAdvanced(false).build()),

    upLight(ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "upLight"), "Light", "Switch").withDescription(
            "Enables or disables the up light of the ceiling fan. The light must also be on to turn on the up light.")
            .withCategory("Light").isAdvanced(false).build()),

    upLightBrightness(
            ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "upLightBrightness"), "Brightness", "Dimmer")
                    .withDescription("Adjusts the brightness of the fan up light").withCategory("Light")
                    .isAdvanced(false).build()),

    upLightDimmerStartStop(ChannelTypeBuilder
            .state(new ChannelTypeUID(BINDING_ID, "upLightDimmerStartStop"), "Light", "Switch")
            .withDescription(
                    "Starts or stops changing the brightness of the ceiling fan up light. The direction of dimming cannot be controlled.")
            .withCategory("Light").isAdvanced(false).build()),

    upLightdimmerIncr(ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "upLightdimmerIncr"), "Light", "Switch")
            .withDescription("Starts or stops increasing the brightness of the ceiling fan up light.")
            .withCategory("Light").isAdvanced(false).build()),

    upLightdimmerDcr(ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "upLightdimmerDcr"), "Light", "Switch")
            .withDescription("Starts or stops decreasing the brightness of the ceiling fan up light.")
            .withCategory("Light").isAdvanced(false).build()),

    downLight(ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "downLight"), "Light", "Switch").withDescription(
            "Enables or disables the down light of the ceiling fan. The light must also be on to turn on the down light.")
            .withCategory("Light").isAdvanced(false).build()),

    downLightBrightness(
            ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "downLightBrightness"), "Brightness", "Dimmer")
                    .withDescription("Adjusts the brightness of the fan down light").withCategory("Light")
                    .isAdvanced(false).build()),

    downLightDimmerStartStop(ChannelTypeBuilder
            .state(new ChannelTypeUID(BINDING_ID, "downLightDimmerStartStop"), "Light", "Switch")
            .withDescription(
                    "Starts or stops changing the brightness of the ceiling fan down light. The direction of dimming cannot be controlled.")
            .withCategory("Light").isAdvanced(false).build()),

    downLightdimmerIncr(
            ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "downLightdimmerIncr"), "Light", "Switch")
                    .withDescription("Starts or stops increasing the brightness of the ceiling fan down light.")
                    .withCategory("Light").isAdvanced(false).build()),

    downLightdimmerDcr(ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "downLightdimmerDcr"), "Light", "Switch")
            .withDescription("Starts or stops decreasing the brightness of the ceiling fan down light.")
            .withCategory("Light").isAdvanced(false).build()),

    flame(ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "flame"), "Flame Level", "Dimmer")
            .withDescription("Turns on or adjusts the flame level").withCategory("Heating").isAdvanced(false).build()),

    fpFanPower(ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "fpFanPower"), "Fireplace fan", "Switch")
            .withDescription("Turns the fireplace fan on or off").withCategory("Heating").isAdvanced(false).build()),

    fpFanSpeed(ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "fpFanSpeed"), "Fireplace fan", "Dimmer")
            .withDescription("Adjusts the speed of the fireplace fan").withCategory("Heating").isAdvanced(false)
            .build()),

    openShade(ChannelTypeBuilder.state(new ChannelTypeUID(BINDING_ID, "openShade"), "Open", "Switch")
            .withDescription("Opens or closes motorize shades").withCategory("Rollershutter").isAdvanced(false)
            .build());

    ChannelType channelType;

    private BondChannelType(ChannelType channelType) {
        this.channelType = channelType;
    }

    /**
     * @return the channelType
     */
    public ChannelType getChannelType() {
        return channelType;
    }

    /**
     * @return the channelTypeUID
     */
    public ChannelTypeUID getChannelTypeUID() {
        return channelType.getUID();
    }

    /**
     * @return the channelTypeUID
     */
    public String getChannelId() {
        return channelType.getUID().getId();
    }

    /**
     * @return the channelTypeUID
     */
    public String getAcceptedItemType() {
        return channelType.getItemType();
    }

}
