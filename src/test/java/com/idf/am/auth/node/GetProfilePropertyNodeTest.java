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
 * Portions copyright 2021-2022 Identity Fusion Inc.
 */
package com.idf.am.auth.node;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;

import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.sun.identity.idm.AMIdentity;

public class GetProfilePropertyNodeTest {

    private static final String USER = "user.1";
    @Mock
    private GetProfilePropertyNode.Config config;
    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private AMIdentity identity;
    @Mock
    private Realm realm;
    @Mock
    private Node verifierNode;
    private Node node;
    private AutoCloseable mocks;

    @BeforeMethod
    public void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        node = new GetProfilePropertyNode(config, realm, coreWrapper);
        given(verifierNode.getInputs()).willReturn(new InputState[]{new InputState("*")});
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    public void shouldRetrieveMultipleAttributesAtOnce() throws Exception {
        given(config.properties()).willReturn(ImmutableMap.of("foo", "bar", "fizz", "buzz"));
        given(coreWrapper.getIdentity(anyString(), eq(realm))).willReturn(identity);

        node.process(setupTreeContext());

        verify(identity).getAttributes(eq(ImmutableSet.of("foo", "fizz")));
    }

    @Test
    public void shouldCarryOnWhenIdentityCannotBeFound() throws Exception {
        Action action = node.process(setupTreeContext());

        assertThat(action.outcome).isEqualTo("outcome");

        verifyNoInteractions(identity);
    }

    @Test
    public void shouldSetAttributesOnSharedState() throws Exception {
        given(config.properties()).willReturn(ImmutableMap.of("fizz", "buzz"));
        given(coreWrapper.getIdentity(anyString(), eq(realm))).willReturn(identity);
        given(identity.getAttributes(eq(singleton("fizz")))).willReturn(singletonMap("fizz", singleton("aldrin")));

        TreeContext context = setupTreeContext();
        node.process(context);

        assertThat(context.getStateFor(verifierNode).get("buzz").asString()).isEqualTo("aldrin");
    }

    @Test
    public void shouldUseArraysForMultivaluedAttributes() throws Exception {
        given(config.properties()).willReturn(ImmutableMap.of("foo", "bar"));
        given(coreWrapper.getIdentity(anyString(), eq(realm))).willReturn(identity);
        given(identity.getAttributes(eq(singleton("foo"))))
                .willReturn(singletonMap("foo", ImmutableSet.of("hello", "world")));

        TreeContext context = setupTreeContext();
        node.process(context);

        assertThat(context.getStateFor(verifierNode).get("bar").asList(String.class))
                .contains("hello", "world");
    }

    private TreeContext setupTreeContext() {
        return new TreeContext(json(object(field(USERNAME, USER))),
                json(object(field("transient", "content"))), new ExternalRequestContext.Builder().build(), emptyList(),
                Optional.empty());
    }
}
