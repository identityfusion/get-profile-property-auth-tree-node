/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017-2019 ForgeRock AS.
 * Portions copyright 2021-2022 Identity Fusion Inc.
 */
package com.idf.am.auth.node;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import java.util.Map;

import javax.inject.Inject;

import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;

/**
 * Core nodes installed by default with no engine dependencies.
 */
public class GetProfilePropertyNodePlugin extends AbstractNodeAmPlugin {

    private static final String PLUGIN_VERSION = "1.0.0";

    @Inject
    public GetProfilePropertyNodePlugin() {
    }

    @Override
    public String getPluginVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    protected Map<String, Iterable<? extends Class<? extends Node>>> getNodesByVersion() {
        return singletonMap(PLUGIN_VERSION, singletonList(GetProfilePropertyNode.class));
    }
}
