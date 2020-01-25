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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
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
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelDefinitionBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateDescriptionFragment;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentBuilder;
import org.openhab.binding.bondhome.internal.BondChannelGroupType;
import org.openhab.binding.bondhome.internal.BondChannelType;
import org.openhab.binding.bondhome.internal.BondHomeStateDescriptionProvider;
import org.openhab.binding.bondhome.internal.BondHomeTypeProvider;
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
    protected final BondHomeStateDescriptionProvider stateDescProvider;
    protected final BondHomeTypeProvider typeProvider;

    /**
     * The supported thing types.
     */
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Stream
            .of(THING_TYPE_BOND_FAN, THING_TYPE_BOND_SHADES, THING_TYPE_BOND_FIREPLACE, THING_TYPE_BOND_GENERIC)
            .collect(Collectors.toSet());

    public BondDeviceHandler(Thing thing, BondHomeTypeProvider typeProvider,
            BondHomeStateDescriptionProvider stateDescProvider) {
        super(thing);
        this.typeProvider = typeProvider;
        this.stateDescProvider = stateDescProvider;
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
                case "power":
                    logger.trace("Power state command");
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.TurnOn : BondDeviceAction.TurnOff, null);
                    break;
                case "fanSpeed":
                    logger.trace("Fan speed command");
                    if (command instanceof DecimalType) {
                        DecimalType decCommand = (DecimalType) command;
                        int value = decCommand.intValue();
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
                case "breezeState":
                    logger.trace("Fan enable/disable breeze command");
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.BreezeOn : BondDeviceAction.BreezeOff, null);
                    break;
                case "breezeMean":
                    // TODO write array command fxn
                    logger.trace("Support for fan breeze settings not yet available");
                    break;
                case "breezeVariability":
                    // TODO write array command fxn
                    logger.trace("Support for fan breeze settings not yet available");
                    break;
                case "direction":
                    logger.trace("Fan direction command");
                    if (command instanceof StringType) {
                        api.executeDeviceAction(config.deviceId, BondDeviceAction.SetDirection,
                                command.toString() == "winter" ? -1 : 1);
                    }
                    break;
                case "light":
                    logger.trace("Fan light state command");
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.TurnLightOn : BondDeviceAction.TurnLightOff,
                            null);
                    break;
                case "brightness":
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
                    } else {
                        logger.info("Unsupported command on fan light brightness channel");
                    }
                    break;
                case "dimmerStartStop":
                    logger.trace("Fan light dimmer start/stop command");
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.StartDimmer : BondDeviceAction.Stop, null);
                    break;
                case "dimmerIncr":
                    logger.trace("Fan light brightness increase start/stop command");
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.StartIncreasingBrightness
                                    : BondDeviceAction.Stop,
                            null);
                    break;
                case "dimmerDcr":
                    logger.trace("Fan light brightness decrease start/stop command");
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.StartDecreasingBrightness
                                    : BondDeviceAction.Stop,
                            null);
                    break;
                case "upLight":
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.TurnUpLightOn : BondDeviceAction.TurnUpLightOff,
                            null);
                    break;
                case "upLightBrightness":
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
                case "upLightDimmerStartStop":
                    logger.trace("Fan up light dimmer change command");
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.StartDimmer : BondDeviceAction.Stop, null);
                    break;
                case "upLightdimmerIncr":
                case "upLightdimmerDcr":
                    // TODO: Command format not documented by Bond for up light directional brightness
                    logger.info("Bi-direction brightness control for up-lights not yet enabled!");
                    break;
                case "downLight":
                    api.executeDeviceAction(config.deviceId, command == OnOffType.ON ? BondDeviceAction.TurnDownLightOn
                            : BondDeviceAction.TurnDownLightOff, null);
                    break;
                case "downLightBrightness":
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
                case "downLightDimmerStartStop":
                    logger.trace("Fan down light dimmer change command");
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.StartDimmer : BondDeviceAction.Stop, null);
                    break;
                case "downLightdimmerIncr":
                case "downLightdimmerDcr":
                    // TODO: Command format not documented by Bond for down light directional brightness
                    // brightness
                    logger.info("Bi-direction brightness control for up-lights not yet enabled!");
                    break;
                case "flame":
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
                case "fpFanPower":
                    api.executeDeviceAction(config.deviceId,
                            command == OnOffType.ON ? BondDeviceAction.TurnFpFanOn : BondDeviceAction.TurnFpFanOff,
                            null);
                    break;
                case "fpFanSpeed":
                    if (command instanceof PercentType) {
                        PercentType pctCommand = (PercentType) command;
                        int value = pctCommand.intValue();
                        logger.trace("Fireplace fan command with value of {}", value);
                        api.executeDeviceAction(config.deviceId, BondDeviceAction.SetFpFan, value);
                    } else {
                        logger.info("Unsupported command on fireplace fan channel");
                    }
                    break;
                case "openShade":
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

    @Override
    public void dispose() {
        stateDescProvider.removeDescriptionsForThing(getThing().getUID());
        typeProvider.removeChannelTypesForThing(getThing().getUID());
        typeProvider.removeChannelGroupTypesForThing(getThing().getUID());
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
            logger.trace("Updating channels with current states for {}", config.deviceId);
            updateChannelsFromState(devState);
        }

        // Now we're online!
        updateStatus(ThingStatus.ONLINE);
    }

    private void createChannelsFromActions(BondDeviceAction[] actions) {
        logger.trace("Creating channels based on the available actions");
        // Get the thing to edit
        ThingBuilder thingBuilder = editThing();

        // list of all the channels
        List<Channel> channels = new ArrayList<>();
        List<String> channelIds = new ArrayList<>();

        // lists of channel definitions by group
        List<ChannelDefinition> basicChannels = new ArrayList<>();
        List<ChannelDefinition> fanChannels = new ArrayList<>();
        List<ChannelDefinition> lightChannels = new ArrayList<>();
        List<ChannelDefinition> upLightChannels = new ArrayList<>();
        List<ChannelDefinition> downLightChannels = new ArrayList<>();
        List<ChannelDefinition> fireplaceChannels = new ArrayList<>();
        List<ChannelDefinition> shadeChannels = new ArrayList<>();

        for (BondDeviceAction action : actions) {
            if (action != null) {
                logger.trace("action: {}", action.getActionId());

                BondChannelType bondChannelType = action.getBondChannelType();
                if (bondChannelType == null) {
                    logger.trace("No channel is associated with this action, ignoring");
                } else if (channelIds.contains(bondChannelType.getChannelId())) {
                    logger.trace("Channel type already added to list for this action, ignoring");
                } else {

                    ChannelType channelType = bondChannelType.getChannelType();
                    ChannelTypeUID channelTypeUID = channelType.getUID();
                    ChannelUID channelUID = new ChannelUID(this.getThing().getUID(), action.getGroupTypeId(),
                            bondChannelType.getChannelId());

                    // Special set up for the fan speed channel
                    if (bondChannelType.equals(BondChannelType.fanSpeed)) {
                        StateDescriptionFragment stateFragment = StateDescriptionFragmentBuilder.create()
                                .withMinimum(new BigDecimal(1)).withMaximum(new BigDecimal(1))
                                .withStep(new BigDecimal(1)).withPattern("%d").withReadOnly(false).build();
                        BondDeviceProperties devProperties = this.deviceProperties;
                        if (devProperties != null) {
                            stateFragment = StateDescriptionFragmentBuilder.create().withMinimum(new BigDecimal(1))
                                    .withMaximum(new BigDecimal(devProperties.max_speed)).withStep(new BigDecimal(1))
                                    .withPattern("%d").withReadOnly(false).build();
                        }
                        StateDescription state = stateFragment.toStateDescription();
                        stateDescProvider.setDescription(channelUID, state);
                        logger.trace("State description for fan speed: {}", state.toString());
                    }

                    // Special set up for the fan breeze channel
                    // We need to manually add the extra channel for variability
                    if (bondChannelType.getChannelId().equals("breezeMean")) {
                        channelIds.add("breezeMean");

                        ChannelType breezVarChannelType = BondChannelType.breezeVariability.getChannelType();
                        ChannelTypeUID breezVarChannelTypeUID = breezVarChannelType.getUID();
                        ChannelUID breezVarChannelUID = new ChannelUID(this.getThing().getUID(),
                                action.getGroupTypeId(), BondChannelType.breezeVariability.getChannelId());
                        Channel breezVarChannel = ChannelBuilder
                                .create(breezVarChannelUID, BondChannelType.breezeVariability.getAcceptedItemType())
                                .withType(breezVarChannelTypeUID).build();
                        channels.add(breezVarChannel);
                        ChannelDefinition breezVarChannelDef = new ChannelDefinitionBuilder(
                                breezVarChannelType.getLabel(), channelTypeUID).build();
                        fanChannels.add(breezVarChannelDef);
                        typeProvider.setChannelType(breezVarChannelTypeUID, breezVarChannelType);
                    }

                    // Get the channel associated with the action
                    Channel channel = ChannelBuilder.create(channelUID, bondChannelType.getAcceptedItemType())
                            .withType(channelTypeUID).build();
                    // Add the channel type to the channel type provider
                    typeProvider.setChannelType(channelTypeUID, channelType);

                    ChannelDefinition channelDef = new ChannelDefinitionBuilder(
                            bondChannelType.getChannelType().getLabel(), channelTypeUID).build();
                    logger.trace("Prepared channel definition: {}", channelDef.toString());

                    channelIds.add(bondChannelType.getChannelId());
                    channels.add(channel);

                    if (action.getChannelGroupId() == BondChannelGroupType.common.getGroupTypeId()) {
                        basicChannels.add(channelDef);
                    } else if (action.getChannelGroupId() == BondChannelGroupType.ceilingFan.getGroupTypeId()) {
                        fanChannels.add(channelDef);
                    } else if (action.getChannelGroupId() == BondChannelGroupType.light.getGroupTypeId()) {
                        lightChannels.add(channelDef);
                    } else if (action.getChannelGroupId() == BondChannelGroupType.upLight.getGroupTypeId()) {
                        upLightChannels.add(channelDef);
                    } else if (action.getChannelGroupId() == BondChannelGroupType.downLight.getGroupTypeId()) {
                        downLightChannels.add(channelDef);
                    } else if (action.getChannelGroupId() == BondChannelGroupType.fireplace.getGroupTypeId()) {
                        fireplaceChannels.add(channelDef);
                    } else if (action.getChannelGroupId() == BondChannelGroupType.shade.getGroupTypeId()) {
                        shadeChannels.add(channelDef);
                    }

                    logger.debug(
                            "Based on Action {}, added channel {} ({}) to channel list with Channel UID {} and Channel Type UID {}",
                            action.getActionId(), channel.toString(), channel.getLabel(), channel.getUID(),
                            channel.getChannelTypeUID());
                }
            }
        }

        if (!basicChannels.isEmpty()) {
            BondChannelGroupType channelGroup = BondChannelGroupType.common;
            ChannelGroupTypeUID groudUid = channelGroup.getGroupTypeUID();
            ChannelGroupType grouptype = ChannelGroupTypeBuilder.instance(groudUid, channelGroup.getGroupTypeLabel())
                    .withChannelDefinitions(basicChannels).build();
            typeProvider.setChannelGroupType(groudUid, grouptype);
            logger.trace("Created channel group type for basic channels {}", grouptype.toString());
        }
        if (!fanChannels.isEmpty()) {
            BondChannelGroupType channelGroup = BondChannelGroupType.ceilingFan;
            ChannelGroupTypeUID groudUid = channelGroup.getGroupTypeUID();
            ChannelGroupType grouptype = ChannelGroupTypeBuilder.instance(groudUid, channelGroup.getGroupTypeLabel())
                    .withChannelDefinitions(basicChannels).build();
            typeProvider.setChannelGroupType(groudUid, grouptype);
            logger.trace("Created channel group type for ceiling fan channels {}", grouptype.toString());
        }
        if (!lightChannels.isEmpty()) {
            BondChannelGroupType channelGroup = BondChannelGroupType.light;
            ChannelGroupTypeUID groudUid = channelGroup.getGroupTypeUID();
            ;
            ChannelGroupType grouptype = ChannelGroupTypeBuilder.instance(groudUid, channelGroup.getGroupTypeLabel())
                    .withChannelDefinitions(basicChannels).build();
            typeProvider.setChannelGroupType(groudUid, grouptype);
            logger.trace("Created channel group type for fan light channels {}", grouptype.toString());
        }
        if (!upLightChannels.isEmpty()) {
            BondChannelGroupType channelGroup = BondChannelGroupType.upLight;
            ChannelGroupTypeUID groudUid = channelGroup.getGroupTypeUID();
            ChannelGroupType grouptype = ChannelGroupTypeBuilder.instance(groudUid, channelGroup.getGroupTypeLabel())
                    .withChannelDefinitions(basicChannels).build();
            typeProvider.setChannelGroupType(groudUid, grouptype);
            logger.trace("Created channel group type for fan up light channels {}", grouptype.toString());
        }
        if (!downLightChannels.isEmpty()) {
            BondChannelGroupType channelGroup = BondChannelGroupType.downLight;
            ChannelGroupTypeUID groudUid = channelGroup.getGroupTypeUID();
            ChannelGroupType grouptype = ChannelGroupTypeBuilder.instance(groudUid, channelGroup.getGroupTypeLabel())
                    .withChannelDefinitions(basicChannels).build();
            typeProvider.setChannelGroupType(groudUid, grouptype);
            logger.trace("Created channel group type for fan down light channels {}", grouptype.toString());
        }
        if (!fireplaceChannels.isEmpty()) {
            BondChannelGroupType channelGroup = BondChannelGroupType.fireplace;
            ChannelGroupTypeUID groudUid = channelGroup.getGroupTypeUID();
            ChannelGroupType grouptype = ChannelGroupTypeBuilder.instance(groudUid, channelGroup.getGroupTypeLabel())
                    .withChannelDefinitions(basicChannels).build();
            typeProvider.setChannelGroupType(groudUid, grouptype);
            logger.trace("Created channel group type for fireplace channels {}", grouptype.toString());
        }
        if (!shadeChannels.isEmpty()) {
            BondChannelGroupType channelGroup = BondChannelGroupType.shade;
            ChannelGroupTypeUID groudUid = channelGroup.getGroupTypeUID();
            ChannelGroupType grouptype = ChannelGroupTypeBuilder.instance(groudUid, channelGroup.getGroupTypeLabel())
                    .withChannelDefinitions(basicChannels).build();
            typeProvider.setChannelGroupType(groudUid, grouptype);
            logger.trace("Created channel group type for motorized shade channels {}", grouptype.toString());
        }

        // Add all the channels
        logger.trace("Saving the thing with all the new channels");
        thingBuilder.withChannels(channels);
        updateThing(thingBuilder.build());
    }

    public String getDeviceId() {
        return config.deviceId;
    }

    public void updateChannelsFromState(@Nullable BondDeviceState updateState) {
        if (updateState != null) {
            logger.debug("Updating channels from state");
            updateState("power", updateState.power == 0 ? OnOffType.OFF : OnOffType.ON);
            updateState("timer", new DecimalType(updateState.timer));
            updateState("fanSpeed", new DecimalType(updateState.speed));
            if (updateState.breeze != null) {
                updateState("breezeState", updateState.breeze[0] == 0 ? OnOffType.OFF : OnOffType.ON);
                updateState("breezeMean", new DecimalType(updateState.breeze[1]));
                updateState("breezeVariability", new DecimalType(updateState.breeze[2]));
            }
            updateState("direction", updateState.direction == 0 ? OnOffType.OFF : OnOffType.ON);
            updateState("light", updateState.light == 0 ? OnOffType.OFF : OnOffType.ON);
            updateState("brightness", new DecimalType(updateState.brightness));
            // updateState("dimmerStartStop", updateState.dimmerStartStop);
            // updateState(CHANNEL_LIGHT_DIRECTIONAL, updateState.DimmerIncr);
            updateState("upLight", updateState.up_light == 0 ? OnOffType.OFF : OnOffType.ON);
            updateState("upLightBrightness", new DecimalType(updateState.upLightBrightness));
            // updateState("upLightDimmerStartStop", updateState.upLightDimmerStartStop);
            // updateState("upLightdimmerIncr", updateState.upLightDimmerIncr);
            updateState("downLight", updateState.down_light == 0 ? OnOffType.OFF : OnOffType.ON);
            updateState("downLightBrightness", new DecimalType(updateState.downLightBrightness));
            // updateState("downLightDimmerStartStop", updateState.downLightDimmerStartStop);
            // updateState("downLightdimmerIncr", updateState.downLightDimmerIncr);
            updateState("flame", new DecimalType(updateState.flame));
            updateState("fpFanPower", updateState.fpfan_power == 0 ? OnOffType.OFF : OnOffType.ON);
            updateState("fpFanSpeed", new DecimalType(updateState.fpfan_speed));
            updateState("openShade", updateState.open == 0 ? OpenClosedType.CLOSED : OpenClosedType.OPEN);
        } else {
            logger.debug("No state information provided to update channels with");
        }
    }

}
