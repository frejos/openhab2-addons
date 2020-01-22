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

import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelKind;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;

/**
 * This enum represents the possible device actions
 *
 * @author Sara Geleskie Damiano - Initial contribution
 *
 */
public enum BondDeviceAction {

    // State Variables
    // power: (integer) 1 = on, 0 = off
    // Actions
    TurnOn("TurnOn", CHANNEL_GROUP_BASIC, CHANNEL_POWER_STATE, "Switch"),
    // ^^ Turn device power on.
    TurnOff("TurnOff", CHANNEL_GROUP_BASIC, CHANNEL_POWER_STATE, "Switch"),
    // ^^ Turn device power off.
    TogglePower("TogglePower", CHANNEL_GROUP_BASIC, CHANNEL_POWER_STATE, "Switch"),
    // ^^ Change device power from on to off, or off to on.

    // State Variables
    // timer: (integer) seconds remaining on timer, or 0 meaning no timer running
    // Actions
    SetTimer("SetTimer", CHANNEL_GROUP_BASIC, CHANNEL_TIMER, "Number"),
    // ^^ Start timer for s seconds. If power if off, device is implicitly turned
    // on. If argument is zero, the timer is
    // canceled without turning off the device.

    // Properties
    // max_speed: (integer) highest speed available
    // State Variables
    // speed: (integer) value from 1 to max_speed. If power=0, speed represents the
    // last speed setting and the speed to
    // which the device resumes when user asks to turn on.
    // Actions
    SetSpeed("SetSpeed", CHANNEL_GROUP_FAN, CHANNEL_FAN_SPEED, "Number"),
    // ^^ Set speed and turn on. If speed>max_speed, max_speed is assumed. If the
    // fan is off, implicitly turn on the
    // power. Setting speed to zero or a negative value is ignored.
    IncreaseSpeed("IncreaseSpeed", CHANNEL_GROUP_FAN, CHANNEL_FAN_SPEED, "Number"),
    // ^^ Increase speed of fan by specified number of speeds. If the fan is off,
    // implicitly turn on the power.
    DecreaseSpeed("DecreaseSpeed", CHANNEL_GROUP_FAN, CHANNEL_FAN_SPEED, "Number"),
    // ^^ Decrease fan speed by specified number of speeds. If attempting to
    // decrease fan speed below 1, the fan will
    // remain at speed 1. That is, power will not be implicitly turned off. If the
    // power is already off, DecreaseSpeed
    // is ignored.

    // State Variables
    // breeze: (array) array of the form [ <mode>, <mean>, <var> ]:
    // mode: (integer) 0 = breeze mode disabled, 1 = breeze mode enabled
    // mean: (integer) sets the average speed. 0 = minimum average speed (calm), 100
    // = maximum average speed (storm)
    // var: (integer) sets the variability of the speed. 0 = minimum variation
    // (steady), 100 = maximum variation (gusty)
    // Actions
    BreezeOn("BreezeOn", CHANNEL_GROUP_FAN, CHANNEL_FAN_BREEZE_STATE, "Switch"),
    // ^^ Enable breeze with remembered parameters. Defaults to [50,50].
    BreezeOff("BreezeOff", CHANNEL_GROUP_FAN, CHANNEL_FAN_BREEZE_STATE, "Switch"),
    // ^^ Stop breeze. Fan remains on at current speed.
    SetBreeze("SetBreeze", CHANNEL_GROUP_FAN, CHANNEL_FAN_BREEZE_MEAN, "Dimmer"),
    // ^^ Enable breeze with specified parameters (same as breeze state variable).
    // Example SetBreeze([1, 20, 90]).

    // State Variables
    // direction: (integer) 1 = forward, -1 = reverse.
    // The forward and reverse modes are sometimes called Summer and Winter,
    // respectively.
    // Actions
    SetDirection("SetDirection", CHANNEL_GROUP_FAN, CHANNEL_FAN_DIRECTION, "String"),
    // ^^ Control forward and reverse.
    ToggleDirection("ToggleDirection", CHANNEL_GROUP_FAN, CHANNEL_FAN_DIRECTION, "Switch"),
    // ^^ Reverse the direction of the fan.

