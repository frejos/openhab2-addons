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
package org.openhab.binding.flumewatermonitor.internal.api;

import static org.openhab.binding.flumewatermonitor.internal.FlumeWaterMonitorBindingConstants.FLUME_API_ENDPOINT;
import static org.openhab.binding.flumewatermonitor.internal.FlumeWaterMonitorBindingConstants.FLUME_QUERY_REQUEST_ID;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.flumewatermonitor.internal.handler.FlumeAccountHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Logger} wraps the Flume Tech cloud REST API.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class FlumeAsyncHttpApi {
    private final Logger logger = LoggerFactory.getLogger(FlumeAsyncHttpApi.class);
    private FlumeAccountHandler accountHandler;
    private FlumeJWTAuthorizer authorizer;
    private Gson gson;

    public FlumeAsyncHttpApi(FlumeAccountHandler accountHandler) {
        this.accountHandler = accountHandler;
        this.authorizer = accountHandler.getAuthorizer();
        this.gson = accountHandler.getGson();
    }

    /**
     * Gets a list of the user's devices
     *
     * @return an array of device id's
     */
    public CompletableFuture<FlumeDeviceData []> getAllDevices() {
        // Create the future to complete
        final CompletableFuture<FlumeDeviceData []> future = new CompletableFuture<>();
        // The uri this request is going to
        String uri = "/devices";

        // Create a listener for the onComplete callback after the request finishes
        FlumeResponseListener<FlumeDeviceData> listener = new FlumeResponseListener<>();

        // check for authorization and then send the request
        try {
            sendAsyncRequest(uri, HttpMethod.GET, null, listener);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        // Return the future
        return future;
    }

    /**
     * Gets information about a single device
     *
     * @return an array of device id's
     */
    public CompletableFuture<@Nullable FlumeDeviceData> getDevice(long deviceId) {
        // Create the future to complete
        final CompletableFuture<@Nullable FlumeDeviceData []> future = new CompletableFuture<>();

        // The uri this request is going to
        String uri = "/devices/" + deviceId;

        // Create a listener for the onComplete callback after the request finishes
        FlumeResponseListener<FlumeDeviceData> listener = new FlumeResponseListener<>();

        // check for authorization and then send the request
        try {
            sendAsyncRequest(uri, HttpMethod.GET, null, listener);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        // Return the future
        return future.thenApply(listResult -> {
            FlumeDeviceData firstDevice = listResult[0];
            logger.trace("Returned device:  {}", firstDevice);
            if (firstDevice == null) {
                logger.debug("Returned device is null");
                future.completeExceptionally(new IOException("Returned device is null!"));
            } else if (firstDevice.deviceType == FlumeDeviceType.FlumeBridge) {
                logger.debug("Incorrect device type returned!  Expecting a flume sensor and got a bridge.");
                future.completeExceptionally(new NotFoundException("Expecting a flume sensor and got a bridge!"));
            }
            return firstDevice;
        });
    }

    /**
     * Submits a query for minute-by-minute water usage
     *
     * @return the latest water use value
     */
    public CompletableFuture<Float> getWaterUse(long deviceId, long numberMinutes) {
        // Create the future to complete
        final CompletableFuture<FlumeQueryValuePair []> future = new CompletableFuture<>();
        // The uri this request is going to
        String uri = "/devices/" + deviceId + "/query";

        // Create a listener for the onComplete callback after the request finishes
        FlumeResponseListener<FlumeQueryValuePair> listener = new FlumeResponseListener<>();

        // check for authorization and then send the request
        try {
            sendAsyncRequest(uri, HttpMethod.POST, createNewQueryRequestContent(numberMinutes), listener);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        // Return the future
        return future.thenApply(listResult -> {
            FlumeQueryValuePair firstValuePair = listResult[0];
            logger.trace("Returned value pairs:  {}", firstValuePair);
            if (firstValuePair == null) {
                logger.info("First returned value pair is null");
                future.completeExceptionally(new IOException("First returned value pair is null!"));
            }
            logger.debug("water use: {}", firstValuePair.value);
            return firstValuePair.value;
        });
    }

    /**
     * Create the query request body.
     *
     * @return The request body
     */
    private StringContentProvider createNewQueryRequestContent(long numberMinutes) {
        // Start time - rounded down to the minute
        // Round down to avoid having issues between the server times which lead to the other server rejecting the
        // request for having an apparently in the starting future timestamp
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(numberMinutes).truncatedTo(ChronoUnit.MINUTES);
        String startTimeString = startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:MM:SS"));
        // End time - this is optional in the request
        LocalDateTime endTime = LocalDateTime.now();
        String endTimeString = endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:MM:SS"));

        String requestBody = "{\"queries\":[{\"request_id\":\"" + FLUME_QUERY_REQUEST_ID + "\",\"since_datetime\":\""
                + startTimeString + "\",\"until_datetime\":\"" + endTimeString
                + "\",\"bucket\":\"MIN\",\"group_multiplier\":" + numberMinutes
                + ",\"operation\":\"SUM\",\"sort_direction\":\"ASC\"}]}";
        logger.trace("Water use query request content: {}", requestBody);
        return new StringContentProvider(requestBody, StandardCharsets.UTF_8);
    }

    private void sendAsyncRequest(String uri, HttpMethod method, @Nullable StringContentProvider content,
            BufferingResponseListener listener) {
        // check for authorization and then send the request
        authorizer.isAuthorized().thenAccept(authResult -> {
            if (authResult) {
                long userId = authorizer.getUserId();
                String url = FLUME_API_ENDPOINT + userId + uri;
                logger.debug("Making request to {} with method type {} and content {}", url, method, content);
                try { // Create the request
                    HttpClient client = accountHandler.getClient();
                    Request newRequest = client.newRequest(url).method(method).timeout(3, TimeUnit.SECONDS)
                            .header("authorization", "Bearer " + authorizer.getAccessToken())
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
                    // Send the request with the listener attached
                    newRequest.send(listener);
                } catch (Exception e) {
                    logger.error("Unable to send request: {}", e.getMessage());
                }
            }
        });
    }
}
