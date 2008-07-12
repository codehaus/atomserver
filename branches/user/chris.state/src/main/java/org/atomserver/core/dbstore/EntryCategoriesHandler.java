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
import org.atomserver.*;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.dbstore.dao.EntryCategoriesDAO;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.exceptions.BadRequestException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class EntryCategoriesHandler
        implements CategoriesHandler, ContentStorage {

    static private final Log log = LogFactory.getLog(EntryCategoriesHandler.class);

    private AtomService atomService;
    private EntryCategoriesDAO entryCategoriesDAO = null;
    private java.util.Map<String, String> categoriesToEntriesMap = null;

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

    public void setCategoriesToEntriesMap(java.util.Map<String, String> categoriesToEntriesMap) {
        this.categoriesToEntriesMap = categoriesToEntriesMap;
    }

    /**
     * Used by Spring
     */
    public void setEntryCategoriesDAO(EntryCategoriesDAO entryCategoriesDAO) {
        this.entryCategoriesDAO = entryCategoriesDAO;
    }
    public EntryCategoriesDAO getEntryCategoriesDAO() {
        return entryCategoriesDAO;
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

    /**
     * {@inheritDoc}
     */
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
        String entriesWorkspaceName = null;
        if (categoriesToEntriesMap != null) {
            entriesWorkspaceName = categoriesToEntriesMap.get(categoriesWorkspaceName);
        }

        if (log.isDebugEnabled()) {
            log.debug("workspace[ " + categoriesWorkspaceName + " ] maps to [ " + entriesWorkspaceName + " ]");
        }
        return entriesWorkspaceName;
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
        entryCategoriesDAO.insertEntryCategoryBatch(entryCatList);
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
                    String msg = "A Category MUST be defined for a workspace. The Category [" + category +
                                 "] was not properly formatted";
                    log.error(msg);
                    throw new BadRequestException(msg);
                }
                entryIn.setWorkspace(workspace);

                String collection = descriptor.getCollection();
                if (collection == null || collection.trim().equals("")) {
                    String msg = "A Category MUST be defined for a collection. The Category [" + category +
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

            String scheme = category.getScheme().toString();
            if (scheme == null || scheme.trim().equals("")) {
                String msg = "A Category MUST have a scheme attached. The Category [" + category +
                             "] was not properly formatted";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            entryIn.setScheme(scheme);

            String term = category.getTerm();
            if (term == null || term.trim().equals("")) {
                String msg = "A Category MUST have a term attached. The Category [" + category +
                             "] was not properly formatted";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            entryIn.setTerm(term);

            entryIn.setLabel(category.getLabel());

            if (log.isTraceEnabled()) {
                log.trace("EntryCategoriesContentStorage:: entryIn:: [" + entryIn + "]");
            }
            entryCatList.add(entryIn);
        }
        return entryCatList;
    }

    //>>>>>>>>>>>>>>>>>>>>>>>>>>
    // DEPRECATED OPTIONS -- remove in 2.0.5
    public void setServiceContext(ServiceContext serviceContext) {
        log.error("setServiceContext is DEPRICATED, and no longer used");
    }

    public void setRealContentStorage(ContentStorage realContentStorage) {
        log.error("setRealContentStorage is DEPRICATED, and no longer used");
    }
    // <<<<<<<<<<<<<<<<<<<<<<<<<
}
