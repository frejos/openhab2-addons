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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.flumewatermonitor.internal.config.FlumeAccountConfiguration;
import org.openhab.binding.flumewatermonitor.internal.handler.FlumeAccountHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private Gson gson;

    private Boolean isAuthorized;

    public FlumeJWTAuthorizer(FlumeAccountHandler handler) {
        this.accountHandler = handler;
        this.gson = accountHandler.getGson();
        logger.debug("Created a JWT authorizer for the Flume account");
        isAuthorized = false;
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
        FlumeJWTToken currentParsedToken = this.parsedToken;
        if (currentParsedToken != null) {
            return currentParsedToken.flumeUserId;
        }
        return 0;
    }

    /**
     * Check for current authentication with the Flume API.
     *
     * @return true if current authorized with the Flume API
     */
    public CompletableFuture<Boolean> isAuthorized() {
        return checkTokens().thenApply(result -> isAuthorized).exceptionally(e -> {
            logger.debug("Completed exceptionally {}", e.getMessage());
            return false;
        });
    }

    /**
     * Check that the refresh and access tokens are valid.
     */
    private CompletableFuture<@Nullable Void> checkTokens() {
        String currentRefresh = this.refreshToken;
        if (currentRefresh == null) {
            logger.trace("No stored refresh token.  A new refresh and access token will be required.");
            return getNewTokens();
        } else if (isExpired(accessTokenExpiresAt)) {
            logger.trace("Access token is expired, use refresh token to get a new one.");
            return refreshAccessToken();
        } else {
            logger.trace("Tokens are valid, yay!");
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

    private void parseTokenResponse(FlumeTokenData tokenEnvelope) {
        logger.trace("Attempting to parse the token response");
        accessToken = tokenEnvelope.accessToken;
        refreshToken = tokenEnvelope.refreshToken;
        accessTokenExpiresAt = getExpiresAt(tokenEnvelope.accessTokenExpires);
        logger.trace("FlumeJWTAuth: New access token:  {}", accessToken);
        logger.trace("FlumeJWTAuth: New access token expires in {} sec", tokenEnvelope.accessTokenExpires);
        logger.trace("FlumeJWTAuth: New refresh token: {}", refreshToken);

        // Split the token into its three parts
        // This will also throw a null pointer exception if the data is missing.
        String[] tokenArray;
        String currentToken = this.accessToken;
        if (currentToken != null) {
            tokenArray = currentToken.split("\\.");
        }

        if (tokenArray.length == 3) {
            try {
                String tokenPayload = new String(Base64.getDecoder().decode(tokenArray[1]));
                parsedToken = gson.fromJson(tokenPayload, FlumeJWTToken.class);
                isAuthorized = true;
            } catch (JsonSyntaxException e) {
                logger.warn("Error deserializing JSON response to access token request!");
            }
        }
    }

    private CompletableFuture<@Nullable Void> sendTokenRequest(StringContentProvider content) {
        // Create a future to complete
        final CompletableFuture<FlumeTokenData []> future = new CompletableFuture<>();
        // Create a listener for the response
        FlumeResponseListener<FlumeTokenData> listener = new FlumeResponseListener<>(future, gson);

        try {
            HttpClient client = accountHandler.getClient();
            Request newRequest = client.newRequest("https://api.flumetech.com/oauth/token").method(HttpMethod.POST)
                    .content(content).timeout(3, TimeUnit.SECONDS).header("content-type", "application/json");
            logger.trace("Request scheme: {}", newRequest.getScheme());
            logger.trace("Request method: {}", newRequest.getMethod());
            logger.trace("Request host: {}", newRequest.getHost());
            logger.trace("Request path: {}", newRequest.getPath());
            logger.trace("Request headers: {}", newRequest.getHeaders());
            if (newRequest.getContent() != null) {
                logger.trace("Request content: {}", newRequest.getContent());
            }
            newRequest.send(listener);
        } catch (Exception e) {
            logger.debug("Error in sending request: {}", e.getMessage());
            future.completeExceptionally(e);
        }

        // Return the future
        return future.thenApply(result -> {
            FlumeTokenData firstEnvelope = result[0];
            logger.trace("Returned token envelope:  {}", firstEnvelope);
            if (firstEnvelope == null) {
                logger.info("Returned token envelope is null");
                voidTokens();
                future.completeExceptionally(new IOException("Returned token envelope is null!"));
            } else {
                parseTokenResponse(firstEnvelope);
            }
            return null;
        }).exceptionally(e -> {
            logger.info("Exception in the token request future: {}", e.getMessage());
            future.completeExceptionally(new IOException(e.getMessage()));
            voidTokens();
            return null;
        });
    }

    private CompletableFuture<@Nullable Void> getNewTokens() {
        // First check to see if another thread has updated it
        if (!isExpired(accessTokenExpiresAt)) {
            logger.debug("Access and refresh tokens are still valid; new ones are not needed.");
            return CompletableFuture.completedFuture(null);
        }
        logger.debug("FlumeJWTAuth: Requesting a new access and refresh token.");

        return sendTokenRequest(createNewTokenRequestContent());
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
        // First check to see if another thread has updated it
        if (!isExpired(accessTokenExpiresAt)) {
            logger.debug("Access token is still valid; a new one is not needed.");
            return CompletableFuture.completedFuture(null);
        }
        logger.debug("FlumeJWTAuth: Updating expired ACCESS token using refresh token {}", refreshToken);

        return sendTokenRequest(createRefreshTokenRequestContent());
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
