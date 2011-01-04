package org.atomserver.spring;

import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.w3c.dom.Element;

// TODO: this class will provide custom XML elements for configuring AtomServer in Spring
public class AtomServerNamespaceHandler extends NamespaceHandlerSupport {

    public AtomServerNamespaceHandler() {
        registerBeanDefinitionParser("content", new AbstractSimpleBeanDefinitionParser() {
            protected Class<?> getBeanClass(Element element) {
                return null;
            };
        });
    }

    public void init() {

    }
}
