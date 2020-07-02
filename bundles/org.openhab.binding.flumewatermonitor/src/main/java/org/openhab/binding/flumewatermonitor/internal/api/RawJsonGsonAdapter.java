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
package org.openhab.binding.flumewatermonitor.internal.api;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * The {@link RawJsonGsonAdapter} is used to leave a portion of the JSON in String format rather than attempting to
 * deserialize it into a class. This is used for some of the fields in the response DTO that could be missing, null,
 * empty arrays, or a structure that is variable. Because of the possibility of all the nulls, and the variable
 * structure, even using a TypeToken and a generic class fails to properly parse the subclass. To get around it, for the
 * messy fields I'm having gson leave them as strings at the first pass and then deserializing the string in a second
 * pass within the specific request response where I already know what the structure will be.
 *
 * Code taken from Daniel in this StackOverflow question:
 * https://stackoverflow.com/questions/30816176/prevent-gson-from-serializing-json-string
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class RawJsonGsonAdapter extends TypeAdapter<String> {

    @Override
    public void write(final @Nullable JsonWriter out, final String value) throws IOException {
        if (out != null) {
            out.jsonValue(value);
        }
    }

    @Override
    public String read(final @Nullable JsonReader in) throws IOException {
        return in.toString();
    }
}
