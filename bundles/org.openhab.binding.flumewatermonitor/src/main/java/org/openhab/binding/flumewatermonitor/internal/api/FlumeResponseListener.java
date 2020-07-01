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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * {@link FlumeResponseListener} listens for async http responses
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault

public class FlumeResponseListener<T extends FlumeDataInterface> extends BufferingResponseListener {
    private final Logger logger = LoggerFactory.getLogger(FlumeResponseListener.class);

    private CompletableFuture<@Nullable T @Nullable []> future;
    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").create();

    public FlumeResponseListener() {
        this.future = new CompletableFuture<>();
    }

    public CompletableFuture<@Nullable T @Nullable []> getFutureArray() {
        return this.future;
    }

    public CompletableFuture<@Nullable T> getFutureSingle() {
        return this.future.thenApply(result -> {
            if (result != null) {
                return result[0];
            } else {
                return null;
            }
        }).exceptionally(e -> {
            logger.debug("Exception in Flume response listener: {}", e.getMessage());
            if (e.getCause() != null) {
                logger.debug("Inner Exception: {}", e.getCause().getMessage());
            }
            return null;
        });
    }

    // Create a listener for the onComplete callback after the request finishes
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
            FlumeResponseDTO<T> dto = gson.fromJson(jsonResponse, new TypeToken<FlumeResponseDTO<T>>() {
            }.getType());
            if (dto == null) {
                throw new IOException("No DTO could be parsed from JSON");
            }
            dto.checkForExceptions();

            // Try to extract the usage data from the response.
            @Nullable
            T @Nullable [] resultDatas = dto.dataResults;
            if (resultDatas == null) {
                throw new IOException("No result data returned in the response");
            }
            // Try to extract the device data from the response.
            if (resultDatas.length == 0) {
                throw new IOException("No results in the array!");
            }
            logger.trace("{} result[s] returned", resultDatas.length);
            for (int i = 0; i < resultDatas.length; i++) {
                if (resultDatas[i] != null) {
                    logger.trace("Result {}:  {}", i, resultDatas[i]);
                } else {
                    logger.trace("Result {} is null!", i);
                    throw new IOException("Malformed array, result " + i + " is null");
                }
            }
            future.complete(resultDatas);
        } catch (AuthorizationException | NotFoundException | IOException e) {
            logger.debug("Exception in Flume response listener: {}", e.getMessage());
            if (e.getCause() != null) {
                logger.debug("Inner Exception: {}", e.getCause().getMessage());
            }
            future.completeExceptionally(e);
        }
    }
}
