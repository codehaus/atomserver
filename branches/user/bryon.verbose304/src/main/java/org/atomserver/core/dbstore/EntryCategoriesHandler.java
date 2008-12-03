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

package org.atomserver.core.dbstore;

import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Categories;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Document;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.protocol.server.ServiceContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import org.atomserver.*;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.WorkspaceOptions;
import org.atomserver.core.dbstore.dao.EntryCategoriesDAO;
import org.atomserver.core.dbstore.dao.EntryCategoryLogEventDAO;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.exceptions.BadRequestException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class EntryCategoriesHandler
        implements CategoriesHandler, ContentStorage {

    static private final Log log = LogFactory.getLog(EntryCategoriesHandler.class);
    static public final String DEFAULT_CATEGORIES_WORKSPACE_PREFIX = "tags:";
    
    static public final int DEFAULT_SIZE = 120;

    private AtomService atomService;
    private EntryCategoriesDAO entryCategoriesDAO;

    private boolean isLoggingAllCategoryEvents = false;
    private EntryCategoryLogEventDAO entryCategoryLogEventDAO;

    private int schemeSize = DEFAULT_SIZE;
    private int termSize = DEFAULT_SIZE;
    private int labelSize = DEFAULT_SIZE;

    // -----------------------------------
    public String getCategoriesWorkspaceName(String entriesWorkspaceName) {
        return DEFAULT_CATEGORIES_WORKSPACE_PREFIX + entriesWorkspaceName;
    }

    public AtomWorkspace newVirtualWorkspace(AtomService parentService, WorkspaceOptions options) {
        String catWorkspaceName = getCategoriesWorkspaceName(options.getName());
        log.debug( "CREATING CategoriesWorkspace= " + catWorkspaceName);

        AtomWorkspace catWorkspace = new DBBasedVirtualAtomWorkspace(parentService, catWorkspaceName);

        WorkspaceOptions catOptions = new WorkspaceOptions();
        catOptions.setName(catWorkspaceName);
        catOptions.setVisible(false);
        catOptions.setDefaultLocalized(options.getDefaultLocalized());
        catOptions.setAllowCategories(false);
        catOptions.setDefaultContentStorage(this);

        catWorkspace.setOptions(catOptions);

        setAtomService(parentService);

        return catWorkspace;
    }

    // -----------------------------------
    //                IOC
    // -----------------------------------
    /**
     * Note; this is set by the AtomService itself, not Spring
     * @param atomService
     */
    public void setAtomService(AtomService atomService) {
        this.atomService = atomService;
    }
    public AtomService getAtomService() {
        return atomService;
    }

    /**
     * Set by Spring
     */
    public void setEntryCategoriesDAO(EntryCategoriesDAO entryCategoriesDAO) {
        this.entryCategoriesDAO = entryCategoriesDAO;
    }
    public EntryCategoriesDAO getEntryCategoriesDAO() {
        return entryCategoriesDAO;
    }

    public boolean isLoggingAllCategoryEvents() {
        return isLoggingAllCategoryEvents;
    }
    public void setLoggingAllCategoryEvents(boolean loggingAllCategoryEvents) {
        isLoggingAllCategoryEvents = loggingAllCategoryEvents;
    }

    public EntryCategoryLogEventDAO getEntryCategoryLogEventDAO() {
        return entryCategoryLogEventDAO;
    }
    public void setEntryCategoryLogEventDAO(EntryCategoryLogEventDAO entryCategoryLogEventDAO) {
        this.entryCategoryLogEventDAO = entryCategoryLogEventDAO;
    }

    public int getSchemeSize() {
        return schemeSize;
    }
    public void setSchemeSize(int schemeSize) {
        this.schemeSize = schemeSize;
    }

    public int getTermSize() {
        return termSize;
    }
    public void setTermSize(int termSize) {
        this.termSize = termSize;
    }

    public int getLabelSize() {
        return labelSize;
    }
    public void setLabelSize(int labelSize) {
        this.labelSize = labelSize;
    }

    // -----------------------------------
    //        CategoriesHandler
    // -----------------------------------
    /**
     * Get a List of all Categories for this Workspace/Collection
     * <p/>
     * NOTE: The workspace entering this method is assumed to be a "regular" workspace
     * <p/>
     * i.e. NOT a "Categories workspace" (e.g. widgets vs. tags:widgets)
     */
    public List<Category> listCategories(String workspace, String collection) {
        if (log.isTraceEnabled()) {
            log.trace("EntryCategoriesContentStorage:: listCategories:: [" + workspace + ", " + collection + "]");
        }

        List schemeTermMapList = entryCategoriesDAO.selectDistictCategoriesPerCollection(workspace, collection);
        if (schemeTermMapList == null || schemeTermMapList.size() <= 0) {
            return null;
        }

        Factory factory = AtomServer.getFactory(getServiceContext().getAbdera());
        List<Category> categoryList = new ArrayList<Category>();
        for (Object obj : schemeTermMapList) {
            Map schemeTermMap = (Map) obj;

            Category category = factory.newCategory();

            category.setScheme((String) (schemeTermMap.get("scheme")));
            category.setTerm((String) (schemeTermMap.get("term")));
            // FIXME: deal w/ labels

            categoryList.add(category);
        }
        return categoryList;
    }

    /* used only by obliterateEntry
    */
    public void deleteEntryCategories(EntryDescriptor entryQuery){
        entryCategoriesDAO.deleteEntryCategories(entryQuery);
    }

    public List<EntryCategory> selectEntryCategories(EntryDescriptor entryQuery){
        return entryCategoriesDAO.selectEntryCategories(entryQuery);
    }

    public List<EntryCategory> selectEntriesCategories(String workspace, String collection, Set<String> entryIds){
        return entryCategoriesDAO.selectEntriesCategories(workspace, collection, entryIds);
    }

    public void insertEntryCategoryBatch(List<EntryCategory> entryCatList) {
        for( EntryCategory category: entryCatList ){
            verifyEntryCategory( category );
        }

        entryCategoriesDAO.insertEntryCategoryBatch(entryCatList);

        if (isLoggingAllCategoryEvents) {
            entryCategoryLogEventDAO.insertEntryCategoryLogEventBatch(entryCatList);
        }
    }

    public void deleteEntryCategoryBatch(List<EntryCategory> entryCategoryList) {
        for( EntryCategory category: entryCategoryList ){
            verifyEntryCategory( category );
        }
        entryCategoriesDAO.deleteEntryCategoryBatch(entryCategoryList);
    }    

    // -----------------------------------
    //           ContentStorage
    // -----------------------------------
    public void initializeWorkspace(String workspace) {
        // do nothing.
    }

    public void testAvailability() {
        entryCategoriesDAO.selectSysDate();
    }

    /**
     * {@inheritDoc}
     */
    public String getContent(EntryDescriptor descriptor) {
        if (log.isTraceEnabled()) {
            log.trace("EntryCategoriesContentStorage:: getEntryData:: [" + descriptor + "]");
        }

        try {
            EntryDescriptor descriptorClone = cloneDescriptorWithEntriesWorkspace(descriptor);
            Categories categories = getCategories(descriptorClone);

            String categoriesXML;
            if (categories == null) {
                categoriesXML =
                        "<categories xmlns=\"http://www.w3.org/2007/app\" " +
                        "xmlns:atom=\"http://www.w3.org/2005/Atom\"></categories>";
            } else {
                StringWriter stringWriter = new StringWriter();
                categories.writeTo(stringWriter);
                categoriesXML = stringWriter.toString();
            }
            return categoriesXML;
        } catch (IOException ee) {
            throw new AtomServerException(ee);
        }
    }

    public void deleteContent(String deletedContentXml, EntryDescriptor descriptor) {
        // in the case of categories, obliteration and deletion are the same;
        obliterateContent(descriptor);
    }

    public void obliterateContent(EntryDescriptor descriptor) {
        if (log.isTraceEnabled()) {
            log.trace("EntryCategoriesContentStorage:: deleteEntry:: [" + descriptor + "]");
        }

        EntryDescriptor descriptorClone = cloneDescriptorWithEntriesWorkspace(descriptor);
        deleteCategories(descriptorClone);

        getAffliatedContentStorage(descriptor).revisionChangedWithoutContentChanging(descriptorClone);
    }

    /**
     * {@inheritDoc}
     * NOTE: putContent for EntryCategoriesHandler always first DELETEs all Categories, and then
     * INSERTs any Categories in the contentXML
     */
    public void putContent(String contentXml, EntryDescriptor descriptor) {
        if (log.isTraceEnabled()) {
            log.trace("EntryCategoriesContentStorage:: putEntry:: [" + descriptor + "]");
            log.trace("EntryCategoriesContentStorage:: putEntry:: contentXml= [" + contentXml + "]");
        }
        EntryDescriptor descriptorClone = cloneDescriptorWithEntriesWorkspace(descriptor);

        // DELETE all current Categories from the DB
        deleteCategories(descriptorClone);

        // INSERT the input Categories to the DB
        insertCategories(contentXml, descriptorClone, true);

        getAffliatedContentStorage(descriptorClone).revisionChangedWithoutContentChanging(descriptorClone);
    }

    public boolean canRead() {
        try {
            testAvailability();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean contentExists(EntryDescriptor descriptor) {
        List categoryList = entryCategoriesDAO.selectEntryCategories(descriptor);
        return !(categoryList == null || categoryList.size() <= 0);
    }

    public void revisionChangedWithoutContentChanging(EntryDescriptor descriptor) {
        // nothing to do for this case
    }

    public Object getPhysicalRepresentation(String workspace,
                                            String collection,
                                            String entryId,
                                            Locale locale,
                                            int revision) {
        throw new UnsupportedOperationException("not implemented");
    }

    // -----------------------------------
    //            Support Code
    // -----------------------------------
    private ServiceContext getServiceContext() {
        return ((DBBasedAtomService) atomService).getServiceContext();
    }

    private ContentStorage getAffliatedContentStorage(EntryDescriptor descriptor) {
        String workspace = descriptor.getWorkspace();
        return atomService.getAtomWorkspace(workspace).getOptions().getDefaultContentStorage();
    }

    private String getEntriesWorkspace(String categoriesWorkspaceName) {
        return DBBasedVirtualAtomWorkspace.getEntriesWorkspaceName(categoriesWorkspaceName);
    }

    /**
     * Clone the descriptor and substitute the Categories workspace
     */
    private EntryDescriptor cloneDescriptorWithEntriesWorkspace(final EntryDescriptor descriptor) {
        return new EntryDescriptor() {
            public String getWorkspace() {
                String mappedWorkspace = getEntriesWorkspace(descriptor.getWorkspace());
                return mappedWorkspace == null ? descriptor.getWorkspace() : mappedWorkspace;
            }

            public String getCollection() {
                return descriptor.getCollection();
            }

            public String getEntryId() {
                return descriptor.getEntryId();
            }

            public Locale getLocale() {
                return descriptor.getLocale();
            }

            public int getRevision() {
                return descriptor.getRevision();
            }

            public String toString() {
                return new StringBuffer()
                    .append("EntryDescriptor:: [")
                    .append(getWorkspace())
                    .append(" ").append(getCollection())
                    .append(" ").append(getEntryId())
                    .append(" ").append(getLocale())
                    .append(" ").append(getRevision())
                    .append("]").toString();
            }           
        };
    }

    /**
     * This method uses whatever workspace is in the descriptor you pass in
     * NOTE: the Categories are stored as Entries using the "tags:workspace" scheme (e.g. "tags:widgets")
     * But are stored in the EntryCategories table with the unadorned "workspace" (e.g. "widgets")
     * so that we can do proper SELECTs against the actual workspace Entries (e.g. "widgets")
     */
    private Categories getCategories(EntryDescriptor descriptor) {
        if (log.isTraceEnabled()) {
            log.trace("EntryCategoriesContentStorage:: getCategories:: [" + descriptor + "]");
        }

        List categoryList = entryCategoriesDAO.selectEntryCategories(descriptor);
        if (categoryList == null || categoryList.size() <= 0) {
            return null;
        }

        Factory factory = AtomServer.getFactory(getServiceContext().getAbdera());
        Categories categories = factory.newCategories();

        for (Object obj : categoryList) {
            EntryCategory entryCategory = (EntryCategory) obj;

            Category category = factory.newCategory();
            category.setScheme(entryCategory.getScheme());
            category.setTerm(entryCategory.getTerm());
            category.setLabel(entryCategory.getLabel());

            categories.addCategory(category);
        }
        return categories;
    }

    /**
     * This method uses whatever workspace is in the descriptor you pass in
     * NOTE: the Categories are stored as Entries using the "tags:workspace" scheme (e.g. "tags:widgets")
     * But are stored in the EntryCategories table with the unadorned "workspace" (e.g. "widgets")
     * so that we can do proper SELECTs against the actual workspace Entries (e.g. "widgets")
     */
    private void deleteCategories(EntryDescriptor descriptor) {
        if (log.isTraceEnabled()) {
            log.trace("EntryCategoriesContentStorage:: deleteCategories:: [" + descriptor + "]");
        }

        Categories categoriesToDelete = getCategories(descriptor);
        if (categoriesToDelete == null) {
            return;
        }

        List<Category> categoryList = categoriesToDelete.getCategories();
        if (categoryList == null) {
            return;
        }

        List<EntryCategory> entryCatList = new ArrayList<EntryCategory>(categoryList.size());
        for (Category category : categoryList) {
            EntryCategory entryIn = new EntryCategory();
            if (descriptor instanceof EntryMetaData) {
                entryIn.setEntryStoreId(((EntryMetaData) descriptor).getEntryStoreId());
            } else {
                entryIn.setWorkspace(descriptor.getWorkspace());
                entryIn.setCollection(descriptor.getCollection());
                entryIn.setEntryId(descriptor.getEntryId());
                entryIn.setLocale(descriptor.getLocale());
            }
            entryIn.setScheme(category.getScheme().toString());
            entryIn.setTerm(category.getTerm());

            entryCatList.add(entryIn);
        }

        // BATCH DELETE
        entryCategoriesDAO.deleteEntryCategoryBatch(entryCatList);
    }

    /**
     * This method uses whatever workspace is in the descriptor you pass in
     * NOTE: the Categories are stored as Entries using the "tags:workspace" scheme (e.g. "tags:widgets")
     * But are stored in the EntryCategories table with the unadorned "workspace" (e.g. "widgets")
     * so that we can do proper SELECTs against the actual workspace Entries (e.g. "widgets")
     */
    private void insertCategories(String contentXml, EntryDescriptor descriptor, boolean wasDeletedFirst) {
        if (!wasDeletedFirst) {
            String msg = "Requires that all Records be deleted first";
            log.error(msg);
            throw new AtomServerException(msg);
        }

        Parser parser = getServiceContext().getAbdera().getParser();
        Document<Categories> doc = parser.parse(new StringReader(contentXml));

        Categories categories = doc.getRoot();
        List<Category> categoryList = categories.getCategories();
        if (categoryList == null) {
            String msg = "A Category List is NULL for entry= " + descriptor;
            log.error(msg);
            throw new BadRequestException(msg);
        }

        List<EntryCategory> entryCatList = getEntryCategoryList(descriptor, categoryList);

        // BATCH INSERT
        insertEntryCategoryBatch(entryCatList);
    }

    /**
     */
    private List<EntryCategory> getEntryCategoryList(EntryDescriptor descriptor, List<Category> categoryList) {
        List<EntryCategory> entryCatList = new ArrayList<EntryCategory>(categoryList.size());

        for (Category category : categoryList) {
            EntryCategory entryIn = new EntryCategory();

            if (descriptor instanceof EntryMetaData) {
                entryIn.setEntryStoreId(((EntryMetaData) descriptor).getEntryStoreId());
            } else {
                String workspace = descriptor.getWorkspace();
                if (workspace == null || workspace.trim().equals("")) {
                    String msg = "A Category MUST be defined within a workspace. The Category [" + category +
                                 "] was not properly formatted";
                    log.error(msg);
                    throw new BadRequestException(msg);
                }
                entryIn.setWorkspace(workspace);

                String collection = descriptor.getCollection();
                if (collection == null || collection.trim().equals("")) {
                    String msg = "A Category MUST be defined within a collection. The Category [" + category +
                                 "] was not properly formatted";
                    log.error(msg);
                    throw new BadRequestException(msg);
                }
                entryIn.setCollection(collection);

                String entryId = descriptor.getEntryId();
                if (entryId == null || entryId.trim().equals("")) {
                    String msg = "A Category MUST be defined for an entryId. The Category [" + category +
                                 "] was not properly formatted";
                    log.error(msg);
                    throw new BadRequestException(msg);
                }
                entryIn.setEntryId(entryId);

                entryIn.setLocale(descriptor.getLocale());
            }

            if ( ! StringUtils.isEmpty(category.getScheme().toString()) ) {
                entryIn.setScheme(category.getScheme().toString().trim());
            }

            if ( ! StringUtils.isEmpty(category.getTerm()) ) {
                entryIn.setTerm(category.getTerm().trim());
            }

            if ( ! StringUtils.isEmpty(category.getLabel()) ) {
                entryIn.setLabel(category.getLabel().trim());
            }

            if (log.isTraceEnabled()) {
                log.trace("EntryCategoriesContentStorage:: entryIn:: [" + entryIn + "]");
            }
            entryCatList.add(entryIn);
        }
        return entryCatList;
    }

    private void verifyEntryCategory( EntryCategory category ) {

        String scheme = category.getScheme();
        if (scheme == null || scheme.trim().equals("")) {
            String msg = "A Category MUST have a scheme attached. The Category [" + category +
                         "] was not properly formatted";
            log.error(msg);
            throw new BadRequestException(msg);
        }
        if (scheme.length() > schemeSize) {
            String msg = "A Category SCHEME must NOT be longer than " + schemeSize +
                         "characters. The Category [" + category + "] was not properly formatted";
            log.error(msg);
            throw new BadRequestException(msg);
        }

        String term = category.getTerm();
        if (term == null || term.trim().equals("")) {
            String msg = "A Category MUST have a term attached. The Category [" + category +
                         "] was not properly formatted";
            log.error(msg);
            throw new BadRequestException(msg);
        }
        if (term.length() > termSize) {
            String msg = "A Category TERM must NOT be longer than " + termSize +
                         "characters. The Category [" + category + "] was not properly formatted";
            log.error(msg);
            throw new BadRequestException(msg);
        }

        String label = category.getLabel();
        if (!StringUtils.isEmpty(label) && label.length() > labelSize) {
            String msg = "A Category LABEL must NOT be longer than " + labelSize +
                         "characters. The Category [" + category + "] was not properly formatted";
            log.error(msg);
            throw new BadRequestException(msg);
        }
    }

    //>>>>>>>>>>>>>>>>>>>>>>>>>>
    // DEPRECATED OPTIONS -- remove in 2.0.5
    /**
     * @deprecated
     */
    public void setServiceContext(ServiceContext serviceContext) {
        log.error("setServiceContext is DEPRECATED, and no longer used");
    }

    /**
     * @deprecated
     */
    public void setRealContentStorage(ContentStorage realContentStorage) {
        log.error("setRealContentStorage is DEPRECATED, and no longer used");
    }

    /**
     * @deprecated
     */
    public void setCategoriesToEntriesMap(java.util.Map<String, String> categoriesToEntriesMap) {
        log.error("setCategoriesToEntriesMap() is DEPRECATED, and no longer used");
    }
    // <<<<<<<<<<<<<<<<<<<<<<<<<
}
