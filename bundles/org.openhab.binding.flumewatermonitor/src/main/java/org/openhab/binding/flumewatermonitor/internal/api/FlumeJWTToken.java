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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link FlumeJWTToken} represents the Flume API java web token.
 *
 * @author Sara Geleskie Damiano - Initial Contribution
 */
@NonNullByDefault
public class FlumeJWTToken {

    /**
     * The the Flume user ID
     */
    @SerializedName("user_id")
    public long flumeUserId;

    /**
     * The type of token, should be "USER"
     */
    public String type = "USER";
    /**
     * The scope of the token, should be: "scope": [ "read:personal",
     * "update:personal", "query:personal" ]
     */
    public String[] scope = { "read:personal", "update:personal", "query:personal" };

    /**
     * Contains the date the token was issued in epoch time.
     */
    public long iat;

    /**
     * Contains the date the token expires in epoch time.
     */
    public long exp;

    /**
     * The subject of the token
     */
    public @Nullable String sub;
}
