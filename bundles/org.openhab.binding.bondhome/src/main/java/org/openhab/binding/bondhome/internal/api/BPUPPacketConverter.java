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
package org.openhab.binding.bondhome.internal.api;

import java.net.DatagramPacket;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.bondhome.internal.api.BPUPUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/**
 * Transforms the datagram packet to request/response
 *
 * @author Sriram Balakrishnan - Initial contribution
 *
 */
@NonNullByDefault
public class BPUPPacketConverter {

    private final Logger logger = LoggerFactory.getLogger(BPUPPacketConverter.class);

    private Gson gsonBuilder;

    /**
     * Default constructor of the packet converter.
     */
    public BPUPPacketConverter() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.excludeFieldsWithoutExposeAnnotation();
        Gson gson = gsonBuilder.create();
        this.gsonBuilder = gson;
    }

    /**
     * Method that transforms {@link DatagramPacket} to a {@link BPUPUpdate}
     * Object
     *
     * @param packet the {@link DatagramPacket}
     * @return the {@link BPUPUpdate}
     */
    public @Nullable BPUPUpdate transformResponsePacket(final DatagramPacket packet) {
        String responseJson = new String(packet.getData(), 0, packet.getLength());
        logger.debug("Response from {} -> {}", packet.getAddress().getHostAddress(), responseJson);

        @Nullable
        BPUPUpdate response = null;
        try {
            response = this.gsonBuilder.fromJson(responseJson, BPUPUpdate.class);
            logger.trace("JSON Deserialized!");
        } catch (JsonParseException e) {
            logger.error("Error parsing json! {}", e.getMessage());
        }
        return response;
    }
}
