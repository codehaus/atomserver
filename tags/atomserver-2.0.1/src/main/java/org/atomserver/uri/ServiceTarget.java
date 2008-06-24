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

package org.atomserver.uri;

import org.atomserver.ServiceDescriptor;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.TargetType;

/**
 * A URITarget that specifically represents a Service request
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class ServiceTarget extends URITarget implements ServiceDescriptor {
    private final String workspace;

    public ServiceTarget(RequestContext requestContext,
                         final String workspace) {
        super(TargetType.TYPE_SERVICE, requestContext);
        this.workspace = workspace;
    }

    public String getWorkspace() {
        return workspace;
    }
}