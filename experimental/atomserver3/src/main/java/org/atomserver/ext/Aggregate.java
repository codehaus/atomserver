package org.atomserver.ext;

import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.ElementWrapper;
import org.apache.abdera.model.ExtensibleElement;
import org.apache.abdera.model.Entry;
import org.atomserver.AtomServerConstants;

import javax.xml.namespace.QName;

public class Aggregate extends ElementWrapper {
    private static final QName COLLECTION = new QName("collection");
    private static final QName ENTRY_ID = new QName("entryid");

    public Aggregate(Element element) {
        super(element);
    }

    public Aggregate(Factory factory) {
        super(factory, AtomServerConstants.AGGREGATE);
    }

    public Aggregate(ExtensibleElement parent) {
        this(parent.getFactory());
        parent.declareNS(AtomServerConstants.NAMESPACE, AtomServerConstants.PREFIX);
    }

    public void setCollection(String collection) {
        getInternal().setAttributeValue(COLLECTION, collection);
    }

    public String getCollection() {
        return getInternal().getAttributeValue(COLLECTION);
    }

    public void setEntryId(String entryId) {
        getInternal().setAttributeValue(ENTRY_ID, entryId);
    }

    public String getEntryId() {
        return getInternal().getAttributeValue(ENTRY_ID);
    }

    public String toString() {
        return String.format("$join/%s/%s", getCollection(), getEntryId());
    }
}
