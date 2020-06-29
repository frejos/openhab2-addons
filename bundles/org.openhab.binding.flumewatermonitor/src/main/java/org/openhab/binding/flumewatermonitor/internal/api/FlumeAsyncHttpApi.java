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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.flumewatermonitor.internal.handler.FlumeAccountHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * {@link Logger} wraps the Flume Tech cloud REST API.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class FlumeAsyncHttpApi {

    private final Logger logger = LoggerFactory.getLogger(FlumeAsyncHttpApi.class);
    private final FlumeJWTAuthorizer authorizer;
    private final Gson gson;

    private HttpClient client;

    public FlumeAsyncHttpApi(FlumeAccountHandler accountHandler) {
        this.authorizer = accountHandler.getAuthorizer();
        this.gson = accountHandler.getGson();
        this.client = new HttpClient();
    }

    /**
     * Gets a list of the user's devices
     *
     * @return an array of device id's
     */
    public CompletableFuture<List<FlumeDeviceData>> getAllDevices() {
        // Create the future to complete
        final CompletableFuture<List<FlumeDeviceData>> future = new CompletableFuture<>();
        // The uri this request is going to
        String uri = "/devices";

        // Create a listener for the onComplete callback after the request finishes
        BufferingResponseListener listener = new BufferingResponseListener() {
            @Override
            public void onComplete(@Nullable Result result) {
                if (result != null && result.getFailure() != null) {
                    future.completeExceptionally(result.getFailure());
                }
                try {
                    logger.debug("Content returned by devices query: {}", getContentAsString(StandardCharsets.UTF_8));
                    String jsonResponse = getContentAsString(StandardCharsets.UTF_8);

                    // Parse the JSON response
                    FlumeResponseDTO<FlumeDeviceData> dto = gson.fromJson(jsonResponse,
                            new TypeToken<FlumeResponseDTO<FlumeDeviceData>>() {
                            }.getType());
                    dto.checkForExceptions();

                    // Try to extract the device data from the response.
                    List<FlumeDeviceData> deviceList = new ArrayList<>();
                    if (dto.dataResults != null && dto.dataResults[0] != null) {
                        for (FlumeDeviceData device : dto.dataResults) {
                            logger.trace("device:  {}", dto.dataResults[0]);
                            deviceList.add(device);
                        }
                    }
                    future.complete(deviceList);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        };

        // check for authorization and then send the request
        sendAsyncRequest(uri, HttpMethod.GET, null, listener);

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
        final CompletableFuture<@Nullable FlumeDeviceData> future = new CompletableFuture<>();
        // The uri this request is going to
        String uri = "/devices/" + deviceId;

        // Create a listener for the onComplete callback after the request finishes
        BufferingResponseListener listener = new BufferingResponseListener() {
            @Override
            public void onComplete(@Nullable Result result) {
                if (result != null && result.getFailure() != null) {
                    future.completeExceptionally(result.getFailure());
                }
                try {
                    logger.debug("Content returned by single device query: {}",
                            getContentAsString(StandardCharsets.UTF_8));
                    String jsonResponse = getContentAsString(StandardCharsets.UTF_8);

                    // Parse the JSON response
                    FlumeResponseDTO<FlumeDeviceData> dto = gson.fromJson(jsonResponse,
                            new TypeToken<FlumeResponseDTO<FlumeDeviceData>>() {
                            }.getType());
                    dto.checkForExceptions();

                    // Try to extract the device data from the response.
                    if (dto.dataResults != null && dto.dataResults[0] != null) {
                        logger.trace("device info:  {}", dto.dataResults[0]);
                        future.complete(dto.dataResults[0]);
                    } else {
                        future.complete(null);
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        };

        // check for authorization and then send the request
        sendAsyncRequest(uri, HttpMethod.GET, null, listener);

        // Return the future
        return future;
    }

    /**
     * Submits a query for minute-by-minute water usage
     *
     * @return the latest water use value
     */
    public CompletableFuture<Float> getWaterUse(long deviceId, long numberMinutes) {
        // Create the future to complete
        final CompletableFuture<Float> future = new CompletableFuture<>();
        // The uri this request is going to
        String uri = "/devices/" + deviceId + "/query";

        // Create a listener for the onComplete callback after the request finishes
        BufferingResponseListener listener = new BufferingResponseListener() {
            @Override
            public void onComplete(@Nullable Result result) {
                if (result != null && result.getFailure() != null) {
                    future.completeExceptionally(result.getFailure());
                }
                try {
                    logger.debug("Content returned by usage query: {}", getContentAsString(StandardCharsets.UTF_8));
                    String jsonResponse = getContentAsString(StandardCharsets.UTF_8);

                    // Parse the JSON response
                    FlumeResponseDTO<FlumeQueryData> dto = gson.fromJson(jsonResponse,
                            new TypeToken<FlumeResponseDTO<FlumeQueryData>>() {
                            }.getType());
                    dto.checkForExceptions();

                    // Try to extract the usage data from the response.
                    if (dto.dataResults != null && dto.dataResults[0] != null && dto.dataResults[0].valuePairs != null
                            && dto.dataResults[0].valuePairs[0] != null) {
                        logger.trace("water use:  {}", dto.dataResults[0].valuePairs[0].value);
                        future.complete(dto.dataResults[0].valuePairs[0].value);
                    } else {
                        future.complete((float) 0);
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        };

        // check for authorization and then send the request
        sendAsyncRequest(uri, HttpMethod.POST, createNewQueryRequestContent(numberMinutes), listener);

        // Return the future
        return future;
    }

    /**
     * Create the query request body.
     *
     * @return The request body
     */
    private StringContentProvider createNewQueryRequestContent(long numberMinutes) {
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(numberMinutes);
        String startTimeString = startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:MM:SS"));
        LocalDateTime endTime = LocalDateTime.now();
        String endTimeString = endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:MM:SS"));

        String requestBody = "{\"request_id\":\"" + FLUME_QUERY_REQUEST_ID + "\",\"since_datetime\":\""
                + startTimeString + "\",\"until_datetime\":\"" + endTimeString
                + "\",\"bucket\":\"MIN\",\"group_multiplier\":" + numberMinutes
                + ",\"operation\":\"SUM\",\"sort_direction\":\"ASC\"}";
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
                // Create the request
                Request request = client.newRequest(url).method(method).timeout(3, TimeUnit.SECONDS)
                        .header("authorization", "Bearer " + authorizer.getAccessToken());
                // Add content, if it exists
                if (content != null)
                    request.content(content);
                // Send the request with the listener attached
                request.send(listener);
            }
        });
    }
}