    // State Variables
    // light: (integer) 1 = light on, 0 = light off
    // Actions
    TurnLightOn("TurnLightOn", CHANNEL_GROUP_LIGHT, CHANNEL_LIGHT_STATE, "Switch"),
    // ^^ Turn light on.
    TurnLightOff("TurnLightOff", CHANNEL_GROUP_LIGHT, CHANNEL_LIGHT_STATE, "Switch"),
    // ^^ Turn off light.
    ToggleLight("ToggleLight", CHANNEL_GROUP_LIGHT, CHANNEL_LIGHT_STATE, "Switch"),
    // ^^ Change light from on to off, or off to on.

    // State Variables
    // up_light: (integer) 1 = up light enabled, 0 = up light disabled
    // down_light: (integer) 1 = down light enabled, 0 = down light disabled
    // If both up_light and light are 1, then the up light will be on, and similar
    // for down light.
    // Note that both up_light and down_light may not be simultaneously zero, so
    // that the device is always ready to
    // respond to a TurnLightOn request.
    // Actions
    TurnUpLightOn("TurnUpLightOn", CHANNEL_GROUP_UP_LIGHT, CHANNEL_UP_LIGHT_STATE, "Switch"),
    // ^^ Turn up light on.
    TurnDownLightOn("TurnDownLightOn", CHANNEL_GROUP_DOWN_LIGHT, CHANNEL_DOWN_LIGHT_STATE, "Switch"),
    // ^^ Turn down light on.
    TurnUpLightOff("TurnUpLightOff", CHANNEL_GROUP_UP_LIGHT, CHANNEL_UP_LIGHT_STATE, "Switch"),
    // ^^ Turn off up light.
    TurnDownLightOff("TurnDownLightOff", CHANNEL_GROUP_DOWN_LIGHT, CHANNEL_DOWN_LIGHT_STATE, "Switch"),
    // ^^ Turn off down light.
    ToggleUpLight("ToggleUpLight", CHANNEL_GROUP_UP_LIGHT, CHANNEL_UP_LIGHT_STATE, "Switch"),
    // ^^ Change up light from on to off, or off to on.
    ToggleDownLight("ToggleDownLight", CHANNEL_GROUP_DOWN_LIGHT, CHANNEL_DOWN_LIGHT_STATE, "Switch"),
    // ^^ Change down light from on to off, or off to on.

    // State Variables
    // brightness: (integer) percentage value of brightness, 1-100. If light=0,
    // brightness represents the last
    // brightness setting and the brightness to resume when user turns on light. If
    // fan has no dimmer or a non-stateful
    // dimmer, brightness is always 100.
    // Actions
    SetBrightness("SetBrightness", CHANNEL_GROUP_LIGHT, CHANNEL_LIGHT_BRIGHTNESS, "Dimmer"),
    // ^^ Set the brightness of the light to specified percentage. Value of 0 is
    // ignored, use TurnLightOff instead.
    IncreaseBrightness("IncreaseBrightness", CHANNEL_GROUP_LIGHT, CHANNEL_LIGHT_BRIGHTNESS, "Dimmer"),
    // ^^ Increase brightness of light by specified percentage. If light is off, it
    // will be turned on at (0 + amount).
    DecreaseBrightness("DecreaseBrightness", CHANNEL_GROUP_LIGHT, CHANNEL_LIGHT_BRIGHTNESS, "Dimmer"),
    // ^^ Decrease light brightness by specified percentage. If attempting to
    // decrease brightness below 1%, light will
    // remain at 1%. Use TurnLightOff to turn off the light. If the light is off,
    // the light will remain off but the
    // remembered brightness will be decreased.

