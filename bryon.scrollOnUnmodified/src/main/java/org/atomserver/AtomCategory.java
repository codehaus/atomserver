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

package org.atomserver;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;

/**
 * AtomCategory - represents the information of an AtomPub Category,
 * including Scheme and Term. NOTE; Label is not currently implemented
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class AtomCategory {
    private String scheme = null;
    private String term = null;

    public AtomCategory(String scheme, String term) {
        this.scheme = scheme;
        this.term = term;
    }

    public String getScheme() { return scheme; }

    public String getTerm() { return term; }

    public String toString() {
        return ("[" + scheme + " " + term + "]");
    }

    public int hashCode() {
        return new HashCodeBuilder(16661, 8675309)
                .append(scheme).append(term).toHashCode();
    }

    public boolean equals(Object obj) {
        if (obj == null || !AtomCategory.class.equals(obj.getClass())) {
            return false;
        }
        AtomCategory other = (AtomCategory) obj;
        return new EqualsBuilder()
                .append(scheme, other.scheme)
                .append(term, other.term)
                .isEquals();
    }
}
