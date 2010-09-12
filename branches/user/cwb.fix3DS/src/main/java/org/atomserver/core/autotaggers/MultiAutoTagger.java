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
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;
import org.w3c.dom.Document;

import java.util.List;

/**
 * MultiAutoTagger - provides for composition of multiple EntryAutoTaggers for a single workspace.
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class MultiAutoTagger extends BaseAutoTagger {

    private List<EntryAutoTagger> taggers;

    /**
     * {@inheritDoc}
     */
    public boolean tag(EntryMetaData entry, Document doc) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            boolean modified = false;
            for (EntryAutoTagger tagger : taggers) {
                modified |= tagger.tag(entry, doc);
            }
            return modified;
        } finally {
            stopWatch.stop("AutoTagger.multi", AtomServerPerfLogTagFormatter.getPerfLogEntryString(entry));
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
