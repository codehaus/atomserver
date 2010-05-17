package org.atomserver.spring;

import org.atomserver.core.Substrate;

public class SubstrateFactoryBean extends ConfigUriFactoryBean {
    public Class getObjectType() { return Substrate.class; }
}
