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

import static org.openhab.binding.flumewatermonitor.internal.FlumeWaterMonitorBindingConstants.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.flumewatermonitor.internal.exceptions.NotFoundException;
import org.openhab.binding.flumewatermonitor.internal.handler.FlumeAccountHandler;
import org.openhab.binding.flumewatermonitor.internal.model.FlumeDeviceData;
import org.openhab.binding.flumewatermonitor.internal.model.FlumeDeviceType;
import org.openhab.binding.flumewatermonitor.internal.model.FlumeQueryData;
import org.openhab.binding.flumewatermonitor.internal.model.FlumeQueryValuePair;
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
    private static final String NULL_REQUEST_NOTICE = "Created request was null!";

    /**
     * Create a new {@link FlumeAsyncHttpApi}
     */
    public FlumeAsyncHttpApi(FlumeAccountHandler accountHandler) {
        this.accountHandler = accountHandler;
    }

    /**
     * Gets a list of the user's devices
     *
     * @return an array of device id's
     */
    public CompletableFuture<FlumeDeviceData[]> getAllDevices() {
        // The uri this request is going to
        String uri = "/devices";

        // Create a listener for the response
        FlumeResponseListener<FlumeDeviceData> listener = new FlumeResponseListener<>(FlumeDeviceData[].class);
        // Create the request
        @Nullable
        Request newRequest = accountHandler.createAuthorizedRequest(uri, HttpMethod.GET, null);

        // If we couldn't create the requst, return a future completed exceptionally
        // @note - In the raw json, everything can be null, but the onComplete function that completes this future will
        // not complete regularly if there are unexpected nulls. So here we can know that the fields are non-null.
        if (newRequest == null) {
            CompletableFuture<FlumeDeviceData[]> future = new CompletableFuture<>();
            logger.debug(NULL_REQUEST_NOTICE);
            future.completeExceptionally(new IOException(NULL_REQUEST_NOTICE));
            return future;
        }

        // Get the future from the listener
        CompletableFuture<FlumeDeviceData[]> future = listener.getFutureArray();

        // Send the request
        newRequest.send(listener);

        // Return the future
        return future;
    }

    /**
     * Gets information about a single device
     *
     * @return an array of device id's
     */
    public CompletableFuture<@Nullable FlumeDeviceData> getDevice(long deviceId) {
        // The uri this request is going to
        String uri = "/devices/" + deviceId;

        // Create a listener for the response
        FlumeResponseListener<FlumeDeviceData> listener = new FlumeResponseListener<>(FlumeDeviceData[].class);
        // Create the request
        @Nullable
        Request newRequest = accountHandler.createAuthorizedRequest(uri, HttpMethod.GET, null);

        // If we couldn't create the requst, return a future completed exceptionally
        if (newRequest == null) {
            CompletableFuture<@Nullable FlumeDeviceData> future = new CompletableFuture<>();
            logger.debug(NULL_REQUEST_NOTICE);
            future.completeExceptionally(new IOException(NULL_REQUEST_NOTICE));
            return future;
        }

        // Get the future from the listener
        // We should only get one device back here, so we'll just ask for the first one.
        CompletableFuture<@Nullable FlumeDeviceData> future = listener.getFutureSingle();

        // Send the request
        newRequest.send(listener);

        // Return the future
        return future.thenApply(firstDevice -> {
            logger.trace("Returned device:  {}", firstDevice);
            if (firstDevice == null) {
                logger.debug(NULL_REQUEST_NOTICE);
                future.completeExceptionally(new IOException(NULL_REQUEST_NOTICE));
                return null;
            } else if (firstDevice.deviceType == FlumeDeviceType.FlumeBridge) {
                logger.warn("Incorrect device type returned!  Expecting a flume sensor and got a bridge.");
                future.completeExceptionally(new NotFoundException("Expecting a flume sensor and got a bridge!"));
                return null;
            }
            return firstDevice;
        }).exceptionally(e -> {
            logger.info("Exception in the token request future: {}", e.getMessage());
            future.completeExceptionally(new IOException(e.getMessage()));
            return null;
        });
    }

    /**
     * Submits a query for minute-by-minute water usage
     *
     * @return the latest water use value
     */
    public CompletableFuture<Float> getWaterUse(long deviceId, long numberMinutes) {
        // The uri this request is going to
        String uri = "/devices/" + deviceId + "/query";

        // Create a listener for the response
        FlumeResponseListener<FlumeQueryData> listener = new FlumeResponseListener<>(FlumeQueryData[].class);
        // Create the request
        @Nullable
        Request newRequest = accountHandler.createAuthorizedRequest(uri, HttpMethod.POST,
                createNewQueryRequestContent(numberMinutes));

        // If we couldn't create the requst, return a future completed exceptionally
        if (newRequest == null) {
            CompletableFuture<Float> future = new CompletableFuture<>();
            logger.debug(NULL_REQUEST_NOTICE);
            future.completeExceptionally(new IOException(NULL_REQUEST_NOTICE));
            return future;
        }

        // Get the future from the listener
        // We should only get one device back here, so we'll just ask for the first one.
        CompletableFuture<@Nullable FlumeQueryData> future = listener.getFutureSingle();

        // Send the request
        newRequest.send(listener);

        // Return the future
        return future.thenApply(firstQueryData -> {
            logger.trace("Returned qeury data:  {}", firstQueryData);
            if (firstQueryData == null) {
                String message = "Returned qeury data is null";
                logger.debug(message);
                future.completeExceptionally(new IOException(message));
                return (float) 0;
            }
            @Nullable
            FlumeQueryValuePair[] valuePairs = firstQueryData.valuePairs;
            if (valuePairs == null) {
                String message = "No value pairs in the query result!";
                logger.debug(message);
                future.completeExceptionally(new NotFoundException(message));
                return (float) 0;
            } else if (valuePairs.length == 0) {
                String message = "The value pair array is empty!";
                logger.debug(message);
                future.completeExceptionally(new NotFoundException(message));
                return (float) 0;
            }
            FlumeQueryValuePair firstValuePair = valuePairs[0];
            if (firstValuePair == null) {
                String message = "The first value pair is null. Weird!";
                logger.debug(message);
                future.completeExceptionally(new NotFoundException(message));
                return (float) 0;
            }
            logger.debug("First value result: {}", firstValuePair.value);
            return firstValuePair.value;
        }).exceptionally(e -> {
            logger.info("Exception in the token request future: {}", e.getMessage());
            future.completeExceptionally(new IOException(e.getMessage()));
            return (float) 0;
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
        String startTimeString = startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        // End time - this is optional in the request
        LocalDateTime endTime = LocalDateTime.now();
        String endTimeString = endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String requestBody = "{\"queries\":[{\"request_id\":\"" + FLUME_QUERY_REQUEST_ID + "\",\"since_datetime\":\""
                + startTimeString + "\",\"until_datetime\":\"" + endTimeString
                + "\",\"bucket\":\"MIN\",\"group_multiplier\":" + numberMinutes
                + ",\"operation\":\"SUM\",\"sort_direction\":\"ASC\"}]}";
        logger.trace("Water use query request content: {}", requestBody);
        return new StringContentProvider(requestBody, StandardCharsets.UTF_8);
    }
}
