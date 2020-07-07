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
 * The {@link FlumeErrorDTO} represents the detailed error message returned
 * for bad API requests.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class FlumeErrorDTO {

    /**
     * On a 400 error, the detailed field contains an array of objects containing
     * the field names that did not validate along with a message for what that
     * field did not validate. On other errors it will be simply an array of human
     * readable messages for what went wrong.
     */
    @SerializedName("field")
    public @Nullable String badField;

    /**
     * A human readable message associated with the error.
     */
    @SerializedName("message")
    public @Nullable String detailedErrorMessage;
}