    // State Variables
    // upLightBrightness: (integer) percentage value of up light brightness,
    // 1-100.
    // downLightBrightness: (integer) percentage value of down light brightness,
    // 1-100.
    // Actions
    SetUpLightBrightness("SetUpLightBrightness", CHANNEL_GROUP_UP_LIGHT, CHANNEL_UP_LIGHT_BRIGHTNESS, "Dimmer"),
    // ^^ Similar to SetBrightness but only for the up light.
    SetDownLightBrightness("SetDownLightBrightness", CHANNEL_GROUP_DOWN_LIGHT, CHANNEL_DOWN_LIGHT_BRIGHTNESS, "Dimmer"),
    // ^^ Similar to SetBrightness but only for the down light.
    IncreaseUpLightBrightness("IncreaseUpLightBrightness", CHANNEL_GROUP_UP_LIGHT, CHANNEL_UP_LIGHT_BRIGHTNESS,
            "Dimmer"),
    // ^^ Similar to IncreaseBrightness but only for the up light.
    IncreaseDownLightBrightness("IncreaseDownLightBrightness", CHANNEL_GROUP_DOWN_LIGHT, CHANNEL_DOWN_LIGHT_BRIGHTNESS,
            "Dimmer"),
    // ^^ Similar to IncreaseBrightness but only for the down light.
    DecreaseUpLightBrightness("DecreaseUpLightBrightness", CHANNEL_GROUP_UP_LIGHT, CHANNEL_UP_LIGHT_BRIGHTNESS,
            "Dimmer"),
    // ^^ Similar to DecreaseBrightness but only for the up light.
    DecreaseDownLightBrightness("DecreaseDownLightBrightness", CHANNEL_GROUP_DOWN_LIGHT, CHANNEL_DOWN_LIGHT_BRIGHTNESS,
            "Dimmer"),
    // ^^ Similar to DecreaseBrightness but only for the down light.

    // State Variables
    // flame: (integer) value from 1 to 100. If power=0, flame represents the last
    // flame setting and the flame to which
    // the device resumes when user asks to turn on.
    // Actions
    SetFlame("SetFlame", CHANNEL_GROUP_FIREPLACE, CHANNEL_FLAME, "Dimmer"),
    // ^^ Set flame and turn on. If flame>100, 100 is assumed. If the fireplace is
    // off, implicitly turn on the power.
    // Setting flame to zero or a negative value is ignored.
    IncreaseFlame("IncreaseFlame", CHANNEL_GROUP_FIREPLACE, CHANNEL_FLAME, "Dimmer"),
    // ^^ Increase flame level of fireplace by specified number of flames. If the
    // fireplace is off, implicitly turn on
    // the power.
    DecreaseFlame("DecreaseFlame", CHANNEL_GROUP_FIREPLACE, CHANNEL_FLAME, "Dimmer"),
    // ^^ Decrease flame level by specified number of flames. If attempting to
    // decrease fireplace flame below 1, the
    // fireplace will remain at fflame 1. That is, power will not be implicitly
    // turned off. If the power is already off,
    // DecreaseFlame is ignored.

    // State Variables
    // fpfan_power: (integer) 1 = on, 0 = off
    // fpfan_speed: (integer) from 1-100
    // Actions
    TurnFpFanOff("TurnFpFanOff", CHANNEL_GROUP_FIREPLACE, CHANNEL_FP_FAN_STATE, "Switch"),
    // ^^ Turn the fireplace fan off
    TurnFpFanOn("TurnFpFanOn", CHANNEL_GROUP_FIREPLACE, CHANNEL_FP_FAN_STATE, "Switch"),
    // ^^ Turn the fireplace fan on, restoring the previous speed
    SetFpFan("SetFpFan", CHANNEL_GROUP_FIREPLACE, CHANNEL_FP_FAN_SPEED, "Dimmer"),
    // ^^ Sets the speed of the fireplace fan

