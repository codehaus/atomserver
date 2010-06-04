package org.atomserver.ext;

import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.ExtensibleElement;
import org.apache.abdera.model.ExtensibleElementWrapper;
import org.atomserver.AtomServerConstants;

import javax.xml.namespace.QName;

public class Filter extends ExtensibleElementWrapper {
    private static final QName CLASSNAME = new QName("class");

    public Filter(Element element) {
        super(element);
    }

    public Filter(Factory factory) {
        super(factory, AtomServerConstants.FILTER);
    }

    public Filter(ExtensibleElement parent) {
        this(parent.getFactory());
        parent.declareNS(AtomServerConstants.NAMESPACE, AtomServerConstants.PREFIX);
    }

    public void setClassname(String classname) {
        getInternal().setAttributeValue(CLASSNAME, classname);
    }

    public String getClassname() {
        return getInternal().getAttributeValue(CLASSNAME);
    }
}
