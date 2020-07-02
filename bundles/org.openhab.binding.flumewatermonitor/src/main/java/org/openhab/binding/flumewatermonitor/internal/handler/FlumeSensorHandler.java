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
package org.openhab.binding.flumewatermonitor.internal.handler;

import static org.openhab.binding.flumewatermonitor.internal.FlumeWaterMonitorBindingConstants.CHANNEL_BATTERY;
import static org.openhab.binding.flumewatermonitor.internal.FlumeWaterMonitorBindingConstants.CHANNEL_USAGE;
import static org.openhab.binding.flumewatermonitor.internal.FlumeWaterMonitorBindingConstants.CHANNEL_WATER_ON;
import static org.openhab.binding.flumewatermonitor.internal.FlumeWaterMonitorBindingConstants.CURRENT_BINDING_VERSION;
import static org.openhab.binding.flumewatermonitor.internal.FlumeWaterMonitorBindingConstants.PROPERTY_BINDING_VERSION;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.flumewatermonitor.internal.api.AuthorizationException;
import org.openhab.binding.flumewatermonitor.internal.api.FlumeAsyncHttpApi;
import org.openhab.binding.flumewatermonitor.internal.api.FlumeDeviceData;
import org.openhab.binding.flumewatermonitor.internal.api.NotFoundException;
import org.openhab.binding.flumewatermonitor.internal.config.FlumeSensorConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link FlumeSensorHandler} is responsible for handling commands, which
 * are sent to one of the channels.
 *
 * Some code for JWT handling taken from NikoHomeControlBridgeHandler2 by Mark
 * Herwege
 *
 * @author Sara Geleskie Damiano - Initial contribution *
 */
