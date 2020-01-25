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

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.thing.type.ThingTypeBuilder;
import org.eclipse.smarthome.core.thing.type.ThingTypeRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link BondHomeTypeProvider} creates thing types, channel and channel group types, and full-fledged thing types.
 * This allows channel groups to be created dynamically based on actions available to the different Bond Home devices.
 * A huge portion of this is copied wholesale from the MQTT Generic binding
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.bondhome.typeprovider", service = { ThingTypeProvider.class,
        ChannelTypeProvider.class, ChannelGroupTypeProvider.class, BondHomeTypeProvider.class })
public class BondHomeTypeProvider implements ThingTypeProvider, ChannelGroupTypeProvider, ChannelTypeProvider {
    private Logger logger = LoggerFactory.getLogger(BondHomeTypeProvider.class);
    private final ThingTypeRegistry typeRegistry;

    private final Map<ChannelTypeUID, ChannelType> types = new HashMap<>();
    private final Map<ChannelGroupTypeUID, ChannelGroupType> groups = new HashMap<>();
    private final Map<ThingTypeUID, ThingType> things = new HashMap<>();

    @Activate
    public BondHomeTypeProvider(@Reference ThingTypeRegistry typeRegistry) {
        super();
        this.typeRegistry = typeRegistry;
    }

    // Channel Type Provider
    @Override
    public Collection<ChannelType> getChannelTypes(@Nullable Locale locale) {
        return types.values();
    }

    @Override
    public @Nullable ChannelType getChannelType(ChannelTypeUID channelTypeUID, @Nullable Locale locale) {
        return types.get(channelTypeUID);
    }

    public void setChannelType(ChannelTypeUID uid, ChannelType type) {
        types.put(uid, type);
    }

    public void removeChannelType(ChannelTypeUID uid) {
        types.remove(uid);
    }

    public void removeChannelTypesForThing(ThingUID uid) {
        for (Map.Entry<ChannelTypeUID, ChannelType> entry : types.entrySet()) {
            if (entry.getKey().getAsString().startsWith(uid.getAsString())) {
                types.remove(entry.getKey());
                logger.trace("Removing channel type {}", entry.getKey().getAsString());
            }
        }
    }

    // Channel Group Type Provider

    @Override
    public @Nullable ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID,
            @Nullable Locale locale) {
        return groups.get(channelGroupTypeUID);
    }

    @Override
    public Collection<ChannelGroupType> getChannelGroupTypes(@Nullable Locale locale) {
        return groups.values();
    }

    public void removeChannelGroupType(ChannelGroupTypeUID uid) {
        groups.remove(uid);
    }

    public void setChannelGroupType(ChannelGroupTypeUID uid, ChannelGroupType type) {
        groups.put(uid, type);
    }

    public void removeChannelGroupTypesForThing(ThingUID uid) {
        for (Map.Entry<ChannelGroupTypeUID, ChannelGroupType> entry : groups.entrySet()) {
            if (entry.getKey().getAsString().startsWith(uid.getAsString())) {
                groups.remove(entry.getKey());
                logger.trace("Removing channel group type {}", entry.getKey().getAsString());
            }
        }
    }

    // Thing Type Provider
    @Override
    public Collection<ThingType> getThingTypes(@Nullable Locale locale) {
        return things.values();
    }

    public Set<ThingTypeUID> getThingTypeUIDs() {
        return things.keySet();
    }

    @Override
    public @Nullable ThingType getThingType(ThingTypeUID thingTypeUID, @Nullable Locale locale) {
        return things.get(thingTypeUID);
    }

    public void removeThingType(ThingTypeUID uid) {
        things.remove(uid);
    }

    public void setThingType(ThingTypeUID uid, ThingType type) {
        things.put(uid, type);
    }

    public void setThingTypeIfAbsent(ThingTypeUID uid, ThingType type) {
        things.putIfAbsent(uid, type);
    }

    public ThingTypeBuilder derive(ThingTypeUID newTypeId, ThingTypeUID baseTypeId) {
        ThingType baseType = typeRegistry.getThingType(baseTypeId);

        ThingTypeBuilder result = ThingTypeBuilder.instance(newTypeId, baseType.getLabel())
                .withChannelGroupDefinitions(baseType.getChannelGroupDefinitions())
                .withChannelDefinitions(baseType.getChannelDefinitions())
                .withExtensibleChannelTypeIds(baseType.getExtensibleChannelTypeIds())
                .withSupportedBridgeTypeUIDs(baseType.getSupportedBridgeTypeUIDs())
                .withProperties(baseType.getProperties()).isListed(false);

        String representationProperty = baseType.getRepresentationProperty();
        if (representationProperty != null) {
            result = result.withRepresentationProperty(representationProperty);
        }
        URI configDescriptionURI = baseType.getConfigDescriptionURI();
        if (configDescriptionURI != null) {
            result = result.withConfigDescriptionURI(configDescriptionURI);
        }
        String category = baseType.getCategory();
        if (category != null) {
            result = result.withCategory(category);
        }
        String description = baseType.getDescription();
        if (description != null) {
            result = result.withDescription(description);
        }

        return result;
    }
}
