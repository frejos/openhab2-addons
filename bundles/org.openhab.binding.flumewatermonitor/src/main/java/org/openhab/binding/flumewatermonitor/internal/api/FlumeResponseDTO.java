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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link FlumeResponseDTO} represents the response envelope for all requests to the Flume API.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class FlumeResponseDTO<T> {

    private final Logger logger = LoggerFactory.getLogger(FlumeResponseDTO.class);

    /**
     * The success field returns whether the operation was successful or not.
     */
    @SerializedName("success")
    public boolean success = false;

    /**
     * The status_code represents the HTTP status_code from the operation.
     * This is present in case the user is accessing the API not using HTTP.
     *
     * The possible status codes are the following:
     * - 200 OK - Successful operation. Is returned with a message body.
     * - 201 Created - Successful creation of an object.
     * - 401 Unauthorized - User does not have access to the API
     * - 403 Forbidden - User does not have the required permission to view specific data.
     * - 400 Bad Request - Malformatted request sent to the server.
     * - 404 Not Found - Request/Resource not found.
     * - 409 Conflict -- Returned when trying to add something that already exists
     * - 500 Internal Server Error - Issue detected on the server.
     * - 503 Service Unavailable - JWT is invalid or malformed.
     */
    @SerializedName("code")
    public int httpResponseCode = 400;

    /**
     * Check if the request was successful and throw exceptions otherwise
     *
     * @throws AuthorizationException
     * @throws IOException
     * @throws NotFoundException
     */
    public void checkForExceptions() throws AuthorizationException, IOException, NotFoundException {
        if (httpResponseCode == 401 || httpResponseCode == 403 || httpResponseCode == 503) {
            logger.error("Authorization problem! {}: {}", httpMessage, message);
            throw new AuthorizationException(httpMessage + ": " + message);
        } else if (httpResponseCode == 400) {
            logger.warn("Issue with request.  {}: {}", httpMessage, message);
            throw new IOException(httpMessage + ": " + message);
        } else if (httpResponseCode == 404) {
            logger.error("Bad request!  {}: {}", httpMessage, message);
            throw new NotFoundException(httpMessage + ": " + message);
        } else if (!success) {
            logger.warn("Request failed.  {}: {}", httpMessage, message);
            throw new IOException(httpMessage + ": " + message);
        }
    }

    /**
     * A message associated with the response - the string representation of the API
     * HTTP response code.
     */
    @SerializedName("message")
    public @Nullable String message;

    /**
     * The status_message is the name of the status_code.
     */
    @SerializedName("http_message")
    public @Nullable String httpMessage;

    /**
     * A field with variable structure contianing details about the error when one occurs.
     *
     * Per Flume documentation:
     * > On a 400 error, the detailed field contains an array of objects containing
     * > the field names that did not validate along with a message for what that
     * > field did not validate. On other errors it will be simply an array of human
     * > readable messages for what went wrong. This will always be null on success.
     *
     * Unfortunately, the structure of this field is inconsistent depending on the error.
     * Some errors give a text response ( "detailed": ["detials"] ) while others give the
     * expected json ( "detailed": [{"field": "badField", "message": "badMessage"}] ).
     * Because I can't know what kind of detailed response this will give, I will set
     * the field as transient to have it ignored in all serializations.
     */
    @SerializedName("detailed")
    public transient String @Nullable [] detailedError;

    /**
     * The data portion of the response. This may either be an empty array or a simple null.
     */
    @SerializedName("data")
    public @Nullable T @Nullable [] dataResults;

    /**
     * The count field contains the total amount of records in existence for the
     * route.
     */
    @SerializedName("count")
    public int count;

    /**
     * The pagination object contains convenient links to get the next and prev data
     * if a call to a GET request exceeds the limit in the query parameter. For
     * example if the call is limited to 300 results in the data objects. But there
     * are 500 total results. The first call would return the first 300 and a link
     * to get the next 200.
     */
    @SerializedName("pagination")
    public @Nullable FlumePaginationDTO pagination;
}
