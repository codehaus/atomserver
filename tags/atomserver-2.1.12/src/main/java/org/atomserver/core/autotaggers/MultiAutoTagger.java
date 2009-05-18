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


package org.atomserver.core.autotaggers;

import org.atomserver.EntryAutoTagger;
import org.atomserver.core.EntryMetaData;

import java.util.List;

/**
 * MultiAutoTagger - provides for composition of multiple EntryAutoTaggers for a single workspace.
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class MultiAutoTagger implements EntryAutoTagger {

    private List<EntryAutoTagger> taggers;

    /**
     * {@inheritDoc}
     */
    public void tag(EntryMetaData entry, String contentXML) {
        for (EntryAutoTagger tagger : taggers) {
            tagger.tag(entry, contentXML);
        }
    }

    /**
     * Setter for property 'taggers'.
     *
     * @param taggers Value to set for property 'taggers'.
     */
    public void setTaggers(List<EntryAutoTagger> taggers) {
        this.taggers = taggers;
    }
}
