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

import static org.openhab.binding.bondhome.internal.BondHomeBindingConstants.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.bondhome.internal.handler.BondBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/**
 * This Thread is responsible maintaining the Bond Push UDP Protocol
 *
 * @author Sara Geleskie Damiano - Initial contribution
 *
 */
@NonNullByDefault
public class BPUPListener extends Thread {

    private static final int SOCKET_TIMEOUT_MILLISECONDS = 3000; // in milliseconds

    private final Logger logger = LoggerFactory.getLogger(BPUPListener.class);

    // To parse the JSON responses
    private Gson gsonBuilder;

    // Used for callbacks to handler
    private final BondBridgeHandler bridgeHandler;

    // UDP socket used to receive status events
    private @Nullable DatagramSocket socket;

    public @Nullable String lastRequestId;
    private long timeOfLastKeepAlivePacket;
    private Boolean shutdown;

    /**
     * Constructor of the receiver runnable thread.
     *
     * @param address The address of the Bond Bridge
     * @throws SocketException is some problem occurs opening the socket.
     */
    public BPUPListener(BondBridgeHandler bridgeHandler) {
        logger.debug("Starting BPUP Listener...");

        this.bridgeHandler = bridgeHandler;
        this.timeOfLastKeepAlivePacket = -1;

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.excludeFieldsWithoutExposeAnnotation();
        Gson gson = gsonBuilder.create();
        this.gsonBuilder = gson;
        this.shutdown = false;
    }

    /**
     * Send keep-alive as necessary and listen for push messages
     */
    @Override
    public void run() {
        receivePackets();
    }

    /**
     * Gracefully shutdown thread. Worst case takes TIMEOUT_TO_DATAGRAM_RECEPTION to
     * shutdown.
     */
    public void shutdown() {
        this.shutdown = true;
        DatagramSocket s = this.socket;
        if (s != null) {
            s.close();
            logger.debug("Listener closed socket");
            this.socket = null;
        }
    }

    private void sendBPUPKeepAlive() {
        // Create a buffer and packet for the response
        byte[] buffer = new byte[256];
        DatagramPacket inPacket = new DatagramPacket(buffer, buffer.length);

        DatagramSocket sKA = this.socket;
        if (sKA != null) {
            logger.trace("Sending keep-alive request ('\\n')");
            try {
                byte[] outBuffer = { (byte) '\n' };
                InetAddress inetAddress = InetAddress.getByName(bridgeHandler.getBridgeIpAddress());
                DatagramPacket outPacket = new DatagramPacket(outBuffer, 1, inetAddress, BOND_BPUP_PORT);
                sKA.send(outPacket);
                sKA.receive(inPacket);
                BPUPUpdate response = transformUpdatePacket(inPacket);
                if (response != null) {
                    if (!response.bondId.equalsIgnoreCase(bridgeHandler.getBridgeId())) {
                        logger.warn("Reponse isn't from expected Bridge!  Expected: {}  Got: {}",
                                bridgeHandler.getBridgeId(), response.bondId);
                    }
                }
            } catch (SocketTimeoutException e) {
                logger.trace("BPUP Socket timeout");
            } catch (IOException e) {
                logger.debug("One exception has occurred: {} ", e.getMessage());
            }
        }
    }

