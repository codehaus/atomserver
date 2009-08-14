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


package org.atomserver.utils.jmx;

import javax.management.Descriptor;
import javax.management.MBeanException;
import javax.management.MBeanOperationInfo;
import javax.management.modelmbean.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This ModelMBeanInfoSupport is used to get around the fact that ModelMBeans always export attributes as BOTH
 * attributes AND getter/setter operations (where the getter/setter operations are unnecessary and just clutter
 * up the management UI).
 * <p/>
 * This class is taken from a Sun blog. For more information on how/why this works, see:
 * http://weblogs.java.net/blog/emcmanus/archive/2007/02/removing_getter.html
 */
public class NoGetterMBeanInfo extends ModelMBeanInfoSupport {
    public NoGetterMBeanInfo(ModelMBeanInfo mmbi) {
        super(mmbi);
    }

    public NoGetterMBeanInfo clone() {
        return new NoGetterMBeanInfo(this);
    }

    private Object writeReplace() {
        List<ModelMBeanOperationInfo> ops = new ArrayList<ModelMBeanOperationInfo>();
        //get rid of the getter/setter operations
        for (MBeanOperationInfo mboi : this.getOperations()) {
            ModelMBeanOperationInfo mmboi = (ModelMBeanOperationInfo) mboi;
            Descriptor d = mmboi.getDescriptor();
            String role = (String) d.getFieldValue("role");
            if (!"getter".equalsIgnoreCase(role) && !"setter".equalsIgnoreCase(role)) {
                ops.add(mmboi);
            }
        }
        ModelMBeanOperationInfo[] mbois = new ModelMBeanOperationInfo[ops.size()];
        ops.toArray(mbois);
        Descriptor mbeanDescriptor;
        try {
            mbeanDescriptor = this.getMBeanDescriptor();
        } catch (MBeanException e) {
            throw new RuntimeException(e);
        }
        return new ModelMBeanInfoSupport(
                this.getClassName(),
                this.getDescription(),
                (ModelMBeanAttributeInfo[]) this.getAttributes(),
                (ModelMBeanConstructorInfo[]) this.getConstructors(),
                mbois,
                (ModelMBeanNotificationInfo[]) this.getNotifications(),
                mbeanDescriptor);
    }
}
