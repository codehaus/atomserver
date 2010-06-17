/* Copyright (c) 2007 HomeAway, Inc.
 *  All rights reserved.  http://www.atomserver.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.atomserver;

import org.apache.abdera.protocol.server.Provider;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.TargetType;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.util.Map;

/**
 * DelegatingProvider - an Abdera Provider which delegates to one of the Providers
 * configured in a Map of Providers (i.e. AtomServers). It is possible to
 * switch between Providers using JMX. Currently there are only two types
 * of Providers:
 * <ol>
 * <li><b>normal</b> The regular AtomServer</li>
 * <li><b>throttled</b> The throttled AtomServer</li>
 * </ol>
 */
@ManagedResource(description = "Delegating AtomServer Provider")
public class DelegatingProvider implements Provider {

    /**
     * These enums must match the names prescribed in abderaBeans
     */
    public enum AtomServerType { normal, throttled };

    private Map<String, Provider> providers;
    private String currentProviderName;

    /**
     * Set the Map of Providers available for delegation
     * @param providers  The Map of Providers. Keys include; "normal" and "throttled"
     */
    public void setProviders(Map<String, Provider> providers) {
        this.providers = providers;
    }

    /**
     * Set the current Provider name. Should be one of the enum; DelegatingProvider.AtomServerType
     * @param currentProviderName
     */
    @ManagedAttribute(description = "set the current AtomServer Provider (normal or throttled)")
    public void setCurrentProviderName(String currentProviderName) {
        if (providers.keySet().contains(currentProviderName)) {
            this.currentProviderName = currentProviderName;
        } else {
            throw new IllegalArgumentException("no provider with the given name (" +
                                               currentProviderName + ") is configured");
        }
    }

    /**
     * Return the current Provider name. Will be one of the enum; DelegatingProvider.AtomServerType
     * @return The current Provider name.
     */
    @ManagedAttribute(description = "get the current AtomServer Provider")
    public String getCurrentProviderName() {
        return currentProviderName;
    }

    /**
     * {@inheritDoc}
     */
    public ResponseContext createEntry(RequestContext requestContext) {
        return getCurrentProvider().createEntry(requestContext);
    }

    /**
     * {@inheritDoc}
     */
    public Provider getCurrentProvider() {
        return providers.get(currentProviderName);
    }

    /**
     * {@inheritDoc}
     */
    public ResponseContext deleteEntry(RequestContext requestContext) {
        return getCurrentProvider().deleteEntry(requestContext);
    }

    /**
     * {@inheritDoc}
     */
    public ResponseContext deleteMedia(RequestContext requestContext) {
        return getCurrentProvider().deleteMedia(requestContext);
    }

    /**
     * {@inheritDoc}
     */
    public ResponseContext updateEntry(RequestContext requestContext) {
        return getCurrentProvider().updateEntry(requestContext);
    }

    /**
     * {@inheritDoc}
     */
    public ResponseContext updateMedia(RequestContext requestContext) {
        return getCurrentProvider().updateMedia(requestContext);
    }

    /**
     * {@inheritDoc}
     */
    public ResponseContext getService(RequestContext requestContext) {
        return getCurrentProvider().getService(requestContext);
    }

    /**
     * {@inheritDoc}
     */
    public ResponseContext getFeed(RequestContext requestContext) {
        return getCurrentProvider().getFeed(requestContext);
    }

    /**
     * {@inheritDoc}
     */
    public ResponseContext getEntry(RequestContext requestContext) {
        return getCurrentProvider().getEntry(requestContext);
    }

    /**
     * {@inheritDoc}
     */
    public ResponseContext getMedia(RequestContext requestContext) {
        return getCurrentProvider().getMedia(requestContext);
    }

    /**
     * {@inheritDoc}
     */
    public ResponseContext getCategories(RequestContext requestContext) {
        return getCurrentProvider().getCategories(requestContext);
    }

    /**
     * {@inheritDoc}
     */
    public ResponseContext entryPost(RequestContext requestContext) {
        return getCurrentProvider().entryPost(requestContext);
    }

    /**
     * {@inheritDoc}
     */
    public ResponseContext mediaPost(RequestContext requestContext) {
        return getCurrentProvider().mediaPost(requestContext);
    }

    /**
     * {@inheritDoc}
     */
    public ResponseContext request(RequestContext requestContext) {
        return getCurrentProvider().request(requestContext);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getAllowedMethods(TargetType targetType) {
        return getCurrentProvider().getAllowedMethods(targetType);
    }
}