@NonNullByDefault
public class FlumeSensorHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(FlumeSensorHandler.class);

    private @NonNullByDefault({}) FlumeSensorConfiguration config;
    private @Nullable FlumeAsyncHttpApi asyncApi;

    private @Nullable ScheduledFuture<?> waterUseJob;
    private @Nullable ScheduledFuture<?> deviceStatusJob;

    private volatile boolean disposed;

    /** Create a new handler for a Flume water use sensor */
    public FlumeSensorHandler(Thing thing) {
        super(thing);
        disposed = true;
        logger.trace("Created handler for Flume sensor.");
    }

    /**
     * This would handle a command sent to one of the flume channels, but all channels are read only and no commands are
     * supported. Refreshes are regularly executed by the handler and don't need to be called manually.
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Command {} on channel {} not supported.", command, channelUID);
    }

    @Override
    public void initialize() {
        logger.debug("Start initializing!");
        config = getConfigAs(FlumeSensorConfiguration.class);

        // Set the thing status to UNKNOWN temporarily and let the background task
        // decide for the real status. The framework is then able to reuse the resources
        // from the thing handler initialization. We set this upfront to reliably check
        // status updates in unit tests.
        updateStatus(ThingStatus.UNKNOWN);
        disposed = false;

        // If the binding was updated, change the thing type back to itself to force
        // channels to be reloaded from XML.
        // This allows new channels to be added or old channels to be modified as the
        // binding is updated without forcing users to go through the tedium of deleting
        // and re-creating all of their things.
        if (wasBindingUpdated()) {
            changeThingType(this.thing.getThingTypeUID(), this.thing.getConfiguration());
            return;
        }

        getApiInstance();

        logger.debug("Starting device status job for every {} minute[s]", config.deviceStatusInterval);
        startdeviceStatusJob();
        logger.debug("Starting water use job for every {} minute[s]", config.waterUseInterval);
        startwaterUseJob();

        // NOTE: Not setting the thing online here, the water use/status jobs will do
        // that.
        logger.debug("Finished initializing!");
    }

    @Override
    public synchronized void dispose() {
        logger.debug("Disposing thing handler for {}.", this.getThing().getUID());
        // Mark handler as disposed as soon as possible to halt updates
        disposed = true;

        final ScheduledFuture<?> currentUseJob = this.waterUseJob;
        if (currentUseJob != null && !currentUseJob.isCancelled()) {
            currentUseJob.cancel(true);
        }
        this.waterUseJob = null;

        final ScheduledFuture<?> currentStatusJob = this.deviceStatusJob;
        if (currentStatusJob != null && !currentStatusJob.isCancelled()) {
            currentStatusJob.cancel(true);
        }
        this.deviceStatusJob = null;
    }

    /**
     * Start a regular job to request the most recent water use from the Flume API
     */
    private synchronized void startwaterUseJob() {
        final ScheduledFuture<?> currentUseJob = this.waterUseJob;
        if (currentUseJob == null || currentUseJob.isCancelled()) {
            Runnable waterRunnable = () -> {
                if (hasConfigurationError() || disposed) {
                    logger.trace("Thing disposed, will not update water use!");
                    return;
                }
                getApiInstance();
                FlumeAsyncHttpApi currentApi = this.asyncApi;
                if (currentApi == null) {
                    return;
                }
                logger.trace("Polling for water use");
                try {
                    DecimalType latestWaterUse = new DecimalType(
                            currentApi.getWaterUse(config.deviceId, config.waterUseInterval).join());
                    OnOffType isWaterOn = latestWaterUse.floatValue() > 0 ? OnOffType.ON : OnOffType.OFF;
                    setBridgeOnline();
                    updateStatus(ThingStatus.ONLINE);
                    updateState(CHANNEL_USAGE, latestWaterUse);
                    updateState(CHANNEL_WATER_ON, isWaterOn);
                } catch (CancellationException | CompletionException e) {
                    handleExceptions(e);
                }
            };
            logger.trace("Asking the Flume server for water use every {} minutes.", config.waterUseInterval);
            this.waterUseJob = scheduler.scheduleWithFixedDelay(waterRunnable, 0, config.waterUseInterval,
                    TimeUnit.MINUTES);
        }
    }

    /**
     * Start a regular job to request device status from the Flume API.
     */
    private synchronized void startdeviceStatusJob() {
        final ScheduledFuture<?> currentStatusJob = this.deviceStatusJob;
        if (currentStatusJob == null || currentStatusJob.isCancelled()) {
            Runnable statusRunnable = () -> {
                if (hasConfigurationError() || disposed) {
                    logger.trace("Thing disposed, will not update battery level!");
                    return;
                }
                getApiInstance();
                FlumeAsyncHttpApi currentApi = this.asyncApi;
                if (currentApi == null) {
                    return;
                }
                logger.trace("Polling for current state for {} ({})", config.deviceId, this.getThing().getLabel());
                try {
                    FlumeDeviceData deviceState = currentApi.getDevice(config.deviceId).join();
                    updateStatus(ThingStatus.ONLINE);
                    if (deviceState != null) {
                        String battLevel = deviceState.batteryLevel;
                        if (battLevel == null) {
                            logger.info("No battery information in the device response!");
                            return;
                        } else {
                            updateBattery(battLevel);
                            return;
                        }
                    }
                } catch (CancellationException | CompletionException e) {
                    handleExceptions(e);
                }
            };
            logger.trace("Asking the Flume server for battery level every {} minutes.", config.deviceStatusInterval);
            this.deviceStatusJob = scheduler.scheduleWithFixedDelay(statusRunnable, 0, config.deviceStatusInterval,
                    TimeUnit.MINUTES);
        }
    }

    /**
     * Translate a text battery level to the percent type needed for the system battery channel.
     * 
     * @param batteryLevel the text battery level
     */
    private void updateBattery(String batteryLevel) {
        switch (batteryLevel) {
            case "LOW":
                updateState(CHANNEL_BATTERY, new PercentType(25));
                break;
            case "MEDIUM":
                updateState(CHANNEL_BATTERY, new PercentType(50));
                break;
            case "HIGH":
                updateState(CHANNEL_BATTERY, new PercentType(75));
                break;
            default:
                break;
        }
    }

    /**
     * Translate an exception thrown by a method into a current thing status
     * 
     * @param e the exception that was thrown
     */
    private void handleExceptions(Throwable e) {
        if (e instanceof CancellationException) {
            logger.warn("Flume API request attempt was canceled unexpectedly!");
        } else if (e instanceof ExecutionException || e instanceof CompletionException) {
            if (e.getCause() instanceof AuthorizationException) {
                logger.warn("Flume API request attempt resulted in an authorization error!");
                @Nullable
                Bridge myBridge = this.getBridge();
                if (myBridge != null) {
                    FlumeAccountHandler myBridgeHandler = (FlumeAccountHandler) myBridge.getHandler();
                    if (myBridgeHandler != null) {
                        myBridgeHandler.applyAuthorizationError(e.getMessage());
                    }
                }
            } else if (e.getCause() instanceof NotFoundException) {
                logger.warn("Flume API request attempt failed because the resource does not exist!");
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getCause().getMessage());
            } else {
                logger.warn("{}", e.getCause().getMessage());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getCause().getMessage());
            }
        }
    }

    /**
     * Checks that the Flume sensor thing is properly tied to an account and then gets the API instance associated with
     * that account.
     */
    private boolean getApiInstance() {
        Bridge myAccount = this.getBridge();
        if (myAccount == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Flume account missing!");
            return false;
        }

        FlumeAccountHandler myAccountHandler = (FlumeAccountHandler) myAccount.getHandler();
        if (myAccountHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Flume account missing!");
            return false;
        }

        this.asyncApi = myAccountHandler.getAsyncApi();
        return true;
    }

    /**
     * Notifies the account that a successful call was made and thus the bridge should be set online.
     */
    private void setBridgeOnline() {
        @Nullable
        Bridge myBridge = this.getBridge();
        if (myBridge != null) {
            FlumeAccountHandler myBridgeHandler = (FlumeAccountHandler) myBridge.getHandler();
            if (myBridgeHandler != null) {
                myBridgeHandler.setAccountOnline();
            }
        }
    }

    /**
     * Checks that the thing is properly configured an online. Used to prevent thing from updating a channel
     * inappropriately.
     *
     * @return True if the thing is properly configured
     */
    private boolean hasConfigurationError() {
        ThingStatusInfo statusInfo = getThing().getStatusInfo();
        return statusInfo.getStatus() == ThingStatus.OFFLINE
                && statusInfo.getStatusDetail() == ThingStatusDetail.CONFIGURATION_ERROR;
    }

    /**
     * Check if the binding version property of the thing matches that of the binding itself. Used to force the thing to
     * recreate itself and all of its channels when the binding is updated. This allows channels to be updated and new
     * channels to appear for users without forcing them to delete and recreate the things each time the binding is
     * updated.
     *
     * @return True if the binding version of the thing is different from that of the binding.
     */
    private synchronized boolean wasBindingUpdated() {
        // Check if the binding has been updated
        boolean updatedBinding = true;
        @Nullable
        String lastBindingVersion = this.getThing().getProperties().get(PROPERTY_BINDING_VERSION);
        updatedBinding = !CURRENT_BINDING_VERSION.equals(lastBindingVersion);
        if (updatedBinding) {
            logger.info("Flume binding has been updated.");
            logger.info("Current version is {}, prior version was {}.", CURRENT_BINDING_VERSION, lastBindingVersion);

            // Update the thing with the new property value
            final Map<String, String> newProperties = new HashMap<>(thing.getProperties());
            newProperties.put(PROPERTY_BINDING_VERSION, CURRENT_BINDING_VERSION);

            final ThingBuilder thingBuilder = editThing();
            thingBuilder.withProperties(newProperties);
            updateThing(thingBuilder.build());
        }
        return updatedBinding;
    }
}
