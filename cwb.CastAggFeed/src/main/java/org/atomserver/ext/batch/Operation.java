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


package org.atomserver.ext.batch;

import org.atomserver.core.etc.AtomServerConstants;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.ElementWrapper;
import org.apache.abdera.model.ExtensibleElement;

import javax.xml.namespace.QName;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class Operation extends ElementWrapper {
    private static final QName TYPE = new QName("type");

    public Operation(Element element) {
        super(element);
    }

    public Operation(Factory factory) {
        super(factory, AtomServerConstants.OPERATION);
    }

    public Operation(ExtensibleElement parent) {
        this(parent.getFactory());
        parent.declareNS(AtomServerConstants.ATOMSERVER_BATCH_NS, AtomServerConstants.ATOMSERVER_BATCH_NS_PREFIX);
    }

    public void setType(String type) {
        if (type == null) {
            getInternal().removeAttribute(TYPE);
        } else {
            getInternal().setAttributeValue(TYPE, type);
        }
    }

    public String getType() {
        return getInternal().getAttributeValue(TYPE);
    }
}