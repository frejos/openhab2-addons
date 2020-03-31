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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.bondhome.internal.api.BondDevice;
import org.openhab.binding.bondhome.internal.api.BondDeviceAction;
import org.openhab.binding.bondhome.internal.api.BondDeviceProperties;
import org.openhab.binding.bondhome.internal.api.BondDeviceState;
import org.openhab.binding.bondhome.internal.api.BondDeviceType;
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

    private @Nullable ScheduledFuture<?> pollingJob;

    /**
     * The supported thing types.
     */
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Stream
            .of(THING_TYPE_BOND_FAN, THING_TYPE_BOND_SHADES, THING_TYPE_BOND_FIREPLACE, THING_TYPE_BOND_GENERIC)
            .collect(Collectors.toSet());

    public BondDeviceHandler(Thing thing) {
        super(thing);
        config = getConfigAs(BondDeviceConfiguration.class);
        logger.trace("Created handler for bond device with device id {}.", config.deviceId);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.trace("Bond device handler for {} received command {} on channel {}", config.deviceId, command,
                channelUID);
        BondHttpApi api = this.api;
        if (api == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Bridge API not available");
            return;
        } else {
            if (command instanceof RefreshType) {
                logger.trace("Executing refresh command");
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
                    logger.trace("Power state command");
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.TurnOn : BondDeviceAction.TurnOff, null);
                    break;

                case CHANNEL_STOP:
                    logger.trace("Stop command");
                    api.executeDeviceAction(config.deviceId, BondDeviceAction.Stop, null);
                    // Mark all the changing channels stopped
                    updateState(CHANNEL_LIGHT_START_STOP, OnOffType.OFF);
                    updateState(CHANNEL_LIGHT_DIRECTIONAL_INC, OnOffType.OFF);
                    updateState(CHANNEL_LIGHT_DIRECTIONAL_DECR, OnOffType.OFF);
                    updateState(CHANNEL_UP_LIGHT_START_STOP, OnOffType.OFF);
                    updateState(CHANNEL_UP_LIGHT_DIRECTIONAL_INC, OnOffType.OFF);
                    updateState(CHANNEL_UP_LIGHT_DIRECTIONAL_DECR, OnOffType.OFF);
                    updateState(CHANNEL_DOWN_LIGHT_START_STOP, OnOffType.OFF);
                    updateState(CHANNEL_DOWN_LIGHT_DIRECTIONAL_INC, OnOffType.OFF);
                    updateState(CHANNEL_DOWN_LIGHT_DIRECTIONAL_DECR, OnOffType.OFF);
                    break;

                case CHANNEL_FAN_SPEED:
                    logger.trace("Fan speed command");
                    if (command instanceof PercentType) {
                        int value = 1;
                        BondDeviceProperties devProperties = this.deviceProperties;
                        if (devProperties != null) {
                            int maxSpeed = devProperties.maxSpeed;
                            value = (int) Math.ceil(((PercentType) command).intValue() * maxSpeed / 100);
                        }
                        logger.trace("Fan speed command with speed set as {}", value);
                        api.executeDeviceAction(config.deviceId, BondDeviceAction.SetSpeed, value);
                    } else if (command instanceof IncreaseDecreaseType) {
                        logger.trace("Fan increase/decrease speed command");
                        api.executeDeviceAction(config.deviceId,
                                ((IncreaseDecreaseType) command == IncreaseDecreaseType.INCREASE
                                        ? BondDeviceAction.IncreaseSpeed
                                        : BondDeviceAction.DecreaseSpeed),
                                null);
                    } else {
                        logger.info("Unsupported command on fan speed channel");
                    }
                    break;

                case CHANNEL_FAN_BREEZE_STATE:
                    logger.trace("Fan enable/disable breeze command");
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.BreezeOn : BondDeviceAction.BreezeOff, null);
                    break;

                case CHANNEL_FAN_BREEZE_MEAN:
                    // TODO(SRGDamia1): write array command fxn
                    logger.trace("Support for fan breeze settings not yet available");
                    break;

                case CHANNEL_FAN_BREEZE_VAR:
                    // TODO(SRGDamia1): write array command fxn
                    logger.trace("Support for fan breeze settings not yet available");
                    break;

                case CHANNEL_FAN_DIRECTION:
                    logger.trace("Fan direction command {}", command.toString());
                    if (command instanceof StringType) {
                        api.executeDeviceAction(config.deviceId, BondDeviceAction.SetDirection,
                                command.toString().equals("winter") ? -1 : 1);
                    }
                    break;

                case CHANNEL_LIGHT_STATE:
                    logger.trace("Fan light state command");
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.TurnLightOn : BondDeviceAction.TurnLightOff,
                            null);
                    break;

                case CHANNEL_LIGHT_BRIGHTNESS:
                    if (command instanceof PercentType) {
                        PercentType pctCommand = (PercentType) command;
                        int value = pctCommand.intValue();
                        logger.trace("Fan light brightness command with value of {}", value);
                        api.executeDeviceAction(config.deviceId, BondDeviceAction.SetBrightness, value);
                    } else if (command instanceof IncreaseDecreaseType) {
                        logger.trace("Fan light brightness increase/decrease command");
                        api.executeDeviceAction(config.deviceId,
                                ((IncreaseDecreaseType) command == IncreaseDecreaseType.INCREASE
                                        ? BondDeviceAction.IncreaseBrightness
                                        : BondDeviceAction.DecreaseBrightness),
                                null);
                        updateState(CHANNEL_STOP, OnOffType.ON);
                    } else {
                        logger.info("Unsupported command on fan light brightness channel");
                    }
                    break;

                case CHANNEL_LIGHT_START_STOP:
                    logger.trace("Fan light dimmer start/stop command");
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.StartDimmer : BondDeviceAction.Stop, null);
                    updateState(CHANNEL_STOP, OnOffType.ON);
                    // Unset in 30 seconds when this times out
                    scheduler.schedule(() -> {
                        updateState(CHANNEL_STOP, OnOffType.OFF);
                        updateState(CHANNEL_LIGHT_START_STOP, OnOffType.ON);
                    }, 30, TimeUnit.SECONDS);
                    break;

                case CHANNEL_LIGHT_DIRECTIONAL_INC:
                    logger.trace("Fan light brightness increase start/stop command");
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.StartIncreasingBrightness
                                    : BondDeviceAction.Stop,
                            null);
                    updateState(CHANNEL_STOP, OnOffType.ON);
                    // Unset in 30 seconds when this times out
                    scheduler.schedule(() -> {
                        updateState(CHANNEL_STOP, OnOffType.OFF);
                        updateState(CHANNEL_LIGHT_DIRECTIONAL_INC, OnOffType.ON);
                    }, 30, TimeUnit.SECONDS);
                    break;

                case CHANNEL_LIGHT_DIRECTIONAL_DECR:
                    logger.trace("Fan light brightness decrease start/stop command");
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.StartDecreasingBrightness
                                    : BondDeviceAction.Stop,
                            null);
                    updateState(CHANNEL_STOP, OnOffType.ON);
                    // Unset in 30 seconds when this times out
                    scheduler.schedule(() -> {
                        updateState(CHANNEL_STOP, OnOffType.OFF);
                        updateState(CHANNEL_LIGHT_DIRECTIONAL_DECR, OnOffType.ON);
                    }, 30, TimeUnit.SECONDS);
                    break;

                case CHANNEL_UP_LIGHT_ENABLE:
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.TurnUpLightOn : BondDeviceAction.TurnUpLightOff,
                            null);
                    break;

                case CHANNEL_UP_LIGHT_STATE:
                    // To turn on the up light, we first have to enable it and then turn on the lights
                    if (command == OnOffType.ON) {
                        api.executeDeviceAction(config.deviceId, BondDeviceAction.TurnUpLightOn, null);
                        api.executeDeviceAction(config.deviceId, BondDeviceAction.TurnLightOn, null);
                    } else {
                        api.executeDeviceAction(config.deviceId, BondDeviceAction.TurnUpLightOn, null);
                        api.executeDeviceAction(config.deviceId, BondDeviceAction.TurnLightOff, null);
                    }
                    break;

                case CHANNEL_UP_LIGHT_BRIGHTNESS:
                    if (command instanceof PercentType) {
                        PercentType pctCommand = (PercentType) command;
                        int value = pctCommand.intValue();
                        logger.trace("Fan up light brightness command with value of {}", value);
                        api.executeDeviceAction(config.deviceId, BondDeviceAction.SetUpLightBrightness, value);
                    } else if (command instanceof IncreaseDecreaseType) {
                        logger.trace("Fan uplight brightness increase/decrease command");
                        api.executeDeviceAction(config.deviceId,
                                ((IncreaseDecreaseType) command == IncreaseDecreaseType.INCREASE
                                        ? BondDeviceAction.IncreaseUpLightBrightness
                                        : BondDeviceAction.DecreaseUpLightBrightness),
                                null);
                    } else {
                        logger.info("Unsupported command on fan up light brightness channel");
                    }
                    break;

                case CHANNEL_UP_LIGHT_START_STOP:
                    logger.trace("Fan up light dimmer change command");
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.StartDimmer : BondDeviceAction.Stop, null);
                    updateState(CHANNEL_STOP, OnOffType.ON);
                    // Unset in 30 seconds when this times out
                    scheduler.schedule(() -> {
                        updateState(CHANNEL_STOP, OnOffType.OFF);
                        updateState(CHANNEL_UP_LIGHT_START_STOP, OnOffType.ON);
                    }, 30, TimeUnit.SECONDS);
                    break;

                case CHANNEL_UP_LIGHT_DIRECTIONAL_INC:
                case CHANNEL_UP_LIGHT_DIRECTIONAL_DECR:
                    // TODO(SRGDamia1): Command format not documented by Bond for up light directional brightness
                    logger.info("Bi-direction brightness control for up-lights not yet enabled!");
                    break;

                case CHANNEL_DOWN_LIGHT_ENABLE:
                    api.executeDeviceAction(config.deviceId, command == OnOffType.ON ? BondDeviceAction.TurnDownLightOn
                            : BondDeviceAction.TurnDownLightOff, null);
                    break;

                case CHANNEL_DOWN_LIGHT_STATE:
                    // To turn on the down light, we first have to enable it and then turn on the lights
                    if (command == OnOffType.ON) {
                        api.executeDeviceAction(config.deviceId, BondDeviceAction.TurnDownLightOn, null);
                        api.executeDeviceAction(config.deviceId, BondDeviceAction.TurnLightOn, null);
                    } else {
                        api.executeDeviceAction(config.deviceId, BondDeviceAction.TurnDownLightOn, null);
                        api.executeDeviceAction(config.deviceId, BondDeviceAction.TurnLightOff, null);
                    }
                    break;

                case CHANNEL_DOWN_LIGHT_BRIGHTNESS:
                    if (command instanceof PercentType) {
                        PercentType pctCommand = (PercentType) command;
                        int value = pctCommand.intValue();
                        logger.trace("Fan down light brightness command with value of {}", value);
                        api.executeDeviceAction(config.deviceId, BondDeviceAction.SetDownLightBrightness, value);
                    } else if (command instanceof IncreaseDecreaseType) {
                        logger.trace("Fan down light brightness increase/decrease command");
                        api.executeDeviceAction(config.deviceId,
                                ((IncreaseDecreaseType) command == IncreaseDecreaseType.INCREASE
                                        ? BondDeviceAction.IncreaseDownLightBrightness
                                        : BondDeviceAction.DecreaseDownLightBrightness),
                                null);
                    } else {
                        logger.info("Unsupported command on fan down light brightness channel");
                    }
                    break;

                case CHANNEL_DOWN_LIGHT_START_STOP:
                    logger.trace("Fan down light dimmer change command");
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.StartDimmer : BondDeviceAction.Stop, null);
                    updateState(CHANNEL_STOP, OnOffType.ON);
                    // Unset in 30 seconds when this times out
                    scheduler.schedule(() -> {
                        updateState(CHANNEL_STOP, OnOffType.OFF);
                        updateState(CHANNEL_DOWN_LIGHT_START_STOP, OnOffType.ON);
                    }, 30, TimeUnit.SECONDS);
                    break;

                case CHANNEL_DOWN_LIGHT_DIRECTIONAL_INC:
                case CHANNEL_DOWN_LIGHT_DIRECTIONAL_DECR:
                    // TODO(SRGDamia1): Command format not documented by Bond for down light directional brightness
                    logger.info("Bi-direction brightness control for up-lights not yet enabled!");
                    break;

                case CHANNEL_FLAME:
                    if (command instanceof PercentType) {
                        PercentType pctCommand = (PercentType) command;
                        int value = pctCommand.intValue();
                        logger.trace("Fireplace flame command with value of {}", value);
                        api.executeDeviceAction(config.deviceId, BondDeviceAction.SetFlame, value);
                    } else if (command instanceof IncreaseDecreaseType) {
                        logger.trace("Fireplace flame increase/decrease command");
                        api.executeDeviceAction(config.deviceId,
                                ((IncreaseDecreaseType) command == IncreaseDecreaseType.INCREASE
                                        ? BondDeviceAction.IncreaseFlame
                                        : BondDeviceAction.DecreaseFlame),
                                null);
                    } else {
                        logger.info("Unsupported command on flame channel");
                    }
                    break;

                case CHANNEL_FP_FAN_STATE:
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.TurnFpFanOn : BondDeviceAction.TurnFpFanOff,
                            null);
                    break;

                case CHANNEL_FP_FAN_SPEED:
                    if (command instanceof PercentType) {
                        PercentType pctCommand = (PercentType) command;
                        int value = pctCommand.intValue();
                        logger.trace("Fireplace fan command with value of {}", value);
                        api.executeDeviceAction(config.deviceId, BondDeviceAction.SetFpFan, value);
                    } else {
                        logger.info("Unsupported command on fireplace fan channel");
                    }
                    break;

                case CHANNEL_OPEN_CLOSE:
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.Open : BondDeviceAction.Close, null);
                    break;

                case CHANNEL_HOLD:
                    api.executeDeviceAction(config.deviceId, BondDeviceAction.Hold, null);
                    break;

                default:
                    logger.info("Command {} on unknown channel {}, {}", command.toFullString(), channelUID.getId(),
                            channelUID.toString());
                    return;
            }
        }
    }

    @Override
    public void initialize() {
        logger.debug("Starting initialization for Bond device with device id {}!", config.deviceId);
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

        // Start polling for state
        final ScheduledFuture<?> pollingJob = this.pollingJob;
        if (pollingJob == null || pollingJob.isCancelled()) {
            Runnable pollingCommand = () -> {
                BondHttpApi api = this.api;
                if (api == null) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                            "Bridge API not available");
                    return;
                } else {
                    logger.trace("Polling for current state for {}", this.getThing().getLabel());
                    try {
                        deviceState = api.getDeviceState(config.deviceId);
                        updateChannelsFromState(deviceState);
                    } catch (IOException e) {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                    }
                }
            };
            this.pollingJob = scheduler.scheduleWithFixedDelay(pollingCommand, 60, 300, TimeUnit.SECONDS);
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing thing handler.");

        final ScheduledFuture<?> pollingJob = this.pollingJob;
        if (pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
        }
        this.pollingJob = null;
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
        String lastBindingVersion = this.getThing().getProperties().get(PROPERTIES_BINDING_VERSION);

        // Update all the thing properties based on the result
        Map<String, String> thingProperties = new HashMap<String, String>();
        if (devInfo != null) {
            logger.trace("Updating device name to {}", devInfo.name);
            thingProperties.put(PROPERTIES_DEVICE_NAME, devInfo.name);
        }
        if (devProperties != null) {
            logger.trace("Updating other device properties for {}", config.deviceId);
            thingProperties.put(PROPERTIES_MAX_SPEED, String.valueOf(devProperties.maxSpeed));
            thingProperties.put(PROPERTIES_TRUST_STATE, String.valueOf(devProperties.trustState));
            thingProperties.put(PROPERTIES_ADDRESS, String.valueOf(devProperties.addr));
            thingProperties.put(PROPERTIES_RF_FREQUENCY, String.valueOf(devProperties.freq));
            thingProperties.put(PROPERTIES_BINDING_VERSION, CURRENT_BINDING_VERSION);
        }
        logger.trace("Saving properties for {}", config.deviceId);
        updateProperties(thingProperties);

        // Recreate all possible channels from xml and delete the extras based on the available actions
        if (devInfo != null) {
            // Anytime the configuration has changed or the binding has been updated,
            // recreate the thing to make sure all possible channels are available
            final @Nullable String lastDeviceConfigurationHash = config.lastDeviceConfigurationHash;
            if (!devInfo.hash.equals(lastDeviceConfigurationHash)
                    || !lastBindingVersion.equals(CURRENT_BINDING_VERSION)) {
                recreateChannels(devInfo.type, devInfo.hash, devInfo.actions);
            }
        }

        // Update all channels with current states
        if (devState != null) {
            logger.trace("Updating channels with current states for {}", config.deviceId);
            updateChannelsFromState(devState);
        }

        // Now we're online!
        updateStatus(ThingStatus.ONLINE);
    }

    private void recreateChannels(BondDeviceType currentType, String currentHash, BondDeviceAction[] currentActions) {
        logger.debug(
                "Recreating all possible channels for a {} and deleting extras based on the available actions for {}",
                currentType.getThingTypeUID().getAsString(), config.deviceId);

        // Create a new configuration
        final Map<String, Object> map = new HashMap<>();
        map.put(CONFIG_DEVICE_ID, config.deviceId);
        map.put(CONFIG_LATEST_HASH, currentHash);
        Configuration newConfiguration = new Configuration(map);

        // Update the thing with the new configuration
        // ThingBuilder thingBuilder = editThing();
        // thingBuilder.withConfiguration(newConfiguration);
        // updateThing(thingBuilder.build());

        // Change the thing type back to itself to force all channels to be re-created from XML
        changeThingType(currentType.getThingTypeUID(), newConfiguration);

        // Get the re-created thing to edit
        final ThingBuilder thingBuilder = editThing();

        // Now, look at the whole list of possible channels
        List<BondDeviceAction> availableActions = Arrays.asList(currentActions);
        List<Channel> possibleChannels = this.getThing().getChannels();
        List<String> availableChannelIds = new ArrayList<>();
        // Always have the last update time channel
        availableChannelIds.add(CHANNEL_LAST_UPDATE);

        for (BondDeviceAction action : availableActions) {
            availableChannelIds.add(action.getChannelTypeId());
            logger.trace("Action: {}, Channel Type Id: {}", action.getActionId(), action.getChannelTypeId());
        }

        for (Channel channel : possibleChannels) {
            if (availableChannelIds.contains(channel.getUID().getId())) {
                logger.trace("Keeping Channel: {}", channel.getUID().getId());
            } else {
                thingBuilder.withoutChannel(channel.getUID());
                logger.trace("Dropping Channel: {}", channel.getUID().getId());
            }
        }

        // Add all the channels
        logger.trace("Saving the thing with extra channels removed");
        updateThing(thingBuilder.build());
    }

    public String getDeviceId() {
        return config.deviceId;
    }

    public void updateChannelsFromState(@Nullable BondDeviceState updateState) {
        if (updateState != null) {
            logger.debug("Updating channels from state");

            updateStatus(ThingStatus.ONLINE);
            updateState(CHANNEL_LAST_UPDATE, new DateTimeType());
            logger.trace("Update Time for {}: {}", this.getThing().getLabel(), (new DateTimeType()).toFullString());

            updateState(CHANNEL_POWER_STATE, updateState.power == 0 ? OnOffType.OFF : OnOffType.ON);
            updateState("timer", new DecimalType(updateState.timer));
            int value = 1;
            BondDeviceProperties devProperties = this.deviceProperties;
            if (devProperties != null) {
                double maxSpeed = devProperties.maxSpeed;
                value = (int) (((double) updateState.speed / maxSpeed) * 100);
                logger.trace("Raw fan speed: {}, Percent: {}", updateState.speed, value);
            } else if (updateState.speed != 0 && this.getThing().getThingTypeUID().equals(THING_TYPE_BOND_FAN)) {
                logger.info("Unable to convert fan speed to a percent for {}!", this.getThing().getLabel());
                scheduler.schedule(() -> {
                    initializeThing();
                }, 30, TimeUnit.SECONDS);
            }
            updateState(CHANNEL_FAN_SPEED, new PercentType(value));
            updateState(CHANNEL_FAN_BREEZE_STATE, updateState.breeze[0] == 0 ? OnOffType.OFF : OnOffType.ON);
            updateState(CHANNEL_FAN_BREEZE_MEAN, new DecimalType(updateState.breeze[1]));
            updateState(CHANNEL_FAN_BREEZE_VAR, new DecimalType(updateState.breeze[2]));
            updateState(CHANNEL_FAN_DIRECTION,
                    updateState.direction == 1 ? new StringType("summer") : new StringType("winter"));
            updateState(CHANNEL_TIMER, new DecimalType(updateState.timer));

            updateState(CHANNEL_LIGHT_STATE, updateState.light == 0 ? OnOffType.OFF : OnOffType.ON);
            updateState(CHANNEL_LIGHT_BRIGHTNESS, new DecimalType(updateState.brightness));

            updateState(CHANNEL_UP_LIGHT_ENABLE, updateState.upLight == 0 ? OnOffType.OFF : OnOffType.ON);
            updateState(CHANNEL_UP_LIGHT_STATE,
                    (updateState.upLight == 1 && updateState.light == 1) ? OnOffType.ON : OnOffType.OFF);
            updateState(CHANNEL_UP_LIGHT_BRIGHTNESS, new DecimalType(updateState.upLightBrightness));

            updateState(CHANNEL_DOWN_LIGHT_ENABLE, updateState.downLight == 0 ? OnOffType.OFF : OnOffType.ON);
            updateState(CHANNEL_DOWN_LIGHT_STATE,
                    (updateState.downLight == 1 && updateState.light == 1) ? OnOffType.ON : OnOffType.OFF);
            updateState(CHANNEL_DOWN_LIGHT_BRIGHTNESS, new DecimalType(updateState.downLightBrightness));

            updateState(CHANNEL_FLAME, new DecimalType(updateState.flame));
            updateState(CHANNEL_FP_FAN_STATE, updateState.fpfanPower == 0 ? OnOffType.OFF : OnOffType.ON);
            updateState(CHANNEL_FP_FAN_SPEED, new DecimalType(updateState.fpfanSpeed));

            updateState(CHANNEL_OPEN_CLOSE, updateState.open == 0 ? OpenClosedType.CLOSED : OpenClosedType.OPEN);

            // Mark all the stateless channels stopped
            updateState(CHANNEL_LIGHT_START_STOP, OnOffType.OFF);
            updateState(CHANNEL_LIGHT_DIRECTIONAL_INC, OnOffType.OFF);
            updateState(CHANNEL_LIGHT_DIRECTIONAL_DECR, OnOffType.OFF);
            updateState(CHANNEL_UP_LIGHT_START_STOP, OnOffType.OFF);
            updateState(CHANNEL_UP_LIGHT_DIRECTIONAL_INC, OnOffType.OFF);
            updateState(CHANNEL_UP_LIGHT_DIRECTIONAL_DECR, OnOffType.OFF);
            updateState(CHANNEL_DOWN_LIGHT_START_STOP, OnOffType.OFF);
            updateState(CHANNEL_DOWN_LIGHT_DIRECTIONAL_INC, OnOffType.OFF);
            updateState(CHANNEL_DOWN_LIGHT_DIRECTIONAL_DECR, OnOffType.OFF);
            updateState(CHANNEL_STOP, OnOffType.OFF);
            updateState(CHANNEL_HOLD, OnOffType.OFF);

        } else {
            logger.debug("No state information provided to update channels with");
        }
    }

}
