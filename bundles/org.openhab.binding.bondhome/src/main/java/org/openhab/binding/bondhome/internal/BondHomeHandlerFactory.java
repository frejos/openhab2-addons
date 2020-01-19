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
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
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
public class BondHomeHandlerFactory extends BaseThingHandlerFactory {
    private Logger logger = LoggerFactory.getLogger(BondHomeHandlerFactory.class);
    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

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
            return new BondDeviceHandler(thing);
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

    private synchronized void registerDeviceDiscoveryService(BondBridgeHandler bridgeHandler) {
        logger.trace("Registering a discovery service for the Bond bridge.");
        BondDiscoveryService discoveryService = new BondDiscoveryService(bridgeHandler);
        discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
    }

}
