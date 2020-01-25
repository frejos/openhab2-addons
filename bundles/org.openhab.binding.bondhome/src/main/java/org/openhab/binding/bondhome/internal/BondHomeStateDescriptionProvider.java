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

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.type.DynamicStateDescriptionProvider;
import org.eclipse.smarthome.core.types.StateDescription;
import org.osgi.service.component.annotations.Component;
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
@Component(configurationPid = "binding.bondhome", service = { DynamicStateDescriptionProvider.class })
public class BondHomeStateDescriptionProvider implements DynamicStateDescriptionProvider {
    private Logger logger = LoggerFactory.getLogger(BondHomeStateDescriptionProvider.class);

    private final Map<ChannelUID, StateDescription> descriptions = new ConcurrentHashMap<>();

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
