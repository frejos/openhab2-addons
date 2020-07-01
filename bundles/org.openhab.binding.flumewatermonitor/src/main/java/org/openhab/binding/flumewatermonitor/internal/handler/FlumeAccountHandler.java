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

import static org.openhab.binding.flumewatermonitor.internal.FlumeWaterMonitorBindingConstants.FLUME_API_ENDPOINT;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
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

/**
 * The {@link FlumeAccountHandler} is responsible for handling commands, which
 * are sent to one of the channels.
 *
 * @author Sara Geleskie Damiano - Initial contribution *
 */
@NonNullByDefault
public class FlumeAccountHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(FlumeAccountHandler.class);

    private @NonNullByDefault({}) FlumeAccountConfiguration config;

    private final FlumeJWTAuthorizer authorizer;
    private final FlumeAsyncHttpApi asyncApi;

    private final HttpClient client = new HttpClient(new SslContextFactory());

    /**
     * Create a new account handler.
     *
     * @param bridge The account thing to handle.
     */
    public FlumeAccountHandler(Bridge bridge) {
        super(bridge);
        logger.trace("Creating handler for Flume account");
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

        // To initialize, confirm that we can authorize with the provided credentials.
        scheduler.execute(() -> {
            try {
                authorizer.isAuthorized().join();
                updateStatus(ThingStatus.ONLINE);
            } catch (CancellationException e) {
                logger.warn("Authorization attempt was canceled unexpectedly!");
            } catch (CompletionException e) {
                if (e.getCause() instanceof AuthorizationException) {
                    applyAuthorizationError(e.getMessage());
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getCause().getMessage());
                }
            }
        });
    }

    @Override
    public void dispose() {
        logger.debug("Disposing handler for Flume account {}.", this.getThing().getUID());

        if (client.isRunning()) {
            try {
                client.stop();
            } catch (Exception e) {
                logger.info("Error stopping client: {}", e.getMessage());
            }
        }
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
     * Attempt to create a new HTTP client for communication with the outside world.
     *
     * @return The created client
     * @throws Exception If the client cannot be created for some reason.
     */
    public HttpClient getClient() throws Exception {
        if (!client.isStarted()) {
            try {
                client.setFollowRedirects(false);
                client.start();
            } catch (Exception e) {
                logger.error("Could not start HTTP client for communication with Flume server!");
                throw new IOException("Could not start HTTP client");
            }
        }
        return this.client;
    }

    /**
     * Get the API instance tied to this account handler.
     *
     * @return the api instance.
     */
    public FlumeAsyncHttpApi getAsyncApi() {
        return this.asyncApi;
    }

    /**
     * Allows a flume sensor, api instance, or the authorizor to inform the account handler that there has been an
     * authorization error and the account should be considered to be badly configured and offline.
     */
    public void applyAuthorizationError(String message) {
        logger.debug("Account handler notified of authorization error.  Setting account offline.");
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, message);
    }

    /**
     * Allows a flume sensor, api instance, or the authorizor to inform the account handler that the last call was
     * successful and the account is definitely "online".
     */
    public void setAccountOnline() {
        logger.debug("Account handler notified of successful request - it must be online.");
        updateStatus(ThingStatus.ONLINE);
    }

    /**
     * Creates a request to send to the Flume API but does not send the request.
     *
     * @param uri The URI the request is going to; the path *after* the api address.
     * @param method The HTTP method to use.
     * @param content The content of the request (for post requests).
     * @return A formed Jetty request.
     */
    public @Nullable Request createAsyncRequest(String uri, HttpMethod method,
            @Nullable StringContentProvider content) {
        String url = FLUME_API_ENDPOINT + uri;
        logger.debug("Creating request to {} with method type {} and content {}", uri, method, content);
        try { // Create the request
            HttpClient outClient = getClient();
            Request newRequest = outClient.newRequest(url).method(method).timeout(3, TimeUnit.SECONDS)
                    .header("content-type", "application/json");
            // Add content, if it exists
            if (content != null) {
                newRequest.content(content);
            }
            logger.trace("Request scheme: {}", newRequest.getScheme());
            logger.trace("Request method: {}", newRequest.getMethod());
            logger.trace("Request host: {}", newRequest.getHost());
            logger.trace("Request path: {}", newRequest.getPath());
            logger.trace("Request headers: {}", newRequest.getHeaders());
            if (newRequest.getContent() != null) {
                logger.trace("Request content: {}", newRequest.getContent());
            }
            return newRequest;
        } catch (Exception e) {
            logger.error("Unable to create client for request: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Confirms that the user is authorized to make requests and then forms a request for an endpoint that requires the
     * authorization but does not send the request.
     *
     * @param uri The URI the request is going to; the path *after* the api address.
     * @param method The HTTP method to use.
     * @param content The content of the request (for post requests).
     * @return A formed Jetty request.
     */
    public @Nullable Request createAuthorizedRequest(String uri, HttpMethod method,
            @Nullable StringContentProvider content) {
        logger.trace("Confirming authorization before creating request");
        boolean authResult = false;
        try {
            authResult = authorizer.isAuthorized().join();
            updateStatus(ThingStatus.ONLINE);
        } catch (CancellationException e) {
            logger.warn("Authorization attempt was canceled unexpectedly!");
            return null;
        } catch (CompletionException e) {
            if (e.getCause() instanceof AuthorizationException) {
                applyAuthorizationError(e.getMessage());
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getCause().getMessage());
            }
            return null;
        }
        // check for authorization and then send the request
        if (authResult) {
            long userId = authorizer.getUserId();
            String url = FLUME_API_ENDPOINT + "users/" + userId + uri;
            logger.debug("Creating request with authorization to {} with method type {} and content {}", uri, method,
                    content);
            Request newRequest = createAsyncRequest(url, method, content);
            if (newRequest != null) {
                newRequest.header("authorization", "Bearer " + authorizer.getAccessToken());
                return newRequest;
            }
        }
        return null;
    }
}
