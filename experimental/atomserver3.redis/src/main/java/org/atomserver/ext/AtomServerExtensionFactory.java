package org.atomserver.ext;

import org.apache.abdera.util.AbstractExtensionFactory;
import org.atomserver.AtomServerConstants;

public class AtomServerExtensionFactory extends AbstractExtensionFactory {
    public AtomServerExtensionFactory() {
        super(AtomServerConstants.NAMESPACE);
        addImpl(AtomServerConstants.FILTER, Filter.class);
        addImpl(AtomServerConstants.STATUS, Status.class);
    }
}
