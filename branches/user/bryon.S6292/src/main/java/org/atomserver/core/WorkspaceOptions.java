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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * WorkspaceOptions - The options which may be set for an AtomWorkspace.
 * This class is meant to be configured from by an IOC container like Spring, and then wired into an AtomWorkspace
 * to configure it. This level of indirection allows AtomServer to cleanly separate configuration
 * from the AtomWorkspace API, which allows Users to supply their own versions or overrides to the properties
 * defined herein.
 * <p/>
 * NOTE: WorkspaceOptions may be used to provide defaults to the CollectionOptions.
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class WorkspaceOptions {
    static private final Log log = LogFactory.getLog(WorkspaceOptions.class);

    static public int DEFAULT_MAX_LINK_ENTRIES_PER_PAGE = 100;
    static public int DEFAULT_MAX_FULL_ENTRIES_PER_PAGE = 15;

    private String name = null;

    private boolean localized = false;
    private boolean visible = true;
    private boolean verboseDeletions = true;

    private boolean producingTotalResultsFeedElement = false;
    private boolean producingEntryCategoriesFeedElement = false;

    private ContentStorage defaultContentStorage = null;
    private ContentValidator defaultContentValidator = null;

    private EntryAutoTagger defaultAutoTagger = null;

    private EntryIdGenerator entryIdGenerator = null;

    private Set<String> allowedVirtualWorkspaces = 
            new HashSet<String>(Arrays.asList(VirtualWorkspaceHandler.CATEGORIES));

    private int maxLinkEntriesPerPage = DEFAULT_MAX_LINK_ENTRIES_PER_PAGE;
    private int maxFullEntriesPerPage = DEFAULT_MAX_FULL_ENTRIES_PER_PAGE;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getAllowedVirtualWorkspaces() {
        return allowedVirtualWorkspaces;
    }

    public void setDefaultLocalized(boolean localized) {
        this.localized = localized;
    }

    public boolean getDefaultLocalized() {
        return localized;
    }

    public void setDefaultProducingTotalResultsFeedElement(boolean producingTotalResultsFeedElement) {
        this.producingTotalResultsFeedElement = producingTotalResultsFeedElement;
    }

    public boolean getDefaultProducingTotalResultsFeedElement() {
        return producingTotalResultsFeedElement;
    }

    public void setDefaultProducingEntryCategoriesFeedElement(boolean producingEntryCategoriesFeedElement) {
        this.producingEntryCategoriesFeedElement = producingEntryCategoriesFeedElement;
    }

    public boolean getDefaultProducingEntryCategoriesFeedElement() {
        return producingEntryCategoriesFeedElement;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setDefaultContentStorage(ContentStorage contentStorage) {
        this.defaultContentStorage = contentStorage;
        this.defaultContentStorage.initializeWorkspace(getName());
    }

    public ContentStorage getDefaultContentStorage() {
        return this.defaultContentStorage;
    }

    public void setDefaultContentValidator(ContentValidator contentValidator) {
        this.defaultContentValidator = contentValidator;
    }

    public ContentValidator getDefaultContentValidator() {
        return defaultContentValidator;
    }

    public void setDefaultAutoTagger(EntryAutoTagger autoTagger) {
        this.defaultAutoTagger = autoTagger;
    }

    public EntryAutoTagger getDefaultAutoTagger() {
        return defaultAutoTagger;
    }

    public void setDefaultVerboseDeletions(boolean verboseDeletions) {
        this.verboseDeletions = verboseDeletions;
    }

    public boolean getDefaultVerboseDeletions() {
        return verboseDeletions;
    }

    public int getDefaultMaxLinkEntriesPerPage() {
        return maxLinkEntriesPerPage;
    }

    public void setDefaultMaxLinkEntriesPerPage(int maxLinkEntriesPerPage) {
        this.maxLinkEntriesPerPage = maxLinkEntriesPerPage;
    }

    public int getDefaultMaxFullEntriesPerPage() {
        return maxFullEntriesPerPage;
    }

    public void setDefaultMaxFullEntriesPerPage(int maxFullEntriesPerPage) {
        this.maxFullEntriesPerPage = maxFullEntriesPerPage;
    }

    public EntryIdGenerator getDefaultEntryIdGenerator() {
        return entryIdGenerator;
    }

    public void setDefaultEntryIdGenerator(EntryIdGenerator entryIdGenerator) {
        this.entryIdGenerator = entryIdGenerator;
    }

    //>>>>>>>>>>>>>>>>>>>>>>>>>>
    // DEPRECATED OPTIONS -- remove in 2.0.5
    public void setDefaultCategoriesHandler(CategoriesHandler categoriesHandler) {
        log.error("setDefaultCategoriesHandler() is deprecated, and does nothing");
    }

    public void setIsCategoryWorkspace(boolean categoryWorkspace) {
        log.error("setIsCategoryWorkspace() is deprecated, and does nothing");
    }

    public void setCategoryWorkspaceName(String categoryWorkspaceName) {
        log.error("setCategoryWorkspaceName() is deprecated, and does nothing");
    }

    public void setAllowCategories(boolean allowCategories) {
        log.error("setAllowCategories() is deprecated, and does nothing");
    }
    //<<<<<<<<<<<<<<<<<<<<
}