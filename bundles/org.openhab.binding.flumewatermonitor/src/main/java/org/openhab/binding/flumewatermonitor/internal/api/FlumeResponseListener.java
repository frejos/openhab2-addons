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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.openhab.binding.flumewatermonitor.internal.exceptions.AuthorizationException;
import org.openhab.binding.flumewatermonitor.internal.exceptions.NotFoundException;
import org.openhab.binding.flumewatermonitor.internal.model.FlumeResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link FlumeResponseListener} listens for async http responses and translates them into a completable future that
 * will resolve to the specific time of Flume data.
 *
 * @param <T> Template parameter for the type of {@link FlumeDataInterface} that should be deseralized from the request.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault

public class FlumeResponseListener<T> extends BufferingResponseListener {
    private final Logger logger = LoggerFactory.getLogger(FlumeResponseListener.class);

    // @note - in the raw json, everything can be null, but the onComplete function that completes this future will not
    // complete regularly if there are unexpected nulls. So here we can know that the fields are non-null.
    private CompletableFuture<T[]> future;

    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").create();
    private final Class<T[]> clazz;

    /**
     * Construct a new {@link FlumeResponseListener}
     */
    public FlumeResponseListener(Class<T[]> clazz) {
        this.future = new CompletableFuture<>();
        this.clazz = clazz;
    }

    /**
     * Get a future which will complete with the full array of data subtypes.
     *
     * @note - In the raw json, everything can be null, but the onComplete function that completes this future will not
     *       complete regularly if there are unexpected nulls. So here we can know that the fields are non-null.
     *
     * @return A completable future which will resovlve to the array of data subtypes or null they aren't present in the
     *         response.
     */
    public CompletableFuture<T[]> getFutureArray() {
        return this.future;
    }

    /**
     * Get a future which will complete with the first item of the array of data subtypes or null if none are present.
     *
     * @note - This CAN complete to null!
     *
     * @return A completable future which will resovlve to first item in the array of data subtypes or null none are
     *         present in the response.
     */
    public CompletableFuture<@Nullable T> getFutureSingle() {
        return this.future.thenApply(result -> result[0]).exceptionally(e -> {
            logger.debug("Exception in Flume response listener: {}", e.getMessage());
            if (e.getCause() != null) {
                logger.debug("Inner Exception: {}", e.getCause().getMessage());
            }
            return null;
        });
    }

    /**
     * A callback that will run when a call to the API completes.
     */
    @Override
    public void onComplete(@Nullable Result result) {
        if (result == null) {
            logger.debug("No result returned!");
            future.completeExceptionally(new IOException("No response returned!"));
            return;
        }
        logger.trace("Returned results: {}", result);
        if (result.getFailure() != null) {
            future.completeExceptionally(result.getFailure());
            logger.debug("Failed request:  {}", result.getFailure().getMessage());
            return;
        }
        try {
            logger.debug("Content returned by query: {}", getContentAsString(StandardCharsets.UTF_8));
            String jsonResponse = getContentAsString(StandardCharsets.UTF_8);
            if (jsonResponse == null) {
                logger.debug("Response content string is null");
                future.completeExceptionally(new IOException("Response content string is null!"));
                return;
            }

            // Parse the JSON response
            @Nullable
            FlumeResponseDTO dto = gson.fromJson(jsonResponse, FlumeResponseDTO.class);
            if (dto == null) {
                throw new IOException("No DTO could be parsed from JSON");
            }
            // Check if the response had an internal success field and throw a bunch of exceptions if not.
            dto.checkForExceptions();

            // Try to extract the usage data from the response.
            @Nullable
            String resultDataString = dto.dataAsString;
            if (resultDataString == null) {
                throw new IOException("No result data returned in the response");
            }
            logger.trace("String with JSON of result array: {}", resultDataString);
            T[] arrayOfDatas = gson.fromJson(resultDataString, clazz);

            // Try to extract the device data from the response.
            if (arrayOfDatas.length == 0) {
                throw new IOException("No results in the array!");
            }
            logger.trace("{} result[s] returned", arrayOfDatas.length);
            for (int i = 0; i < arrayOfDatas.length; i++) {
                if (arrayOfDatas[i] != null) {
                    logger.trace("Result {}:  {}", i, arrayOfDatas[i]);
                } else {
                    // If the data field is present in the json, and it's not an empty array,
                    // there should not be any null fields in the array.
                    logger.trace("Result {} is null!", i);
                    throw new IOException("Malformed array, result " + i + " is null");
                }
            }
            logger.trace("Finished listener onComplete portion of response parsing");
            future.complete(arrayOfDatas);
        } catch (AuthorizationException | NotFoundException | IOException | RuntimeException e) {
            logger.debug("Exception in Flume response listener: {}", e.getMessage());
            if (e.getCause() != null) {
                logger.debug("Inner Exception: {}", e.getCause().getMessage());
            }
            future.completeExceptionally(e);
        }
    }
}
