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

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.bondhome.internal.handler.BondBridgeHandler;
import org.openhab.binding.bondhome.internal.handler.BondDeviceHandler;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link BondHomeHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.bondhome", service = ThingHandlerFactory.class)
public class BondHomeHandlerFactory extends BaseThingHandlerFactory {


    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_BRIDGE_TYPES.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_BOND_BRIDGE.equals(thingTypeUID)) {
            return new BondBridgeHandler((Bridge) thing);
        } else if (SUPPORTED_DEVICE_TYPES.contains(thingTypeUID)) {
            return new BondDeviceHandler(thing);
        }

        return null;
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof BondBridgeHandler) {
            ServiceRegistration<?> serviceReg = this.discoveryServiceRegs.remove(thingHandler.getThing().getUID());
            if (serviceReg != null) {
                serviceReg.unregister();
            }
        }
    }

    private synchronized void registerDeviceDiscoveryService(BondBridgeHandler wemoBridgeHandler,
            WemoHttpCall wemoHttpCaller) {
        WemoLinkDiscoveryService discoveryService = new WemoLinkDiscoveryService(wemoBridgeHandler, upnpIOService,
                wemoHttpCaller);
        this.discoveryServiceRegs.put(wemoBridgeHandler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
    }

}
