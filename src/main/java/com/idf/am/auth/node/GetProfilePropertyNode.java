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
 * Portions copyright 2021-2024 Identity Fusion Inc.
 */
package com.idf.am.auth.node;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.am.identity.application.IdentityStoreFactory;
import org.forgerock.am.identity.persistence.IdentityStore;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;

/**
 * A node which contributes a configurable set of properties to be added to the user's session, if/when it is created.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = GetProfilePropertyNode.Config.class)
public class GetProfilePropertyNode extends SingleOutcomeNode {

    private static final Logger logger = LoggerFactory.getLogger(GetProfilePropertyNode.class);
    private final Realm realm;
    private final IdentityStoreFactory identityStoreFactory;

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * A map of property name to value.
         *
         * @return a map of properties.
         */
        @Attribute(order = 100)
        Map<String, String> properties();
    }

    private final Config config;

    /**
     * Constructs a new GetSessionPropertiesNode instance.
     *
     * @param config Node configuration.
     */
    @Inject
    public GetProfilePropertyNode(@Assisted Config config, @Assisted Realm realm,
            IdentityStoreFactory identityStoreFactory) {
        this.config = config;
        this.realm = realm;
        this.identityStoreFactory = identityStoreFactory;
    }

    @Override
    public Action process(TreeContext context) {
        NodeState state = context.getStateFor(this);
        String username = state.get(USERNAME).asString();
        logger.trace("Searching for user {} in realm {}", username, realm);

        IdentityStore identityStore = identityStoreFactory.create(realm);
        try {
            Optional<AMIdentity> identity = identityStore.findIdentityByUsername(username, IdType.USER);
            if (identity.isEmpty()) {
                logger.error("Unable to find user identity, profile attributes will not be saved in shared state");
                return goToNext().build();
            }
            Map<String, String> attributeMappings = config.properties();
            @SuppressWarnings("unchecked")
            Map<String, Set<String>> attributes = identity.get().getAttributes(attributeMappings.keySet());
            for (Map.Entry<String, String> mapping : attributeMappings.entrySet()) {
                String attribute = mapping.getKey();
                Set<String> values = attributes.get(attribute);
                if (values == null || values.isEmpty()) {
                    logger.warn("Unable to find attribute value for: {}", attribute);
                } else {
                    logger.trace("Found attribute value for: {}", attribute);
                    state.putShared(mapping.getValue(), convertValues(values));
                }
            }
        } catch (IdRepoException | SSOException ex) {
            logger.error("Unable to retrieve profile attributes", ex);
        }

        return goToNext().build();
    }

    private Object convertValues(Set<String> values) {
        if (values.size() == 1) {
            return values.iterator().next();
        } else {
            return array(values.toArray());
        }
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[]{
                new InputState(USERNAME)
        };
    }

    @Override
    public OutputState[] getOutputs() {
        return config.properties().values()
                .stream()
                .map(OutputState::new)
                .toArray(OutputState[]::new);
    }
}
