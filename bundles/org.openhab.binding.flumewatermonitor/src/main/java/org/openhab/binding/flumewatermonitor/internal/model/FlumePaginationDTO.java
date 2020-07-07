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
 * The {@link FlumePaginationDTO} object contains convenient links to get the
 * next and prev data if a call to a GET request exceeds the limit in the query
 * parameter. For example if the call is limited to 300 results in the data
 * objects. But there are 500 total results. The first call would return the
 * first 300 and a link to get the next 200.
 *
 * Caveat: If the result set is say 20 results with an offset of 5, the previous
 * link will contain information to get the first 5 results even if the count is
 * specified at 20. This prevents overlapping when cursoring through the data.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class FlumePaginationDTO {

    /**
     * On a 400 error, the detailed field contains an array of objects containing
     * the field names that did not validate along with a message for what that
     * field did not validate. On other errors it will be simply an array of human
     * readable messages for what went wrong.
     */
    @SerializedName("next")
    public @Nullable String nextResultUrl;

    /**
     * A human readable message associated with the error.
     */
    @SerializedName("prev")
    public @Nullable String previousResultUrl;
}
