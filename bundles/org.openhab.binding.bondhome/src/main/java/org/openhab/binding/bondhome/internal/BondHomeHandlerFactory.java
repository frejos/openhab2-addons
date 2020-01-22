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
package org.openhab.binding.bondhome.internal;

import static org.openhab.binding.bondhome.internal.BondHomeBindingConstants.*;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.core.thing.type.DynamicStateDescriptionProvider;
import org.eclipse.smarthome.core.types.StateDescription;
import org.openhab.binding.bondhome.internal.discovery.BondDiscoveryService;
import org.openhab.binding.bondhome.internal.handler.BondBridgeHandler;
import org.openhab.binding.bondhome.internal.handler.BondDeviceHandler;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link BondHomeHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.bondhome", service = ThingHandlerFactory.class)
public class BondHomeHandlerFactory extends BaseThingHandlerFactory implements DynamicStateDescriptionProvider {
    private Logger logger = LoggerFactory.getLogger(BondHomeHandlerFactory.class);

    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();
    private final Map<ChannelUID, StateDescription> descriptions = new ConcurrentHashMap<>();

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_BRIDGE_TYPES.contains(thingTypeUID) || SUPPORTED_DEVICE_TYPES.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_BOND_BRIDGE.equals(thingTypeUID)) {
            logger.trace("Creating handler for Bond bridge");
            final BondBridgeHandler handler = new BondBridgeHandler((Bridge) thing);
            registerDeviceDiscoveryService(handler);
            return handler;
        } else if (SUPPORTED_DEVICE_TYPES.contains(thingTypeUID)) {
            logger.trace("Creating handler for Bond device");
            return new BondDeviceHandler(thing, this);
        }

        return null;
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        logger.trace("Removing Bond Handler");
        if (thingHandler instanceof BondBridgeHandler) {
            logger.trace("Removing discovery service tied to Bond Bridge from the service registry.");
            ServiceRegistration<?> serviceReg = this.discoveryServiceRegs.remove(thingHandler.getThing().getUID());
            serviceReg.unregister();
        }
    }

    // Discovery Service
    private synchronized void registerDeviceDiscoveryService(BondBridgeHandler bridgeHandler) {
        logger.trace("Registering a discovery service for the Bond bridge.");
        BondDiscoveryService discoveryService = new BondDiscoveryService(bridgeHandler);
        discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
    }

    /**
     * Set a state description for a channel. This description will be used when
     * preparing the channel state by the framework for presentation. A previous
     * description, if existed, will be replaced.
     *
     * @param channelUID channel UID
     * @param description state description for the channel
     */
    public void setDescription(ChannelUID channelUID, StateDescription description) {
        logger.debug("Adding state description for channel {}", channelUID);
        descriptions.put(channelUID, description);
    }

    /**
     * Clear all registered state descriptions
     */
    void removeAllDescriptions() {
        logger.debug("Removing all state descriptions");
        descriptions.clear();
    }

    /**
     * Removes a state description for a given channel ID
     *
     * @param channelUID channel ID to remove description for
     */
    void removeDescription(ChannelUID channelUID) {
        logger.debug("Removing state description for channel {}", channelUID);
        descriptions.remove(channelUID);
    }

    /**
     * Removes the state descriptions tied to a specific thing
     *
     * @param channelUID channel ID to remove description for
     */
    public void removeDescriptionsForThing(ThingUID uid) {
        for (ChannelUID c : descriptions.keySet()) {
            if (c.getThingUID() == uid) {
                descriptions.remove(c);
            }
        }
    }

    @Override
    public @Nullable StateDescription getStateDescription(Channel channel,
            @Nullable StateDescription originalStateDescription, @Nullable Locale locale) {
        StateDescription description = descriptions.get(channel.getUID());
        logger.trace("Providing state description for channel {}", channel.getUID());
        return description;
    }
}
