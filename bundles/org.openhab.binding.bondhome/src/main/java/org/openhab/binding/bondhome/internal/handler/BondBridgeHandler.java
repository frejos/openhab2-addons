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
package org.openhab.binding.bondhome.internal.handler;

import static org.openhab.binding.bondhome.internal.BondHomeBindingConstants.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.bondhome.internal.config.BondBridgeConfiguration;;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link BondBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class BondBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(BondBridgeHandler.class);

    private @Nullable BondBridgeConfiguration config;
    private @NonNullByDefault({}) DatagramSocket socket;
    private final byte[] buffer = new byte[1024];
    private final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    private @Nullable ScheduledFuture<?> running;

    public BondBridgeHandler(Bridge bridge) {
        super(bridge);
        logger.debug("Creating a BondBridgeHandler for thing '{}'", getThing().getUID());
    }

    /**
     * Returns the bridge serial number/id associated with the handler
     *
     * @param packet the {@link DatagramPacket}
     * @return the Bridge ID
     */
    public String getBridgeId() {
        config = getConfigAs(BondBridgeConfiguration.class);
        return config.bondId;
    }

    /**
     * Returns the bridge address
     *
     * @return the {@link InetAddress}
     */
    public InetAddress getBridgeAddress() {
        config = getConfigAs(BondBridgeConfiguration.class);
        return InetAddress.getByName(config.ipAddress);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Not needed, all commands are handled in the {@link BondDeviceHandler}
        }
    }

    @Override
    public void initialize() {
        // logger.debug("Start initializing!");
        config = getConfigAs(BondBridgeConfiguration.class);

        if (!config.ipAddress.isEmpty()) {
            try {
                logger.trace("Attempting to get IP address for Bond Bridge SN {}", config.bondId);
                InetAddress ia = InetAddress.getByName(config.bondId);
                String ip = ia.getHostAddress();
                Configuration c = editConfiguration();
                c.put(CONFIG_IP_ADDRESS, ip);
                updateConfiguration(c);
            } catch (UnknownHostException ignored) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Unable to get an IP Address for Bond Bridge");
                return;
            }
        }

        startConnectAndKeepAlive();
    }

    /**
     * Creates a discovery object and the send queue. The initial IP address may be
     * null or is not matching with the real IP address of the bridge. The discovery
     * class will send a broadcast packet to find the bridge with the respective
     * bridge ID. The response in bridgeDetected() may lead to a recreation of the
     * send queue object.
     *
     * The keep alive timer that is also setup here, will send keep alive packets
     * periodically. If the bridge doesn't respond anymore (e.g. DHCP IP change),
     * the initial session handshake starts all over again.
     */
    private void startConnectAndKeepAlive() {

        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.setBroadcast(true);
            socket.bind(new InetSocketAddress(BOND_BPUP_PORT));
        } catch (SocketException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
        }

        // NOTE:  The Bond push UDP protocol specifies that keep-alive messages should be sent every 60s
        running = scheduler.scheduleWithFixedDelay(this::receive, 0, 60, TimeUnit.SECONDS);
    }

    private synchronized void stopKeepAlive() {
        if (running != null) {
            running.cancel(false);
            running = null;
        }
        if (socket != null) {
            socket.close();
        }
    }

    @Override
    public void dispose() {
        stopKeepAlive();
    }

    private void receive() {
        try {
            final int attempts = 5;
            int timeoutsCounter = 0;
            for (timeoutsCounter = 1; timeoutsCounter <= attempts; ++timeoutsCounter) {
                try {
                    packet.setLength(buffer.length);
                    socket.setSoTimeout(500 * timeoutsCounter);
                    socket.send(discoverPacketV3);
                    socket.receive(packet);
                } catch (SocketTimeoutException e) {
                    continue;
                }
                // We expect packets with a format like this: 10.1.1.27,ACCF23F57AD4,HF-LPB100
                final String received = new String(packet.getData());


                updateStatus(ThingStatus.ONLINE);
                break;
            }
            if (timeoutsCounter > attempts) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "Bridge did not respond!");
            }
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

}
