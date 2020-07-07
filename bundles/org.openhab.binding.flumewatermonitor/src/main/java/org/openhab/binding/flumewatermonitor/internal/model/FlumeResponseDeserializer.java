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

import java.lang.reflect.Type;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * The {@link RawJsonGsonAdapter} is needed to read the somewhat messy JSON used by the Flume API - several of the
 * fields in the response DTO could be missing, null, empty arrays, or a structure that is variable. Because of the
 * possibility of all the nulls, and the variable structure, even using a TypeToken and a generic class fails to
 * properly parse the subclass. To get around it, for the messy fields I'm having gson leave them as strings at the
 * first pass and then deserializing the string in a second pass within the specific request response where I already
 * know what the structure will be.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class FlumeResponseDeserializer implements JsonDeserializer<FlumeResponseDTO> {

    private final Logger logger = LoggerFactory.getLogger(FlumeResponseDeserializer.class);

    @Override
    public FlumeResponseDTO deserialize(@Nullable JsonElement json, @Nullable Type typeOfT,
            @Nullable JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        FlumeResponseDTO out = new FlumeResponseDTO();

        JsonElement element = jsonObject.get("success");
        if (element != null) {
            out.setSuccess(element.getAsBoolean());
            logger.trace("success deserailized as {}", element.getAsBoolean());
        } else {
            out.setSuccess(false);
            logger.trace("success field missing from response");
        }

        element = jsonObject.get("code");
        if (element != null) {
            out.setHttpResponseCode(element.getAsInt());
            logger.trace("http response code deserailized as {}", element.getAsInt());
        } else {
            out.setHttpResponseCode(503);
            logger.trace("code field missing from response");
        }

        element = jsonObject.get("message");
        if (element != null) {
            out.setMessage(element.getAsString());
            logger.trace("message deserailized as {}", element.getAsString());
        } else {
            out.setMessage(null);
            logger.trace("message field missing from response");
        }

        element = jsonObject.get("http_message");
        if (element != null) {
            out.setHttpMessage(element.getAsString());
            logger.trace("http message deserailized as {}", element.getAsString());
        } else {
            out.setHttpMessage(null);
            logger.trace("http_message field missing from response");
        }

        element = jsonObject.get("detailed");
        if (element != null) {
            if (element.isJsonObject()) {
                out.setDetailedErrorAsString(element.getAsJsonObject().toString());
                logger.trace("detailed error message object left as the string {}", element.getAsJsonObject());
            } else if (element.isJsonArray()) {
                out.setDetailedErrorAsString(element.getAsJsonArray().toString());
                logger.trace("detailed error message array left as the string {}", element.getAsJsonArray());
            } else if (element.isJsonPrimitive()) {
                out.setDetailedErrorAsString(element.getAsString());
                logger.trace("detailed error message deserialized as the string {}", element.getAsString());
            } else {
                out.setDetailedErrorAsString(null);
                logger.trace("detailed error field missing from response");
            }
        } else {
            out.setDetailedErrorAsString(null);
        }

        element = jsonObject.get("data");
        if (element != null) {
            if (element.isJsonObject()) {
                out.setDataAsString(element.getAsJsonObject().toString());
                logger.warn("Unexpected JSON object in the data portion of the DTO.  Expected an array!");
                logger.trace("data object left as the string {}", element.getAsJsonObject());
                out.setDataAsString(element.getAsJsonObject().toString());
            } else if (element.isJsonArray()) {
                out.setDataAsString(element.getAsJsonArray().toString());
                logger.trace("data array left as the string {}", element.getAsJsonArray());
            } else if (element.isJsonPrimitive()) {
                out.setDataAsString(element.getAsString());
                logger.trace("data deserialized as the string {}", element.getAsString());
            } else {
                out.setDataAsString(null);
                logger.trace("data field missing from response");
            }
        } else {
            out.setDataAsString(null);
        }

        element = jsonObject.get("count");
        if (element != null) {
            out.setCount(element.getAsInt());
            logger.trace("count deserailized as {}", element.getAsInt());
        } else {
            out.setCount(0);
            logger.trace("count field missing from response");
        }

        element = jsonObject.get("pagination");
        if (element != null) {
            if (element.isJsonObject()) {
                out.setPagination(context.deserialize(jsonObject.getAsJsonObject(), FlumePaginationDTO.class));
                logger.trace("pagination deserailized as {}", element.getAsJsonObject());
            } else {
                out.setPagination(null);
                logger.trace("pagination field missing from response");
            }
        } else {
            out.setPagination(null);
        }

        return out;
    }
}
