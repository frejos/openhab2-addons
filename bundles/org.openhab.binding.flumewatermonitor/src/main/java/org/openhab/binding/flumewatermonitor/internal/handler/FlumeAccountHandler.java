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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.flumewatermonitor.internal.api.AuthorizationException;
import org.openhab.binding.flumewatermonitor.internal.api.FlumeAsyncHttpApi;
import org.openhab.binding.flumewatermonitor.internal.api.FlumeJWTAuthorizer;
import org.openhab.binding.flumewatermonitor.internal.config.FlumeAccountConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link FlumeAccountHandler} is responsible for handling commands, which
 * are sent to one of the channels.
 *
 * Some code for JWT handling taken from NikoHomeControlBridgeHandler2 by Mark Herwege
 *
 * @author Sara Geleskie Damiano - Initial contribution *
 */
@NonNullByDefault
public class FlumeAccountHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(FlumeAccountHandler.class);

    private @NonNullByDefault({}) FlumeAccountConfiguration config;

    private final FlumeJWTAuthorizer authorizer;
    private final FlumeAsyncHttpApi asyncApi;

    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").create();

    public FlumeAccountHandler(Bridge bridge) {
        super(bridge);
        authorizer = new FlumeJWTAuthorizer(this);
        asyncApi = new FlumeAsyncHttpApi(this);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Not needed, no commands are supported
    }

    @Override
    public void initialize() {
        config = getConfigAs(FlumeAccountConfiguration.class);

        logger.trace("Initializing handler for Flume account");

        // set the thing status to UNKNOWN temporarily and let the background task decide for the real status.
        // the framework is then able to reuse the resources from the thing handler initialization.
        // we set this upfront to reliably check status updates in unit tests.
        updateStatus(ThingStatus.UNKNOWN);

        // Example for background initialization:
        scheduler.execute(() -> {
            try {
                authorizer.isAuthorized().get();
                updateStatus(ThingStatus.ONLINE);
            } catch (CancellationException e) {
                logger.warn("Authorization attempt was canceled unexpectedly!");
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            } catch (InterruptedException e) {
                logger.warn("Authorization attempt was interrupted before completion!");
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            } catch (ExecutionException e) {
                if (e.getCause() instanceof AuthorizationException) {
                    applyAuthorizationError(e.getMessage());
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getCause().getMessage());
                }
            }
        });
    }

    /**
     * Gets the account configuration.
     *
     * @return The {@link FlumeAccountConfiguration} for the Flume account.
     */
    public FlumeAccountConfiguration getAccountConfiguration() {
        config = getConfigAs(FlumeAccountConfiguration.class);
        return config;
    }

    /**
     * Get the gson to parse JSON
     *
     * @return A Gson object
     */
    public Gson getGson() {
        return gson;
    }

    public FlumeJWTAuthorizer getAuthorizer() {
        return this.authorizer;
    }

    public FlumeAsyncHttpApi getApi() {
        return this.asyncApi;
    }

    public void applyAuthorizationError(String message) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, message);
    }
}
