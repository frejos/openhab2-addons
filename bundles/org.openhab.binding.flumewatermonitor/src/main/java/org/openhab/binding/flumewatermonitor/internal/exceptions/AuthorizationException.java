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
package org.openhab.binding.flumewatermonitor.internal.exceptions;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link AuthorizationException} is thrown whenever account authorization
 * fails.
 *
 * @author Sara Geleskie Damiano - Initial contribution
 */
@NonNullByDefault
public class AuthorizationException extends Exception {
    private static final long serialVersionUID = 3630924933867280491L;

    public AuthorizationException() {
    }

    public AuthorizationException(String message) {
        super(message);
    }
}
