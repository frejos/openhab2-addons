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
package org.openhab.binding.flumewatermonitor.internal;

import static org.openhab.binding.flumewatermonitor.internal.FlumeWaterMonitorBindingConstants.*;

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
import org.openhab.binding.flumewatermonitor.internal.discovery.FlumeDiscoveryService;
import org.openhab.binding.flumewatermonitor.internal.handler.FlumeAccountHandler;
import org.openhab.binding.flumewatermonitor.internal.handler.FlumeSensorHandler;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link FlumeHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.flumewatermonitor", service = ThingHandlerFactory.class)
public class FlumeHandlerFactory extends BaseThingHandlerFactory {
    private Logger logger = LoggerFactory.getLogger(FlumeHandlerFactory.class);

    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_BRIDGE_TYPES.contains(thingTypeUID) || SUPPORTED_DEVICE_TYPES.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        logger.trace("Request to Flume Handler factory to create a new thing");
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        logger.trace("New thing has UID {} and type UID {}", thing.getUID(), thing.getThingTypeUID());

        if (THING_TYPE_FLUME_ACCOUNT.equals(thingTypeUID)) {
            logger.info(
                    "\n\n*************************************************\nFlume Water Monitor, binding version: {}\n*************************************************\n",
                    CURRENT_BINDING_VERSION);

            logger.debug("Creating handler for a Flume Tech Account with type UID {}", thingTypeUID);
            final FlumeAccountHandler handler = new FlumeAccountHandler((Bridge) thing);
            registerDeviceDiscoveryService(handler);
            return handler;
        } else if (THING_TYPE_FLUME_SENSOR.equals(thingTypeUID)) {
            logger.debug("Creating handler for Flume device with type UID {}", thingTypeUID);
            return new FlumeSensorHandler(thing);
        } else {
            logger.trace("Thing type is not supported!");
            return null;
        }
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        logger.trace("Removing Flume Handler");
        if (thingHandler instanceof FlumeAccountHandler) {
            logger.trace("Removing discovery service tied to Flume account from the service registry.");
            ServiceRegistration<?> serviceReg = this.discoveryServiceRegs.remove(thingHandler.getThing().getUID());
            serviceReg.unregister();
        }
    }

    // Discovery Service
    private synchronized void registerDeviceDiscoveryService(FlumeAccountHandler bridgeHandler) {
        logger.trace("Registering a discovery service for the Flume account.");
        FlumeDiscoveryService discoveryService = new FlumeDiscoveryService(bridgeHandler);
        discoveryServiceRegs.put(bridgeHandler.getThing().getUID(),
                bundleContext.registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<>()));
    }
}
