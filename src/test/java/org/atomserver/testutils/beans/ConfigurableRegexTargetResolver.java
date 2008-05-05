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

package org.atomserver.testutils.beans;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

//import org.apache.abdera.protocol.server.provider.TargetType;
import org.apache.abdera.protocol.server.TargetType;
//import org.apache.abdera.protocol.server.util.RegexTargetResolver;
import org.apache.abdera.protocol.server.impl.RegexTargetResolver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * ConfigurableRegexTargetResolver reads the Regex mappings from a properties file instead of hard coding them.
 *
 * Each line of the properties file must consist of two fields 
 * <i>separated by one or more TAB characters</i> (no spaces!!!).
 * 
 * The first field is a regular expression that is matched against the request URI.
 * The second field is a target type and can be one of: SERVICE, COLLECTION, ENTRY, MEDIA, CATEGORIES
 * Blank lines and lines starting with a hash sign ('#') are ignored.
 * 
 * <b>Be very careful not to edit the properties file with an editor that removes tabs!!!</b>
 *
 * NOTE: the mapping is Pattern-match to Type, so you can have several Patterns associated with a given Type
 * 
 * For example; 
<pre>
/atom/(\?[^#]*)?				SERVICE
/atom/widgets/([^/#?]+)/?			COLLECTION
/atom/widgets/([^/#?]+)/([^/#?]+)(\?[^#]*)?	ENTRY
</pre>
 * 
 * Note: modeled after code by Ugo Cei (received in private correspondence).
 *       It may someday show up as a standalone Atom Server called Apache Nucleus  
 */
public class ConfigurableRegexTargetResolver extends RegexTargetResolver {

    private final static Log logger = LogFactory.getLog(ConfigurableRegexTargetResolver.class);
    
    public ConfigurableRegexTargetResolver() {
        //super("");
    }
    
    public void setConfiguration(InputStream in) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line = null;
        while ((line = br.readLine()) != null) {
            if ("".equals(line.trim()) || line.startsWith("#")) {
                continue;
            }
            String[] fields = line.split("\\t+");
            if (fields.length < 2) {
                logger.warn("Invalid configuration line: {" + line + "}.");
                continue;
            }
            TargetType type = TargetType.get(fields[1]);
            if (type == null) {
                logger.warn("No such target type: {" + fields[1] + "}");
                continue;
            }
            setPattern(fields[0], type);
            if (logger.isDebugEnabled()) {
                logger.debug("Set pattern: " + fields[0] + " => " + type);
            }
        }
    }

}