    // State Variables
    // open: (integer) 1 = open, 0 = closed
    // Actions
    Open("Open", CHANNEL_GROUP_SHADES, CHANNEL_OPEN_CLOSE, "Switch"),
    // ^^ Open the device.
    Close("Close", CHANNEL_GROUP_SHADES, CHANNEL_OPEN_CLOSE, "Switch"),
    // ^^ Close the device.
    ToggleOpen("ToggleOpen", CHANNEL_GROUP_SHADES, CHANNEL_OPEN_CLOSE, "Switch"),
    // ^^ Close the device if it's open, open it if it's closed

    // Other actions
    Stop("Stop", "", "", ""),
    // ^^ This action tells the Bond to stop any in-progress transmission and empty
    // its transmission queue.
    Hold("Hold", "", "", ""),
    // ^^ Can be used when a signal is required to tell a device to stop moving or
    // the like, since Stop is a special
    // "stop transmitting" action
    Pair("Pair", "", "", ""),
    // ^^ Used in devices that need to be paired with a receiver.
    StartDimmer("StartDimmer", CHANNEL_GROUP_LIGHT, CHANNEL_LIGHT_START_STOP, "Switch"),
    // ^^ Start dimming. The Bond should time out its transmission after 30 seconds,
    // or when the Stop action is called.
    StartUpLightDimmer("StartUpLightDimmer", CHANNEL_GROUP_UP_LIGHT, CHANNEL_UP_LIGHT_START_STOP, "Switch"),
    // ^^ Use this and the StartDownLightDimmer instead of StartDimmer if your
    // device has two dimmable lights.
    StartDownLightDimmer("StartDownLightDimmer", CHANNEL_GROUP_DOWN_LIGHT, CHANNEL_DOWN_LIGHT_START_STOP, "Switch"),
    // ^^ The counterpart to StartUpLightDimmer
    StartIncreasingBrightness("StartIncreasingBrightness", CHANNEL_GROUP_LIGHT, CHANNEL_LIGHT_DIRECTIONAL_INC,
            "Switch"),
    StartDecreasingBrightness("StartDecreasingBrightness", CHANNEL_GROUP_LIGHT, CHANNEL_LIGHT_DIRECTIONAL_DECR,
            "Switch"),

    // More actions
    OEMRandom("OEMRandom", "", "", ""),
    OEMTimer("OEMTimer", "", "", ""),
    Unknown("Unknown", "", "", "");

    private String actionId;
    String channelId;
    String channelGroupId;
    String acceptedItemType;
    ChannelTypeUID channelTypeUID;

    private BondDeviceAction(final String actionId, String channelGroupId, String channelId, String acceptedItemType) {
        this.actionId = actionId;
        this.channelGroupId = channelGroupId;
        this.channelId = channelId;
        this.acceptedItemType = acceptedItemType;
        this.channelTypeUID = new ChannelTypeUID(BINDING_ID, channelId + "Type");
    }

    /**
     * Gets the action ID for request action
     *
     * @return the actionId
     */
    public String getActionId() {
        return actionId;
    }

    /**
     * Creates and returns a {@link Channel}
     *
     * @return the {@link Channel}
     */
    public Channel createChannel(ThingUID thingUID) {
        ChannelUID channelUid = new ChannelUID(thingUID, channelGroupId, channelId);
        Channel channel = ChannelBuilder.create(channelUid, acceptedItemType).withKind(ChannelKind.STATE)
                .withType(channelTypeUID).build();
        return channel;
    }

    /**
     * Creates and returns a {@link ChannelUID}
     *
     * @return the {@link ChannelUID}
     */
    public ChannelUID createChannelUid(ThingUID thingUID) {
        ChannelUID channelUid = new ChannelUID(thingUID, channelGroupId, channelId);
        return channelUid;
    }

    /**
     * Gets the channel ID for the first channel associated with the action
     *
     * @return the channel ID as a String
     */
    public String getChannelId() {
        return channelId;
    }

    /**
     * @return the acceptedItemType as a String
     */
    public String getAcceptedItemType() {
        return acceptedItemType;
    }

    /**
     * @return the channelTypeUID as a String
     */
    public ChannelTypeUID getChannelTypeUID() {
        return channelTypeUID;
    }
}
