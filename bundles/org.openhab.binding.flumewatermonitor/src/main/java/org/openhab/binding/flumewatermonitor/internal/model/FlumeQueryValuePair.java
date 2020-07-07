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

import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link FlumeQueryValuePair} represents the date-time value pairs returned
 * by a data query.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class FlumeQueryValuePair {

    /**
     * The timestame in yyyy-MM-dd HH:mm:ss.SSS format.
     */
    @SerializedName("datetime")
    public @Nullable Date valueDatetime;

    /**
     * The number of gallons of water in question.
     */
    @SerializedName("value")
    public float value;
}
