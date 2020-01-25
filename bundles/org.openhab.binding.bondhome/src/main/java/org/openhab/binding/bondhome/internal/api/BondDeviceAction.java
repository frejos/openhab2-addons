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

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.bondhome.internal.BondChannelGroupType;
import org.openhab.binding.bondhome.internal.BondChannelType;

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
    TurnOn("TurnOn", BondChannelGroupType.common, BondChannelType.power),
    // ^^ Turn device power on.
    TurnOff("TurnOff", BondChannelGroupType.common, BondChannelType.power),
    // ^^ Turn device power off.
    TogglePower("TogglePower", BondChannelGroupType.common, BondChannelType.power),
    // ^^ Change device power from on to off, or off to on.

    // State Variables
    // timer: (integer) seconds remaining on timer, or 0 meaning no timer running
    // Actions
    SetTimer("SetTimer", BondChannelGroupType.common, BondChannelType.timer),
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
    SetSpeed("SetSpeed", BondChannelGroupType.ceilingFan, BondChannelType.fanSpeed),
    // ^^ Set speed and turn on. If speed>max_speed, max_speed is assumed. If the
    // fan is off, implicitly turn on the
    // power. Setting speed to zero or a negative value is ignored.
    IncreaseSpeed("IncreaseSpeed", BondChannelGroupType.ceilingFan, BondChannelType.fanSpeed),
    // ^^ Increase speed of fan by specified number of speeds. If the fan is off,
    // implicitly turn on the power.
    DecreaseSpeed("DecreaseSpeed", BondChannelGroupType.ceilingFan, BondChannelType.fanSpeed),
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
    BreezeOn("BreezeOn", BondChannelGroupType.ceilingFan, BondChannelType.breezeState),
    // ^^ Enable breeze with remembered parameters. Defaults to [50,50].
    BreezeOff("BreezeOff", BondChannelGroupType.ceilingFan, BondChannelType.breezeState),
    // ^^ Stop breeze. Fan remains on at current speed.
    SetBreeze("SetBreeze", BondChannelGroupType.ceilingFan, BondChannelType.breezeMean),
    // ^^ Enable breeze with specified parameters (same as breeze state variable).
    // Example SetBreeze([1, 20, 90]).

    // State Variables
    // direction: (integer) 1 = forward, -1 = reverse.
    // The forward and reverse modes are sometimes called Summer and Winter,
    // respectively.
    // Actions
    SetDirection("SetDirection", BondChannelGroupType.ceilingFan, BondChannelType.direction),
    // ^^ Control forward and reverse.
    ToggleDirection("ToggleDirection", BondChannelGroupType.ceilingFan, BondChannelType.direction),
    // ^^ Reverse the direction of the fan.

    // State Variables
    // light: (integer) 1 = light on, 0 = light off
    // Actions
    TurnLightOn("TurnLightOn", BondChannelGroupType.light, BondChannelType.light),
    // ^^ Turn light on.
    TurnLightOff("TurnLightOff", BondChannelGroupType.light, BondChannelType.light),
    // ^^ Turn off light.
    ToggleLight("ToggleLight", BondChannelGroupType.light, BondChannelType.light),
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
    TurnUpLightOn("TurnUpLightOn", BondChannelGroupType.upLight, BondChannelType.upLight),
    // ^^ Turn up light on.
    TurnDownLightOn("TurnDownLightOn", BondChannelGroupType.downLight, BondChannelType.downLight),
    // ^^ Turn down light on.
    TurnUpLightOff("TurnUpLightOff", BondChannelGroupType.upLight, BondChannelType.upLight),
    // ^^ Turn off up light.
    TurnDownLightOff("TurnDownLightOff", BondChannelGroupType.downLight, BondChannelType.downLight),
    // ^^ Turn off down light.
    ToggleUpLight("ToggleUpLight", BondChannelGroupType.upLight, BondChannelType.upLight),
    // ^^ Change up light from on to off, or off to on.
    ToggleDownLight("ToggleDownLight", BondChannelGroupType.downLight, BondChannelType.downLight),
    // ^^ Change down light from on to off, or off to on.

    // State Variables
    // brightness: (integer) percentage value of brightness, 1-100. If light=0,
    // brightness represents the last
    // brightness setting and the brightness to resume when user turns on light. If
    // fan has no dimmer or a non-stateful
    // dimmer, brightness is always 100.
    // Actions
    SetBrightness("SetBrightness", BondChannelGroupType.light, BondChannelType.brightness),
    // ^^ Set the brightness of the light to specified percentage. Value of 0 is
    // ignored, use TurnLightOff instead.
    IncreaseBrightness("IncreaseBrightness", BondChannelGroupType.light, BondChannelType.brightness),
    // will be turned on at (0 + amount).
    DecreaseBrightness("DecreaseBrightness", BondChannelGroupType.light, BondChannelType.brightness),
    // ^^ Decrease light brightness by specified percentage. If attempting to
    // decrease brightness below 1%, light will
    // remain at 1%. Use TurnLightOff to turn off the light. If the light is off,
    // the light will remain off but the
    // remembered brightness will be decreased.

    // State Variables
    // up_light_brightness: (integer) percentage value of up light brightness,
    // 1-100.
    // down_light_brightness: (integer) percentage value of down light brightness,
    // 1-100.
    // Actions
    SetUpLightBrightness("SetUpLightBrightness", BondChannelGroupType.upLight, BondChannelType.upLightBrightness),
    // ^^ Similar to SetBrightness but only for the up light.
    SetDownLightBrightness("SetDownLightBrightness", BondChannelGroupType.downLight,
            BondChannelType.downLightBrightness),
    // ^^ Similar to SetBrightness but only for the down light.
    IncreaseUpLightBrightness("IncreaseUpLightBrightness", BondChannelGroupType.upLight,
            BondChannelType.upLightBrightness),
    // ^^ Similar to IncreaseBrightness but only for the up light.
    IncreaseDownLightBrightness("IncreaseDownLightBrightness", BondChannelGroupType.downLight,
            BondChannelType.downLightBrightness),
    // ^^ Similar to IncreaseBrightness but only for the down light.
    DecreaseUpLightBrightness("DecreaseUpLightBrightness", BondChannelGroupType.upLight,
            BondChannelType.upLightBrightness),
    // ^^ Similar to DecreaseBrightness but only for the up light.
    DecreaseDownLightBrightness("DecreaseDownLightBrightness", BondChannelGroupType.downLight,
            BondChannelType.downLightBrightness),
    // ^^ Similar to DecreaseBrightness but only for the down light.

    // State Variables
    // flame: (integer) value from 1 to 100. If power=0, flame represents the last
    // flame setting and the flame to which
    // the device resumes when user asks to turn on.
    // Actions
    SetFlame("SetFlame", BondChannelGroupType.fireplace, BondChannelType.flame),
    // ^^ Set flame and turn on. If flame>100, 100 is assumed. If the fireplace is
    // off, implicitly turn on the power.
    // Setting flame to zero or a negative value is ignored.
    IncreaseFlame("IncreaseFlame", BondChannelGroupType.fireplace, BondChannelType.flame),
    // ^^ Increase flame level of fireplace by specified number of flames. If the
    // fireplace is off, implicitly turn on
    // the power.
    DecreaseFlame("DecreaseFlame", BondChannelGroupType.fireplace, BondChannelType.flame),
    // ^^ Decrease flame level by specified number of flames. If attempting to
    // decrease fireplace flame below 1, the
    // fireplace will remain at flame 1. That is, power will not be implicitly
    // turned off. If the power is already off,
    // DecreaseFlame is ignored.

    // State Variables
    // fpfan_power: (integer) 1 = on, 0 = off
    // fpfan_speed: (integer) from 1-100
    // Actions
    TurnFpFanOff("TurnFpFanOff", BondChannelGroupType.fireplace, BondChannelType.fpFanPower),
    // ^^ Turn the fireplace fan off
    TurnFpFanOn("TurnFpFanOn", BondChannelGroupType.fireplace, BondChannelType.fpFanPower),
    // ^^ Turn the fireplace fan on, restoring the previous speed
    SetFpFan("SetFpFan", BondChannelGroupType.fireplace, BondChannelType.fpFanSpeed),
    // ^^ Sets the speed of the fireplace fan

    // State Variables
    // open: (integer) 1 = open, 0 = closed
    // Actions
    Open("Open", BondChannelGroupType.shade, BondChannelType.openShade),
    // ^^ Open the device.
    Close("Close", BondChannelGroupType.shade, BondChannelType.openShade),
    // ^^ Close the device.
    ToggleOpen("ToggleOpen", BondChannelGroupType.shade, BondChannelType.openShade),
    // ^^ Close the device if it's open, open it if it's closed

    // Other actions
    Stop("Stop", BondChannelGroupType.common, null),
    // ^^ This action tells the Bond to stop any in-progress transmission and empty
    // its transmission queue.
    Hold("Hold", BondChannelGroupType.common, null),
    // ^^ Can be used when a signal is required to tell a device to stop moving or
    // the like, since Stop is a special
    // "stop transmitting" action
    Pair("Pair", BondChannelGroupType.common, null),
    // ^^ Used in devices that need to be paired with a receiver.
    StartDimmer("StartDimmer", BondChannelGroupType.light, BondChannelType.dimmerStartStop),
    // ^^ Start dimming. The Bond should time out its transmission after 30 seconds,
    // or when the Stop action is called.
    StartUpLightDimmer("StartUpLightDimmer", BondChannelGroupType.upLight, BondChannelType.upLightDimmerStartStop),
    // ^^ Use this and the StartDownLightDimmer instead of StartDimmer if your
    // device has two dimmable lights.
    StartDownLightDimmer("StartDownLightDimmer", BondChannelGroupType.downLight,
            BondChannelType.downLightDimmerStartStop),
    // ^^ The counterpart to StartUpLightDimmer
    StartIncreasingBrightness("StartIncreasingBrightness", BondChannelGroupType.light, BondChannelType.dimmerIncr),
    StartDecreasingBrightness("StartDecreasingBrightness", BondChannelGroupType.light, BondChannelType.dimmerDcr),

    // More actions
    OEMRandom("OEMRandom", BondChannelGroupType.common, null),
    OEMTimer("OEMTimer", BondChannelGroupType.common, null),
    Unknown("Unknown", BondChannelGroupType.common, null);

    private String actionId;
    private BondChannelGroupType bondChannelGroupType;
    private @Nullable BondChannelType bondChannelType;

    private BondDeviceAction(final String actionId, BondChannelGroupType bondChannelGroupType,
            @Nullable BondChannelType bondChannelType) {
        this.actionId = actionId;
        this.bondChannelGroupType = bondChannelGroupType;
        this.bondChannelType = bondChannelType;
    }

    /**
     * @return the actionId
     */
    public String getActionId() {
        return actionId;
    }

    /**
     * @return the bondChannelGroup
     */
    public BondChannelGroupType getBondChannelGroupType() {
        return bondChannelGroupType;
    }

    /**
     * @return the bondChannel
     */
    public @Nullable BondChannelType getBondChannelType() {
        return bondChannelType;
    }

    public String getGroupTypeId() {
        return bondChannelGroupType.getGroupTypeId();
    }

    public String getChannelGroupId() {
        return bondChannelGroupType.getGroupTypeId();
    }

}
