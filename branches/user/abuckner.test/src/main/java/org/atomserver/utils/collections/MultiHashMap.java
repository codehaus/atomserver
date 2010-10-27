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


package org.atomserver.utils.collections;

import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
@SuppressWarnings("serial")
public class MultiHashMap<K, V> extends HashMap<K, Set<V>> {
    public Set<V> putValue(K k, V v) {
        Set<V> set = lazy(k);
        set.add(v);
        return set;
    }

    public boolean remove(K k, V v) {
        Set<V> set = lazy(k);
        return set.remove(v);
    }

    private Set<V> lazy(K k) {
        Set<V> set = get(k);
        if (set == null) {
            put(k, set = new HashSet<V>());
        }
        return set;
    }
}
