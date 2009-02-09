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

package org.atomserver.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.atomserver.*;

/**
 * CollectionOptions - The options which may be set for an AtomCollection.
 * This class is meant to be configured from by an IOC container like Spring, and then wired into an AtomCollection
 * to configure it. This level of indirection allows AtomServer to cleanly separate configuration
 * from the AtomCollection API, which allows Users to supply their own versions or overrides to the properties
 * defined herein
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class CollectionOptions {
    static private final Log log = LogFactory.getLog(CollectionOptions.class);

    private String name = null;
    private boolean localized = false;
    private boolean verboseDeletions = true;

    private boolean producingTotalResultsFeedElement = false;
    private boolean producingEntryCategoriesFeedElement = false;

    private ContentStorage contentStorage = null;
    private ContentValidator contentValidator = null;

    private EntryAutoTagger autoTagger = null;

    private EntryIdGenerator entryIdGenerator = null;

    private int maxLinkEntriesPerPage = -1;
    private int maxFullEntriesPerPage = -1;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public void setLocalized(boolean localized) {
        this.localized = localized;
    }
    public boolean isLocalized() {
        return localized;
    }

    public void setProducingTotalResultsFeedElement( boolean producingTotalResultsFeedElement ) {
        this.producingTotalResultsFeedElement = producingTotalResultsFeedElement;
    }
    public boolean isProducingTotalResultsFeedElement() {
        return producingTotalResultsFeedElement;
    }

    public void setProducingEntryCategoriesFeedElement( boolean producingEntryCategoriesFeedElement ) {
        this.producingEntryCategoriesFeedElement = producingEntryCategoriesFeedElement;
    }
    public boolean isProducingEntryCategoriesFeedElement() {
        return producingEntryCategoriesFeedElement;
    }

    public void setContentStorage(ContentStorage contentStorage) {
        this.contentStorage = contentStorage;

    }       
    protected ContentStorage getContentStorage() {
        return this.contentStorage;
    }
        
    public void setContentValidator( ContentValidator contentValidator ) {
        this.contentValidator = contentValidator;
    }       
    public ContentValidator getContentValidator() {
        return contentValidator;
    }

    public void setAutoTagger(EntryAutoTagger autoTagger) {
        this.autoTagger = autoTagger;
    }
    public EntryAutoTagger getAutoTagger() {
        return autoTagger;
    }

    public void setVerboseDeletions(boolean verboseDeletions) {
        this.verboseDeletions = verboseDeletions;
    }
    public boolean isVerboseDeletions() {
        return verboseDeletions;
    }

    public int getMaxLinkEntriesPerPage() {
        return maxLinkEntriesPerPage;
    }
    public void setMaxLinkEntriesPerPage( int maxLinkEntriesPerPage ) {
        this.maxLinkEntriesPerPage = maxLinkEntriesPerPage;
    }

    public int getMaxFullEntriesPerPage() {
        return maxFullEntriesPerPage;
    }
    public void setMaxFullEntriesPerPage( int maxFullEntriesPerPage ) {
        this.maxFullEntriesPerPage = maxFullEntriesPerPage;
    }

    public EntryIdGenerator getEntryIdGenerator() {
        return entryIdGenerator;
    }

    public void setEntryIdGenerator(EntryIdGenerator entryIdGenerator) {
        this.entryIdGenerator = entryIdGenerator;
    }
        
    //>>>>>>>>>>>>>>>>>>>>>>>>>>
    // DEPRECATED OPTIONS -- remove in 2.0.5
    /**
     * @deprecated
     */
    public void setCategoriesHandler( CategoriesHandler categoriesHandler ) {
        log.error("setAllowCategories() is deprecated, and does nothing");
    }
    //<<<<<<<<<<<<<
}
