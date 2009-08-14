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
import java.text.MessageFormat;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class Results extends ElementWrapper {
    private static final QName INSERTS = new QName("inserts");
    private static final QName UPDATES = new QName("updates");
    private static final QName DELETES = new QName("deletes");
    private static final QName ERRORS = new QName("errors");

    public Results(Element element) {
        super(element);
    }

    public Results(Factory factory) {
        super(factory, AtomServerConstants.RESULTS);
    }

    public Results(ExtensibleElement parent) {
        this(parent.getFactory());
        parent.declareNS(AtomServerConstants.ATOMSERVER_BATCH_NS, AtomServerConstants.ATOMSERVER_BATCH_NS_PREFIX);
    }

    public void setInserts(int inserts) {
        getInternal().setAttributeValue(INSERTS, String.valueOf(inserts));
    }

    public int getInserts() {
        return Integer.parseInt(getInternal().getAttributeValue(INSERTS));
    }

    public void setUpdates(int updates) {
        getInternal().setAttributeValue(UPDATES, String.valueOf(updates));
    }

    public int getUpdates() {
        return Integer.parseInt(getInternal().getAttributeValue(UPDATES));
    }

    public void setDeletes(int deletes) {
        getInternal().setAttributeValue(DELETES, String.valueOf(deletes));
    }

    public int getDeletes() {
        return Integer.parseInt(getInternal().getAttributeValue(DELETES));
    }

    public void setErrors(int errors) {
        getInternal().setAttributeValue(ERRORS, String.valueOf(errors));
    }

    public int getErrors() {
        return Integer.parseInt(getInternal().getAttributeValue(ERRORS));
    }

    public String toString() {
        return MessageFormat.format("[{0} inserts, {1} updates, {2} deletes, {3} errors]",
                                    getInserts(), getUpdates(), getDeletes(), getErrors());
    }
}