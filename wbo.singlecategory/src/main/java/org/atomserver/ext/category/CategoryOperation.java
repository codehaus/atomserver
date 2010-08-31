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


package org.atomserver.ext.category;

import org.atomserver.core.etc.AtomServerConstants;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.ElementWrapper;
import org.apache.abdera.model.ExtensibleElement;

import javax.xml.namespace.QName;

/**
 * Single Category operations supported by this class are:
 * i. insert a category  (type="insert")
 * ii. update a category (type="update")
 * iii. delete a category (type="delete")
 *
 * This operation is an added extension to Categories element. If the operation
 * is an insert, all the listed categories will be inserted.
 * If the operation is an update, Categories object should consist of only two categories,
 * the first of which will be replaced by the second if the scheme are the same. If the first
 * do not exists or not of the expected term value, the request will be flagged as Optimistic
 * Concurrency exception. If they are of different scheme values, an error code will be returned.
 * If the operation is a delete, all the categories in the Categories object will be deleted.
 *
 */
public class CategoryOperation extends ElementWrapper {
    
    public static final String INSERT="insert";
    public static final String UPDATE="update";
    public static final String DELETE="delete";

    public static final QName TYPE = new QName("type");

    public CategoryOperation(Element element) {
        super(element);
    }

    public CategoryOperation(Factory factory) {
        super(factory, AtomServerConstants.CATEGORY_OP);
    }

    public CategoryOperation(ExtensibleElement parent) {
        this(parent.getFactory());
        parent.declareNS(AtomServerConstants.ATOMSERVER_CATEGORY_NS, AtomServerConstants.ATOMSERVER_CATEGORY_NS_PREFIX);
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

    public boolean isInsert() {
        return INSERT.equals(getType());
    }

    public boolean isUpdate() {
        return UPDATE.equals(getType());
    }

    public boolean isDelete() {
        return DELETE.equals(getType());
    }
}