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
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.flumewatermonitor.internal.config.FlumeAccountConfiguration;
import org.openhab.binding.flumewatermonitor.internal.handler.FlumeAccountHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/**
 * The {@link FlumeJWTAuthorizer} manages the authentication process with the
 * Flume Tech API. This class requests access and refresh tokens based on the
 * expiration times provided by said API.
 *
 * Taken from the ZoneMinder (v2)binding by Mark Hilbush
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class FlumeJWTAuthorizer {
    private final Logger logger = LoggerFactory.getLogger(FlumeJWTAuthorizer.class);

    private FlumeAccountHandler accountHandler;

    private @Nullable String accessToken;
    private @Nullable FlumeJWTToken parsedToken;
    private @Nullable String refreshToken;
    private long accessTokenExpiresAt;

    private HttpClient client;

    private Boolean isAuthorized;

    public FlumeJWTAuthorizer(FlumeAccountHandler handler) {
        this.accountHandler = handler;
        logger.debug("Created a JWT authorizer for the Flume account");
        isAuthorized = false;

        client = new HttpClient();
    }

    /**
     * Provide the current access token for use in other API calls.
     *
     * @return The current API access token.
     */
    public @Nullable String getAccessToken() {
        return this.accessToken;
    }

    /**
     * Provide the numeric user ID associated with the API.
     *
     * @return The the numeric user ID for API access.
     */
    public long getUserId() {
        return this.parsedToken.flumeUserId;
    }

    /**
     * Check for current authentication with the Flume API.
     *
     * @return true if current authorized with the Flume API
     * @throws AuthorizationException
     * @throws IOException
     * @throws InterruptedException
     * @throws NotFoundException
     */
    public CompletableFuture<Boolean> isAuthorized() {
        CompletableFuture<Boolean> f = new CompletableFuture<>();
        checkTokens().thenRun(() -> f.complete(isAuthorized));
        return f;
    }

    /**
     * Check that the refresh and access tokens are valid.
     */
    private CompletableFuture<@Nullable Void> checkTokens() {
        if (refreshToken == null) {
            return getNewTokens();
        } else if (isExpired(accessTokenExpiresAt)) {
            return refreshAccessToken();
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Create the authentication request body for a request for a new access and
     * refresh token.
     *
     * @return The request body
     */
    private StringContentProvider createNewTokenRequestContent() {
        FlumeAccountConfiguration currentConfig = accountHandler.getAccountConfiguration();
        Gson gson = accountHandler.getGson();
        String requestBody = gson.toJson(currentConfig);
        logger.trace("Access token request content: {}", requestBody);
        return new StringContentProvider(requestBody, StandardCharsets.UTF_8);
    }

    /**
     * Sets tokens to null and times to 0. Called after an authentication failure.
     */
    private void voidTokens() {
        isAuthorized = false;
        accessToken = null;
        refreshToken = null;
        accessTokenExpiresAt = System.currentTimeMillis() / 1000;
    }

    /**
     * Parses a byte request response into the needed token components.
     *
     * @param tokenResponseContent The raw request response.
     */
    private void parseTokenResponse(String tokenResponse)
            throws AuthorizationException, IOException, NotFoundException {
        Gson gson = accountHandler.getGson();
        try {
            // First parse the whole response into a generic API response.
            FlumeResponseDTO<FlumeTokenData> dto = gson.fromJson(tokenResponse,
                    new TypeToken<FlumeResponseDTO<FlumeTokenData>>() {
                    }.getType());
            dto.checkForExceptions();

            // Try to parse the token envelope from the data portion of the response.
            if (dto.dataResults != null && dto.dataResults[0] != null) {
                FlumeTokenData tokenEnvelope = dto.dataResults[0];
                if (tokenEnvelope != null) {
                    accessToken = tokenEnvelope.accessToken;
                    refreshToken = tokenEnvelope.refreshToken;
                    accessTokenExpiresAt = getExpiresAt(tokenEnvelope.accessTokenExpires);
                }
                logger.trace("FlumeJWTAuth: New access token:  {}", accessToken);
                logger.trace("FlumeJWTAuth: New access token expires in {} sec", tokenEnvelope.accessTokenExpires);
                logger.trace("FlumeJWTAuth: New refresh token: {}", refreshToken);
            }

            // Split the token into its three parts
            // This will also throw a null pointer exception if the data is missing.
            String[] tokenArray = {};
            String currentToken = this.accessToken;
            if (currentToken != null) {
                tokenArray = accessToken.split("\\.");
            }

            if (tokenArray.length == 3) {
                String tokenPayload = new String(Base64.getDecoder().decode(tokenArray[1]));
                parsedToken = gson.fromJson(tokenPayload, FlumeJWTToken.class);
                isAuthorized = true;
            }
        } catch (JsonSyntaxException e) {
            logger.warn("Error deserializing JSON response to access token request!");
        }
    }

    private CompletableFuture<@Nullable Void> getNewTokens() {
        final CompletableFuture<@Nullable Void> f = new CompletableFuture<>();

        // First check to see if another thread has updated it
        if (!isExpired(accessTokenExpiresAt)) {
            logger.debug("Access and refresh tokens are still valid; new ones are not needed.");
            f.complete(null);
        }
        logger.debug("FlumeJWTAuth: Requesting a new access and refresh token.");

        client.newRequest("https://api.flumetech.com/oauth/token").method(HttpMethod.POST)
                .content(createNewTokenRequestContent()).timeout(3, TimeUnit.SECONDS)
                .send(new BufferingResponseListener() {
                    @Override
                    public void onComplete(@Nullable Result result) {
                        if (result != null && result.getFailure() != null) {
                            f.completeExceptionally(result.getFailure());
                        }
                        try {
                            logger.debug("Returned content: {}", getContentAsString(StandardCharsets.UTF_8));
                            parseTokenResponse(getContentAsString(StandardCharsets.UTF_8));
                            f.complete(null);
                        } catch (Exception e) {
                            voidTokens();
                            f.completeExceptionally(e);
                        }
                    }
                });
        return f;
    }

    /**
     * Create the authentication request body for a request to refresh the access
     * token.
     *
     * @return The request body
     */
    private StringContentProvider createRefreshTokenRequestContent() {
        FlumeAccountConfiguration currentConfig = accountHandler.getAccountConfiguration();
        String requestBody = "{\"grant_type\":\"refresh_token\",\"refresh_token\":\"" + refreshToken
                + "\",\"client_id\":\"" + currentConfig.clientId + "\",\"client_secret\":\""
                + currentConfig.clientSecret + "\"}";
        logger.trace("Access token refresh request content: {}", requestBody);
        return new StringContentProvider(requestBody, StandardCharsets.UTF_8);
    }

    private CompletableFuture<@Nullable Void> refreshAccessToken() {
        final CompletableFuture<@Nullable Void> f = new CompletableFuture<>();
        // First check to see if another thread has updated it
        if (!isExpired(accessTokenExpiresAt)) {
            logger.debug("Access token is still valid; a new one is not needed.");
        }
        logger.debug("FlumeJWTAuth: Updating expired ACCESS token using refresh token {}", refreshToken);

        client.newRequest("https://api.flumetech.com/oauth/token").method(HttpMethod.POST)
                .content(createRefreshTokenRequestContent()).timeout(3, TimeUnit.SECONDS)
                .header("Content-Type", "application/json").send(new BufferingResponseListener() {
                    @Override
                    public void onComplete(@Nullable Result result) {
                        if (result != null && result.getFailure() != null) {
                            f.completeExceptionally(result.getFailure());
                        }
                        try {
                            logger.debug("Returned content: {}", getContentAsString(StandardCharsets.UTF_8));
                            parseTokenResponse(getContentAsString(StandardCharsets.UTF_8));
                            f.complete(null);
                        } catch (Exception e) {
                            voidTokens();
                            f.completeExceptionally(e);
                        }
                    }
                });
        return f;
    }

    private boolean isExpired(long expiresAt) {
        return (System.currentTimeMillis() / 1000) > expiresAt;
    }

    /**
     * Calculate the time at which the access token will expire based on the number
     * of seconds until the expiration. Subtract 5 minutes from the exact expiration
     * time for safety.
     *
     * @param expiresInSeconds The number of seconds until the token expires.
     * @return The millis time at which the token is considered to be expired.
     */
    private long getExpiresAt(long expiresInSeconds) {
        try {
            return (System.currentTimeMillis() / 1000) + (expiresInSeconds - 300);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
