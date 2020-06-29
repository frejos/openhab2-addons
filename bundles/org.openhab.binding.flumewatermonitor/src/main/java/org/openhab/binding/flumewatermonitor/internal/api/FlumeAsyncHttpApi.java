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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.flumewatermonitor.internal.handler.FlumeAccountHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

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
    @Nullable
    public CompletableFuture<List<FlumeDeviceData>> getAllDevices() {
        long userId = authorizer.getUserId();
        String url = FLUME_API_ENDPOINT + userId + "/devicess";

        return getAsyncResponseData(url, HttpMethod.GET, null).thenApply(responseData -> {
            List<FlumeDeviceData> deviceList = new ArrayList<>();
            for (String device : responseData) {
                deviceList.add(gson.fromJson(device, FlumeDeviceData.class));
            }
            return deviceList;
        });
    }

    /**
     * Gets information about a single device
     *
     * @return an array of device id's
     */
    @Nullable
    public CompletableFuture<@Nullable FlumeDeviceData> getDevice(long deviceId) {
        long userId = authorizer.getUserId();
        String url = FLUME_API_ENDPOINT + userId + "/devices/" + deviceId;

        return getAsyncResponseData(url, HttpMethod.GET, null)
                .thenApply(responseData -> gson.fromJson(responseData[0], FlumeDeviceData.class));
    }

    /**
     * Submits a query for minute-by-minute water usage
     *
     * @return an array of time-value pairs
     */
    @Nullable
    public CompletableFuture<Float> getWaterUse(long deviceId, long numberMinutes) {
        long userId = authorizer.getUserId();
        String url = FLUME_API_ENDPOINT + userId + "/devices/" + deviceId + "/query";

        return getAsyncResponseData(url, HttpMethod.POST, createNewQueryRequestContent(numberMinutes))
                .thenApply(responseData -> {
                    FlumeQueryData queryResult = gson.fromJson(responseData[0], FlumeQueryData.class);
                    return queryResult.valuePairs[0].value;
                });
    }

    /**
     * Gets a list of the user's devices
     *
     * @return an array of device id's
     */
    @Nullable
    private CompletableFuture<String[]> getAsyncResponseData(String uri, HttpMethod method,
            @Nullable StringContentProvider content) {
        final CompletableFuture<String[]> future = new CompletableFuture<>();

        boolean isAuthorized = false;
        try {
            isAuthorized = authorizer.isAuthorized();
        } catch (AuthorizationException e) {
            future.completeExceptionally(new AuthorizationException(e.getMessage()));
        } catch (IOException e) {
            future.completeExceptionally(new IOException(e.getMessage()));
        }
        if (!isAuthorized) {
            future.completeExceptionally(new AuthorizationException("Authentication failure!"));
        }

        long userId = authorizer.getUserId();
        client.newRequest(FLUME_API_ENDPOINT + userId + "/devices").method(method).timeout(3, TimeUnit.SECONDS)
                .header("authorization", "Bearer " + authorizer.getAccessToken()).content(content)
                .send(new BufferingResponseListener() {
                    @Override
                    public void onComplete(@Nullable Result result) {
                        if (result != null && result.getFailure() != null) {
                            future.completeExceptionally(result.getFailure());
                        }
                        try {
                            String jsonResponse = getContentAsString(StandardCharsets.UTF_8);
                            String[] responseData = getResponseData(jsonResponse);
                            future.complete(responseData);
                        } catch (JsonSyntaxException e) {
                            logger.warn("Syntax error in JSON returned by request to {}!", uri);
                            future.completeExceptionally(e);
                        } catch (JsonParseException e) {
                            logger.warn("Exception parsing JSON returned by request to {}!", uri);
                            future.completeExceptionally(e);
                        } catch (NullPointerException e) {
                            logger.warn("One or more expected fields were null in response to request to {}!", uri);
                            future.completeExceptionally(e);
                        } catch (AuthorizationException e) {
                            logger.warn("Authentication failure in response to request to {}!", uri);
                            future.completeExceptionally(e);
                        } catch (IOException e) {
                            logger.warn("IOException in response to request to {}!", uri);
                            future.completeExceptionally(e);
                        }
                    }
                });
        return future;
    }

    /**
     * Parses a byte request response into the needed token components.
     *
     * @param responseContent The raw request response.
     *
     * @throws AuthorizationException
     * @throws IOException
     */
    private String @Nullable [] getResponseData(String responseContent) throws AuthorizationException, IOException {
        try {
            // First parse the whole response into a generic API response.
            FlumeResponseDTO dto = gson.fromJson(responseContent, FlumeResponseDTO.class);
            // Immediately bail if this wasn't successful
            if (!dto.success) {
                if (dto.httpResponseCode == 401 || dto.httpResponseCode == 403) {
                    throw new AuthorizationException("Failed to execute API call: " + dto.message);
                } else if (dto.httpResponseCode == 404) {
                    throw new IOException("Resource not found");
                }
            }
            return dto.dataResults;
        } catch (JsonSyntaxException e) {
            logger.warn("Error deserializing JSON response from Flume API!");
            return new String[0];
        } catch (AuthorizationException e) {
            throw new AuthorizationException(e.getMessage());
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }
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
}
