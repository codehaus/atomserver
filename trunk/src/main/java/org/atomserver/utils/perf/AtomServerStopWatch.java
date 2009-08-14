/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
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

package org.atomserver.utils.perf;

import org.perf4j.log4j.Log4JStopWatch;
import org.atomserver.server.servlet.AtomServerUserInfo;
import org.apache.abdera.protocol.server.RequestContext;

/**
 * AtomServerStopWatch appends the authenticated user name to the tag when logging the elapsed time.
 * The user name is set in the ThreadLocal by the servlet filter.
 */
public class AtomServerStopWatch extends Log4JStopWatch {
    String user;

    public AtomServerStopWatch() {       
        this.user = AtomServerUserInfo.getUser();
    }

    public String stop(String tag) {
        return this.stop(getUserAppendedTag(tag),"");
    }

    public String stop(String tag, String message) {
        return super.stop(getUserAppendedTag(tag),message);
    }

    private String getUserAppendedTag(String tag) {
        return (this.user == null) ? tag : tag + "." + this.user;
    }
}
