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
import org.openhab.binding.bondhome.internal.handler.BondBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Thread is responsible maintaining the Bond Push UDP Protocol
 *
 * @author Sara Geleskie Damiano - Initial contribution
 *
 */
@NonNullByDefault
public class BPUPRunnable implements Runnable {

    // private static final int TIMEOUT_TO_DATAGRAM_RECEPTION = 15000; // in milliseconds

    private final Logger logger = LoggerFactory.getLogger(BPUPRunnable.class);

    private DatagramSocket datagramSocket;
    private final BPUPPacketConverter packetConverter = new BPUPPacketConverter();

    // Used for callbacks to handler
    private final BondBridgeHandler bridgeHandler;

    private boolean shutdown;
    private int listeningPort = BOND_BPUP_PORT;
    private long timeOfLastKeepAlivePacket;

    /**
     * Constructor of the receiver runnable thread.
     *
     * @param address  The address of the Bond Bridge
     * @throws SocketException is some problem occurs opening the socket.
     */
    public BPUPRunnable(BondBridgeHandler bridgeHandler) throws SocketException {
        logger.debug("Starting BPUP Runnable...");

        this.bridgeHandler = bridgeHandler;
        this.timeOfLastKeepAlivePacket = -1;

        // Create a socket to listen on the port.
        logger.debug("Opening socket and start listening UDP port: {}", listeningPort);
        DatagramSocket dsocket = new DatagramSocket(null);
        dsocket.setReuseAddress(true);
        dsocket.setBroadcast(true);
        // dsocket.setSoTimeout(TIMEOUT_TO_DATAGRAM_RECEPTION);
        dsocket.bind(new InetSocketAddress(listeningPort));
        this.datagramSocket = dsocket;
        logger.debug("Update Receiver Runnable and socket started with success...");

        this.shutdown = false;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        long timePassedFromLastUpdate = now - timeOfLastKeepAlivePacket;

        // Now loop forever, waiting to receive packets and redirect them to mediator.
        while (!this.shutdown) {

            // Create a buffer to read datagrams into. If a
            // packet is larger than this buffer, the
            // excess will simply be discarded!
            byte[] buffer = new byte[2048];

            // Create a packet to receive data into the buffer
            DatagramPacket inPacket = new DatagramPacket(buffer, buffer.length);

            if (timeOfLastKeepAlivePacket == -1 || timePassedFromLastUpdate >= 60000L) {
                try {
                    byte[] outBuffer = { (byte) '\n' };
                    DatagramPacket outPacket = new DatagramPacket(outBuffer, 1, bridgeHandler.getBridgeAddress(), listeningPort);
                    datagramSocket.send(outPacket);
                    datagramSocket.receive(inPacket);
                    BPUPUpdate response = this.packetConverter.transformResponsePacket(inPacket);
                    if (response.bondId != bridgeHandler.getBridgeId()) {
                        logger.warn("Reponse isn't from expected Bridge!  Expected: {}  Got: {}",
                                bridgeHandler.getBridgeId(), response.bondId);
                    }
                } catch (SocketTimeoutException e) {
                    logger.trace("BPUP Socket timeout");
                } catch (IOException e) {
                    logger.debug("One exception has occurred: {} ", e.getMessage());
                }
            }

            datagramSocketHealthRoutine();

            // Wait to receive a datagram
            try {
                datagramSocket.receive(inPacket);

                logger.debug("Received packet from Bond Bridge");

                // Redirect packet to the mediator
                BPUPUpdate update = this.packetConverter.transformResponsePacket(inPacket);
                if (update != null) {
                    this.mediator.processReceivedPacket(update);
                    logger.debug("Message delivered with success to mediator.");
                } else {
                    logger.debug("No WizLightingResponse was parsed from returned packet");
                }
            } catch (SocketTimeoutException e) {
                logger.trace("BPUP Socket timeout");
            } catch (IOException e) {
                logger.debug("One exception has occurred: {} ", e.getMessage());
            }
        }

        // close the socket
        logger.trace("Ending run loop; closing socket.");
        datagramSocket.close();
    }

    private void datagramSocketHealthRoutine() {
        DatagramSocket datagramSocket = this.datagramSocket;
        if (datagramSocket.isClosed() || !datagramSocket.isConnected()) {
            logger.trace("Datagram Socket is disconnected or has been closed (probably timed out), reconnecting...");
            try {
                // close the socket before trying to reopen
                this.datagramSocket.close();
                logger.trace("Old socket closed.");
                DatagramSocket dsocket = new DatagramSocket(null);
                dsocket.setReuseAddress(true);
                dsocket.setBroadcast(true);
                // dsocket.setSoTimeout(TIMEOUT_TO_DATAGRAM_RECEPTION);
                dsocket.bind(new InetSocketAddress(listeningPort));
                this.datagramSocket = dsocket;
                logger.trace("Datagram Socket reconnected.");
            } catch (SocketException exception) {
                logger.error("Problem creating one new socket on port {}. Error: {}", listeningPort,
                        exception.getLocalizedMessage());
            }
        }
    }

    /**
     * Gracefully shutdown thread. Worst case takes TIMEOUT_TO_DATAGRAM_RECEPTION to
     * shutdown.
     */
    public void shutdown() {
        this.shutdown = true;
    }
}
