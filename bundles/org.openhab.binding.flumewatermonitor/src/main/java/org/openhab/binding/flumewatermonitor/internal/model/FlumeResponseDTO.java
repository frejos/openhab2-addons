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

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.flumewatermonitor.internal.exceptions.AuthorizationException;
import org.openhab.binding.flumewatermonitor.internal.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

/**
 * The {@link FlumeResponseDTO} represents the response envelope for all requests to the Flume API.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@JsonAdapter(FlumeResponseDeserializer.class)
@NonNullByDefault
public class FlumeResponseDTO {

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
     * Check if the request was successful based on the internal success code and throw exceptions otherwise.
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
     * Because I can't know what kind of detailed response this will give, I am deserializing
     * it as a raw String containing the json with the message (if any).
     */
    @SerializedName("detailed")
    public @Nullable String detailedErrorAsString;

    /**
     * The data portion of the response. If there is an error, this may be missing, a simple null ( "data": null) or an
     * empty array ( "data": [] ). When present, the structure of the data field depends on the type of request made.
     *
     * While I would prefer to make the response DTO a generic class and make use of the GSON TypeToken functionality to
     * deserialize the DTO, because it's all nullable the TypeToken can't figure out what to do and hangs. To get around
     * this, I'm leaving this field as a string that will have to be deserialized in a second step with the proper class
     * type.
     *
     * // public class FlumeResponseDTO<T> {
     * // public T [] dataResults;
     */
    @SerializedName("data")
    public @Nullable String dataAsString;

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
     *
     * @note This should always be null for us because we are never requesting many fields.
     */
    @SerializedName("pagination")
    public @Nullable FlumePaginationDTO pagination;

    /**
     * @return the success
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @param success the success to set
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * @return the httpResponseCode
     */
    public int getHttpResponseCode() {
        return httpResponseCode;
    }

    /**
     * @param httpResponseCode the httpResponseCode to set
     */
    public void setHttpResponseCode(int httpResponseCode) {
        this.httpResponseCode = httpResponseCode;
    }

    /**
     * @return the message
     */
    public @Nullable String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(@Nullable String message) {
        this.message = message;
    }

    /**
     * @return the httpMessage
     */
    public @Nullable String getHttpMessage() {
        return httpMessage;
    }

    /**
     * @param httpMessage the httpMessage to set
     */
    public void setHttpMessage(@Nullable String httpMessage) {
        this.httpMessage = httpMessage;
    }

    /**
     * @return the detailedErrorAsString
     */
    public @Nullable String getDetailedErrorAsString() {
        return detailedErrorAsString;
    }

    /**
     * @param detailedErrorAsString the detailedErrorAsString to set
     */
    public void setDetailedErrorAsString(@Nullable String detailedErrorAsString) {
        this.detailedErrorAsString = detailedErrorAsString;
    }

    /**
     * @return the dataAsString
     */
    public @Nullable String getDataAsString() {
        return dataAsString;
    }

    /**
     * @param dataAsString the dataAsString to set
     */
    public void setDataAsString(@Nullable String dataAsString) {
        this.dataAsString = dataAsString;
    }

    /**
     * @return the count
     */
    public int getCount() {
        return count;
    }

    /**
     * @param count the count to set
     */
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * @return the pagination
     */
    public @Nullable FlumePaginationDTO getPagination() {
        return pagination;
    }

    /**
     * @param pagination the pagination to set
     */
    public void setPagination(@Nullable FlumePaginationDTO pagination) {
        this.pagination = pagination;
    }
}
