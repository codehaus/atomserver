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

import org.springframework.jmx.export.MBeanExporter;

import javax.management.JMException;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.ModelMBeanInfo;

/**
 * This class is used to ensure that the MBeanExporter doesn't export getter/setter operations.
 * <p/>
 * This class was taken from a Sun blog. For more information see:
 * http://weblogs.java.net/blog/emcmanus/archive/2007/02/removing_getter.html
 */
public class NoGetterMBeanExporter extends MBeanExporter {
    public NoGetterMBeanExporter() {
    }

    protected void doRegister(Object mbean, ObjectName objectName) throws JMException {
        if (mbean instanceof ModelMBean) {
            ModelMBean mmb = (ModelMBean) mbean;
            ModelMBeanInfo mmbi = (ModelMBeanInfo) mmb.getMBeanInfo();
            mmb.setModelMBeanInfo(new NoGetterMBeanInfo(mmbi));
        }
        super.doRegister(mbean, objectName);
    }
}
