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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.flumewatermonitor.internal.config.FlumeAccountConfiguration;
import org.openhab.binding.flumewatermonitor.internal.handler.FlumeAccountHandler;
import org.openhab.binding.flumewatermonitor.internal.model.FlumeTokenData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link FlumeJWTAuthorizer} manages the authentication process with the
 * Flume Tech API. This class requests access and refresh tokens based on the
 * expiration times provided by said API.
 *
 * Some portions taken from the ZoneMinder (v2) binding by Mark Hilbush and the
 * NikoHomeControlBridgeHandler2 by Mark Herwege
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
    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").create();

    private Boolean isAuthorized;

    /**
     * Construct a new authorizer for the Flume account.
     *
     * @param handler A handler associated with a Flume user account.
     */
    public FlumeJWTAuthorizer(FlumeAccountHandler handler) {
        this.accountHandler = handler;
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
     *
     * @return A completable future which will check if the current tokens are valid, refresh as necessary, and finally
     *         return null.
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
     * refresh token. This is a simple json-ification of the account configuration information.
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
     * Parses a byte request response into the needed token components and saves those to the class instance.
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
        String currentToken = this.accessToken;
        if (currentToken != null) {
            String[] tokenArray = currentToken.split("\\.");

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
    }

    /**
     * Send a request to for a new access and refresh token or refresh the current token.
     *
     * @param content A string content provider with a fully formed json request for new tokens
     *
     * @return A completable future which will be void after the token has been received, parsed, and the values saved
     *         to the class instance
     */
    private CompletableFuture<@Nullable Void> sendTokenRequest(StringContentProvider content) {
        // Create a listener for the response
        FlumeResponseListener<FlumeTokenData> listener = new FlumeResponseListener<>(FlumeTokenData[].class);
        // Create the request
        @Nullable
        Request newRequest = accountHandler.createAsyncRequest("oauth/token", HttpMethod.POST, content);

        if (newRequest == null) {
            CompletableFuture<@Nullable Void> future = new CompletableFuture<>();
            String message = "Created request was null!";
            logger.debug(message);
            future.completeExceptionally(new IOException(message));
            return future;
        }

        // Get the future from the listener
        // We should only get one token envelope back, so we'll just ask for the first one.
        CompletableFuture<@Nullable FlumeTokenData> future = listener.getFutureSingle();

        newRequest.send(listener);

        // Return the future
        return future.thenAccept(firstEnvelope -> {
            logger.trace("Returned token envelope:  {}", firstEnvelope);
            if (firstEnvelope == null) {
                logger.info("Returned token envelope is null");
                voidTokens();
                future.completeExceptionally(new IOException("Returned token envelope is null!"));
            } else {
                parseTokenResponse(firstEnvelope);
            }
        }).exceptionally(e -> {
            logger.info("Exception in the token request future: {}", e.getMessage());
            future.completeExceptionally(new IOException(e.getMessage()));
            voidTokens();
            return null;
        });
    }

    /**
     * Send a request for new tokens.
     *
     * @return A completable future which will be void after the token has been received, parsed, and the values saved
     *         to the class instance
     */
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
     * token using the refresh token.
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

    /**
     * Send a request to refresh the current access token using the refresh token.
     *
     * @return A completable future which will be void after the token has been received, parsed, and the values saved
     *         to the class instance
     */
    private CompletableFuture<@Nullable Void> refreshAccessToken() {
        // First check to see if another thread has updated it
        if (!isExpired(accessTokenExpiresAt)) {
            logger.debug("Access token is still valid; a new one is not needed.");
            return CompletableFuture.completedFuture(null);
        }
        logger.debug("FlumeJWTAuth: Updating expired ACCESS token using refresh token {}", refreshToken);

        return sendTokenRequest(createRefreshTokenRequestContent());
    }

    /**
     * Check the expiration time of the token against the current system time.
     */
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
