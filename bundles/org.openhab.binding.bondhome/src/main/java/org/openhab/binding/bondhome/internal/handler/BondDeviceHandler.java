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
package org.openhab.binding.bondhome.internal.handler;

import static org.openhab.binding.bondhome.internal.BondHomeBindingConstants.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.bondhome.internal.api.BondDevice;
import org.openhab.binding.bondhome.internal.api.BondDeviceAction;
import org.openhab.binding.bondhome.internal.api.BondDeviceProperties;
import org.openhab.binding.bondhome.internal.api.BondDeviceState;
import org.openhab.binding.bondhome.internal.api.BondHttpApi;
import org.openhab.binding.bondhome.internal.config.BondDeviceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link BondDeviceHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class BondDeviceHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(BondDeviceHandler.class);

    private @NonNullByDefault({}) BondDeviceConfiguration config;
    private @Nullable BondHttpApi api;

    private @Nullable BondDevice deviceInfo;
    private @Nullable BondDeviceProperties deviceProperties;
    private @Nullable BondDeviceState deviceState;

    /**
     * The supported thing types.
     */
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Stream
            .of(THING_TYPE_BOND_FAN, THING_TYPE_BOND_SHADES, THING_TYPE_BOND_FIREPLACE, THING_TYPE_BOND_GENERIC)
            .collect(Collectors.toSet());

    public BondDeviceHandler(Thing thing) {
        super(thing);
        logger.trace("Created handler for bond device.");
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        BondHttpApi api = this.api;
        if (api == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Bridge API not available");
            return;
        } else {

            if (command instanceof RefreshType) {
                try {
                    deviceState = api.getDeviceState(config.deviceId);
                    updateChannelsFromState(deviceState);
                } catch (IOException e) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                }
                return;
            }

            switch (channelUID.getId()) {
                case CHANNEL_POWER_STATE:
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.TurnOn : BondDeviceAction.TurnOff, null);
                    break;
                case CHANNEL_FAN_SPEED:
                case CHANNEL_FAN_BREEZE_STATE:
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.BreezeOn : BondDeviceAction.BreezeOff, null);
                    break;
                case CHANNEL_FAN_BREEZE_MEAN:
                case CHANNEL_FAN_BREEZE_VAR:
                case CHANNEL_FAN_DIRECTION:
                    api.executeDeviceAction(config.deviceId, BondDeviceAction.SetDirection,
                            command == OnOffType.ON ? 1 : -1);
                    break;
                case CHANNEL_FAN_LIGHT_STATE:
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.TurnLightOn : BondDeviceAction.TurnLightOff,
                            null);
                    break;
                case CHANNEL_LIGHT_BRIGHTNESS:
                case CHANNEL_FAN_UP_LIGHT_STATE:
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.TurnUpLightOn : BondDeviceAction.TurnUpLightOff,
                            null);
                    break;
                case CHANNEL_UP_LIGHT_BRIGHTNESS:
                case CHANNEL_FAN_DOWN_LIGHT_STATE:
                    api.executeDeviceAction(config.deviceId, command == OnOffType.ON ? BondDeviceAction.TurnDownLightOn
                            : BondDeviceAction.TurnDownLightOff, null);
                    break;
                case CHANNEL_DOWN_LIGHT_BRIGHTNESS:
                case CHANNEL_FLAME:
                case CHANNEL_FP_FAN_STATE:
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.TurnFpFanOn : BondDeviceAction.TurnFpFanOff,
                            null);
                    break;
                case CHANNEL_FP_FAN_SPEED:
                case CHANNEL_OPEN_CLOSE:
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.Open : BondDeviceAction.Close, null);
                    break;
                default:
                    return;
            }
        }
    }

    @Override
    public void initialize() {
        logger.debug("Starting initialization for Bond device!");
        config = getConfigAs(BondDeviceConfiguration.class);

        // set the thing status to UNKNOWN temporarily
        updateStatus(ThingStatus.UNKNOWN);

        // Example for background initialization:
        scheduler.execute(() -> {
            Bridge myBridge = this.getBridge();
            if (myBridge == null) {

                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "No Bond bridge is associated with this Bond device");
                logger.error("No Bond bridge is associated with this Bond device - cannot create device!");

                return;
            } else {
                BondBridgeHandler myBridgeHandler = (BondBridgeHandler) myBridge.getHandler();
                if (myBridgeHandler != null) {
                    this.api = myBridgeHandler.getBridgeAPI();
                    initializeThing();
                    logger.debug("Finished initializing!");
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                            "Cannot access API for Bridge associated with this Bond device");
                    logger.error("Cannot access API for Bridge associated with this Bond device!");
                }
            }
        });
    }

    private void initializeThing() {
        BondHttpApi api = this.api;
        if (api != null) {
            try {
                logger.trace("Getting device information for {}", config.deviceId);
                deviceInfo = api.getDevice(config.deviceId);
                logger.trace("Getting device properties for {}", config.deviceId);
                deviceProperties = api.getDeviceProperties(config.deviceId);
                logger.trace("Getting device state for {}", config.deviceId);
                deviceState = api.getDeviceState(config.deviceId);
            } catch (IOException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
            }
        }

        BondDevice devInfo = this.deviceInfo;
        BondDeviceProperties devProperties = this.deviceProperties;
        BondDeviceState devState = this.deviceState;

        // Update all the thing properties based on the result
        Map<String, String> thingProperties = new HashMap<String, String>();
        if (devInfo != null) {
            logger.trace("Updating device name to {}", devInfo.name);
            thingProperties.put(PROPERTIES_DEVICE_NAME, devInfo.name);
        }
        if (devProperties != null) {
            logger.trace("Updating other device properties for {}", config.deviceId);
            thingProperties.put(PROPERTIES_MAX_SPEED, String.valueOf(devProperties.max_speed));
            thingProperties.put(PROPERTIES_TRUST_STATE, String.valueOf(devProperties.trust_state));
            thingProperties.put(PROPERTIES_ADDRESS, String.valueOf(devProperties.addr));
            thingProperties.put(PROPERTIES_RF_FREQUENCY, String.valueOf(devProperties.freq));
        }
        logger.trace("Saving properties for {}", config.deviceId);
        updateProperties(thingProperties);

        // Create channels based on the available actions
        if (devInfo != null) {
            logger.trace("Creating channels based on available actions for {}", config.deviceId);
            createChannelsFromActions(devInfo.actions);
        }

        // Update all channels with current states
        if (devState != null) {
            logger.trace("Updateing channels with current states for {}", config.deviceId);
            updateChannelsFromState(devState);
        }

        // Now we're online!
        updateStatus(ThingStatus.ONLINE);
    }

    private void createChannelsFromActions(BondDeviceAction[] actions) {
        // TODO
    }

    public String getDeviceId() {
        return config.deviceId;
    }

    public void updateChannelsFromState(@Nullable BondDeviceState updateState) {
        // TODO
    }

}
