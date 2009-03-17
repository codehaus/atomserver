package org.atomserver.ext;

import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.ExtensibleElement;
import org.apache.abdera.model.ExtensibleElementWrapper;
import org.atomserver.AtomServerConstants;

import javax.xml.namespace.QName;

public class Status extends ExtensibleElementWrapper {
    private static final QName STATUS_CODE = new QName("code");
    private static final QName MESSAGE = new QName("message");


    public Status(Element element) {
        super(element);
    }

    public Status(Factory factory) {
        super(factory, AtomServerConstants.STATUS);
    }

    public Status(ExtensibleElement parent) {
        this(parent.getFactory());
        parent.declareNS(AtomServerConstants.NAMESPACE, AtomServerConstants.PREFIX);
    }

    public void setStatusCode(int statusCode) {
        getInternal().setAttributeValue(STATUS_CODE, String.valueOf(statusCode));
    }

    public int getStatusCode() {
        return Integer.valueOf(getInternal().getAttributeValue(STATUS_CODE));
    }

    public void setMessage(String message) {
        addSimpleExtension(MESSAGE, message);
    }

    public String getMessage() {
        return getSimpleExtension(MESSAGE);
    }
}
