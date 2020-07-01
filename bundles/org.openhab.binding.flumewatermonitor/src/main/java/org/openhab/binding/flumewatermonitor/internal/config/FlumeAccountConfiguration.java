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
package org.openhab.binding.flumewatermonitor.internal.config;

import static org.openhab.binding.flumewatermonitor.internal.FlumeWaterMonitorBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link FlumeAccountConfiguration} class contains fields mapping binding
 * configuration parameters.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class FlumeAccountConfiguration {

    /**
     * Flume API access grant type - will always be "password".
     */
    @SerializedName("grant_type")
    public String grantType = FLUME_TOKEN_GRANT_TYPE;

    /**
     * Flume account user name.
     */
    @SerializedName("username")
    public String flumeUser = MISSING_USER_NAME;

    /**
     * Flume account password.
     */
    @SerializedName("password")
    public String flumePass = MISSING_PASSWORD;

    /**
     * Flume API access client ID.
     */
    @SerializedName("client_id")
    public String clientId = MISSING_CLIENT_ID;

    /**
     * Flume API access client secret.
     */
    @SerializedName("client_secret")
    public String clientSecret = MISSING_CLIENT_SECRET;
}
