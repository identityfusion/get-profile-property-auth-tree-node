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
 * Copyright 2019 ForgeRock AS.
 * Portions copyright 2021 Identity Fusion Inc.
 */
package com.idf.auth.nodes;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.idf.auth.nodes.GetProfilePropertyNode.Config;
import com.sun.identity.idm.AMIdentity;

public class GetProfilePropertyNodeTest {

    private static final String USER = "user.1";
    private static final String REALM_NAME = "hello-world";
    @Mock
    private Config config;
    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private AMIdentity identity;
    private Node node;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
        node = new GetProfilePropertyNode(config, coreWrapper);
    }

    @Test
    public void shouldRetrieveMultipleAttributesAtOnce() throws Exception {
        given(config.properties()).willReturn(ImmutableMap.of("foo", "bar", "fizz", "buzz"));
        given(coreWrapper.getIdentity(anyString(), anyString())).willReturn(identity);

        node.process(setupTreeContext());

        verify(identity).getAttributes(eq(ImmutableSet.of("foo", "fizz")));
    }

    @Test
    public void shouldCarryOnWhenIdentityCannotBeFound() throws Exception {
        Action action = node.process(setupTreeContext());

        assertThat(action.outcome).isEqualTo("outcome");

        verifyZeroInteractions(identity);
    }

    @Test
    public void shouldSetAttributesOnSharedState() throws Exception {
        given(config.properties()).willReturn(ImmutableMap.of("fizz", "buzz"));
        given(coreWrapper.getIdentity(anyString(), anyString())).willReturn(identity);
        given(identity.getAttributes(eq(singleton("fizz")))).willReturn(singletonMap("fizz", singleton("aldrin")));

        Action action = node.process(setupTreeContext());

        assertThat(action.sharedState.asMap())
                .contains(entry("buzz", "aldrin"));
    }

    @Test
    public void shouldUseArraysForMultivaluedAttributes() throws Exception {
        given(config.properties()).willReturn(ImmutableMap.of("foo", "bar"));
        given(coreWrapper.getIdentity(anyString(), anyString())).willReturn(identity);
        given(identity.getAttributes(eq(singleton("foo"))))
                .willReturn(singletonMap("foo", ImmutableSet.of("hello", "world")));

        Action action = node.process(setupTreeContext());

        assertThat(action.sharedState.asMap())
                .contains(entry("bar", asList("hello", "world")));
    }

    private TreeContext setupTreeContext() {
        return new TreeContext(json(object(field(USERNAME, USER), field(REALM, REALM_NAME))),
                json(object(field("transient", "content"))), new ExternalRequestContext.Builder().build(), emptyList());
    }
}
