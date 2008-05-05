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
package org.atomserver.core.etc;

import javax.xml.namespace.QName;

/**
 * Constants for AtomServer, such XML namespaces and extended APP element names
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public final class AtomServerConstants {
    private AtomServerConstants() {}

    public static final String SCHEMAS_NAMESPACE = "http://schemas.atomserver.org/atomserver/v1/rev0";

    public static final String ATOMSERVER_NS = "http://atomserver.org/namespaces/1.0/";

    public static final String ATOMSERVER_NS_PREFIX = "as";

    public static final String ATOMSERVER_BATCH_NS = "http://atomserver.org/namespaces/1.0/batch";

    public static final String ATOMSERVER_BATCH_NS_PREFIX = "asbatch";

    public static final String END_INDEX_LN = "endIndex";
    public static final QName END_INDEX = new QName(ATOMSERVER_NS, END_INDEX_LN, ATOMSERVER_NS_PREFIX);

    public static final String OPERATION_LN = "operation";
    public static final QName OPERATION = new QName(ATOMSERVER_BATCH_NS, OPERATION_LN, ATOMSERVER_BATCH_NS_PREFIX);

    public static final String STATUS_LN = "status";
    public static final QName STATUS = new QName(ATOMSERVER_BATCH_NS, STATUS_LN, ATOMSERVER_BATCH_NS_PREFIX);

    public static final String RESULTS_LN = "results";
    public static final QName RESULTS = new QName(ATOMSERVER_BATCH_NS, RESULTS_LN, ATOMSERVER_BATCH_NS_PREFIX);
}
