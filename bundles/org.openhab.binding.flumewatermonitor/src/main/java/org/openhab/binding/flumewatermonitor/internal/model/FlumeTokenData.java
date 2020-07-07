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
package org.openhab.binding.flumewatermonitor.internal.model;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link FlumeTokenData} represents the envelope containing the JWT access
 * token and some other information returned with get token and refresh token
 * requests.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class FlumeTokenData implements FlumeDataInterface {

    /**
     * The type of token - should always be "bearer".
     */
    @SerializedName("token_type")
    public String tokenType = "bearer";

    /**
     * The access token itself
     */
    @SerializedName("access_token")
    public @Nullable String accessToken;

    /**
     * Number of seconds until the access token expires
     */
    @SerializedName("expires_in")
    public long accessTokenExpires;

    /**
     * Refresh token to be used to request a new access token. A new access token
     * should be requested slightly before it is about to expire
     */
    @SerializedName("refresh_token")
    public @Nullable String refreshToken;
}
