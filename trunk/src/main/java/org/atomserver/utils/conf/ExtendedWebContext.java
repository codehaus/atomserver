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

package org.atomserver.utils.conf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.utils.conf.ConfigurationAwareClassLoader;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.XmlWebApplicationContext;

import java.io.IOException;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class ExtendedWebContext
        extends XmlWebApplicationContext {
    private static final Log log = LogFactory.getLog(ExtendedWebContext.class);

    public ExtendedWebContext() {
        super();
        log.trace("creating ExtendedWebContext");
        setClassLoader(new ConfigurationAwareClassLoader(getClassLoader()));
    }

    public Resource[] getResources(String s) throws IOException {
        log.trace("getting resources : " + s);
        try {
            Resource[] resources = super.getResources(s);
            if (log.isTraceEnabled() && resources != null) {
                log.trace("found " + resources.length + " resources for " + s);
                for (Resource resource : resources) {
                    log.trace(" found resource : " + resource);
                }
            }
            return resources;
        } catch (IOException e) {
            if (s.endsWith("*.xml")) {
                log.warn("IOException getting resources - returning no resources");
                return new Resource[0];
            } else {
                throw e;
            }
        }
    }
}