    private synchronized void receivePackets() {

        try {
            DatagramSocket s = new DatagramSocket(null);
            s.setSoTimeout(SOCKET_TIMEOUT_MILLISECONDS);
            s.setReuseAddress(true);
            s.setBroadcast(true);
            InetSocketAddress address = new InetSocketAddress(BOND_BPUP_PORT);
            s.bind(address);
            socket = s;
            logger.debug("Listener created UDP socket on port {} with timeout {}", BOND_BPUP_PORT,
                    SOCKET_TIMEOUT_MILLISECONDS);
        } catch (SocketException e) {
            logger.debug("Listener got SocketException: {}", e.getMessage(), e);
            socket = null;
            return;
        }

        // Create a buffer and packet for the response
        byte[] buffer = new byte[256];
        DatagramPacket inPacket = new DatagramPacket(buffer, buffer.length);

        while (!this.shutdown) {

            // Check if we're due to send something to keep the connection
            long now = System.currentTimeMillis();
            long timePassedFromLastKeepAlive = now - timeOfLastKeepAlivePacket;
            // logger.trace("Time since last keep alive: {}", timePassedFromLastKeepAlive);
            if (timeOfLastKeepAlivePacket == -1 || timePassedFromLastKeepAlive >= 60000L) {
                sendBPUPKeepAlive();
                timeOfLastKeepAlivePacket = now;
            }

            DatagramSocket sock = this.socket;
            if (sock != null) {
                if (sock.isClosed() || !sock.isConnected()) {
                    // logger.trace(
                    // "Datagram Socket is disconnected or has been closed (probably timed out), reconnecting...");
                    try {
                        // close the socket before trying to reopen
                        sock.close();
                        // logger.trace("Old socket closed.");
                        DatagramSocket s = new DatagramSocket(null);
                        s.setReuseAddress(true);
                        s.setBroadcast(true);
                        s.setSoTimeout(SOCKET_TIMEOUT_MILLISECONDS);
                        s.bind(new InetSocketAddress(BOND_BPUP_PORT));
                        this.socket = s;
                        sock = s;
                        // logger.trace("Datagram Socket reconnected.");
                    } catch (SocketException exception) {
                        logger.error("Problem creating one new socket on port {}. Error: {}", BOND_BPUP_PORT,
                                exception.getLocalizedMessage());
                    }
                }
            }

            sock = this.socket;
            if (sock != null) {
                try {
                    sock.receive(inPacket);
                    processPacket(inPacket);
                } catch (SocketTimeoutException e) {
                    // Nothing to do on socket timeout
                } catch (IOException e) {
                    logger.debug("Listener got IOException waiting for datagram: {}", e.getMessage());
                    // this.socket = null;
                }
            }
        }
        logger.debug("Listener exiting");
    }

    private void processPacket(DatagramPacket packet) {
        logger.trace("Got datagram of length {} from {}", packet.getLength(), packet.getAddress().getHostAddress());

        BPUPUpdate update = transformUpdatePacket(packet);
        if (update != null) {
            if (!update.bondId.equalsIgnoreCase(bridgeHandler.getBridgeId())) {
                logger.warn("Response isn't from expected Bridge!  Expected: {}  Got: {}", bridgeHandler.getBridgeId(),
                        update.bondId);
            }

            // Check for duplicate packet
            if (isDuplicate(update)) {
                logger.trace("Dropping duplicate packet");
                return;
            }

            // Send the update the the bridge for it to pass on to the devices
            bridgeHandler.forwardUpdateToThing(update);
        }
    }

    /**
     * Method that transforms {@link DatagramPacket} to a {@link BPUPUpdate} Object
     *
     * @param packet the {@link DatagramPacket}
     * @return the {@link BPUPUpdate}
     */
    public @Nullable BPUPUpdate transformUpdatePacket(final DatagramPacket packet) {
        String responseJson = new String(packet.getData(), 0, packet.getLength());
        logger.debug("Response from {} -> {}", packet.getAddress().getHostAddress(), responseJson);

        @Nullable
        BPUPUpdate response = null;
        try {
            response = this.gsonBuilder.fromJson(responseJson, BPUPUpdate.class);
            // logger.trace("JSON Deserialized!");
        } catch (JsonParseException e) {
            logger.error("Error parsing json! {}", e.getMessage());
        }
        return response;
    }

    private boolean isDuplicate(BPUPUpdate update) {
        boolean packetIsDuplicate = false;
        String newReqestId = update.requestId;
        String lastRequestId = this.lastRequestId;
        if (lastRequestId != null && newReqestId != null) {
            if (lastRequestId.equalsIgnoreCase(newReqestId)) {
                packetIsDuplicate = true;
            }
        }
        // Remember this packet for duplicate check
        lastRequestId = newReqestId;
        return packetIsDuplicate;
    }

    // private void datagramSocketHealthRoutine() {
    // DatagramSocket datagramSocket = this.socket;
    // if (datagramSocket != null) {
    // if (datagramSocket.isClosed() || !datagramSocket.isConnected()) {
    // logger.trace(
    // "Datagram Socket is disconnected or has been closed (probably timed out), reconnecting...");
    // try {
    // // close the socket before trying to reopen
    // datagramSocket.close();
    // logger.trace("Old socket closed.");
    // DatagramSocket s = new DatagramSocket(null);
    // s.setReuseAddress(true);
    // s.setBroadcast(true);
    // s.setSoTimeout(SOCKET_TIMEOUT_MILLISECONDS);
    // s.bind(new InetSocketAddress(BOND_BPUP_PORT));
    // this.socket = s;
    // logger.trace("Datagram Socket reconnected.");
    // } catch (SocketException exception) {
    // logger.error("Problem creating one new socket on port {}. Error: {}", BOND_BPUP_PORT,
    // exception.getLocalizedMessage());
    // }
    // }
    // }
    // }
}
