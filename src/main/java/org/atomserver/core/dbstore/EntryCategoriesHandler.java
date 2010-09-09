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
import org.atomserver.core.dbstore.dao.*;
import org.atomserver.core.dbstore.dao.impl.CategoriesDAOiBatisImpl;
import org.atomserver.core.dbstore.dao.impl.CategoryLogEventsDAOiBatisImpl;
import org.atomserver.core.dbstore.dao.rwdao.WriteReadCategoriesDAO;
import org.atomserver.core.dbstore.dao.rwdao.WriteReadCategoryLogEventsDAO;
import org.atomserver.ext.category.CategoryOperation;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.WorkspaceOptions;
import org.atomserver.core.etc.AtomServerConstants;
import org.atomserver.core.dbstore.utils.SizeLimit;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.exceptions.BadRequestException;
import org.atomserver.exceptions.OptimisticConcurrencyException;

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
    
    private AtomService atomService;
    private CategoriesDAO categoriesDAO;

    private boolean isLoggingAllCategoryEvents = false;
    private CategoryLogEventsDAO categoryLogEventsDAO;

    private SizeLimit sizeLimit = null;

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

    public WriteReadCategoriesDAO getWriteReadCategoriesDAO() {
         return ((CategoriesDAOiBatisImpl)categoriesDAO).getWriteReadCategoriesDAO();
    }

    public WriteReadCategoryLogEventsDAO getWriteReadCategoryLogEventsDAO() {
         return ((CategoryLogEventsDAOiBatisImpl)categoryLogEventsDAO).getWriteReadEntryCategoryLogEventDAO();
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
    public void setEntryCategoriesDAO(CategoriesDAO categoriesDAO) {
        this.categoriesDAO = categoriesDAO;
    }
    public CategoriesDAO getEntryCategoriesDAO() {
        return categoriesDAO;
    }

    public boolean isLoggingAllCategoryEvents() {
        return isLoggingAllCategoryEvents;
    }
    public void setLoggingAllCategoryEvents(boolean loggingAllCategoryEvents) {
        isLoggingAllCategoryEvents = loggingAllCategoryEvents;
    }

    public CategoryLogEventsDAO getEntryCategoryLogEventDAO() {
        return categoryLogEventsDAO;
    }
    public void setEntryCategoryLogEventDAO(CategoryLogEventsDAO categoryLogEventsDAO) {
        this.categoryLogEventsDAO = categoryLogEventsDAO;
    }

    /**
     * Get the size limit settings
     */
    public SizeLimit getSizeLimit() {
        return sizeLimit;
    }

    /**
     * Set the size limit settings
     */
    public void setSizeLimit(SizeLimit sizeLimit) {
        this.sizeLimit = sizeLimit;
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

        List schemeTermMapList = categoriesDAO.selectDistictCategoriesPerCollection(workspace, collection);
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
        getWriteReadCategoriesDAO().deleteEntryCategories(entryQuery);
    }

    // USED BY THE AUTOTAGGER
    public List<EntryCategory> selectEntryCategories(EntryDescriptor entryQuery){
        return getWriteReadCategoriesDAO().selectEntryCategories(entryQuery);
    }

    public List<EntryCategory> selectEntriesCategories(String workspace, String collection, Set<String> entryIds){
        return categoriesDAO.selectEntriesCategories(workspace, collection, entryIds);
    }

    public void insertEntryCategoryBatch(List<EntryCategory> entryCatList) {
        for( EntryCategory category: entryCatList ){
            verifyEntryCategory( category );
        }

        getWriteReadCategoriesDAO().insertEntryCategoryBatch(entryCatList);

        if (isLoggingAllCategoryEvents) {
            getWriteReadCategoryLogEventsDAO().insertEntryCategoryLogEventBatch(entryCatList);
        }
    }

    public void deleteEntryCategoryBatch(List<EntryCategory> entryCategoryList) {
        // NOTE: we need to be able to delete Categories that may be bad, so do NOT verify here
        getWriteReadCategoriesDAO().deleteEntryCategoryBatch(entryCategoryList);
    }    

    // -----------------------------------
    //           ContentStorage
    // -----------------------------------
    public void initializeWorkspace(String workspace) {
        // do nothing.
    }

    public void testAvailability() {
        categoriesDAO.selectSysDate();
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
            log.info("EntryCategoriesContentStorage:: putEntry:: [" + descriptor + "]");
            log.info("EntryCategoriesContentStorage:: putEntry:: contentXml= [" + contentXml + "]");
        }
        EntryDescriptor descriptorClone = cloneDescriptorWithEntriesWorkspace(descriptor);

        Categories categories = getCategoriesFromContent(contentXml, descriptor);

        CategoryOperation categoryOp = categories.getExtension(AtomServerConstants.CATEGORY_OP);

        if( categoryOp == null) {

            // DELETE all current Categories from the DB
            deleteCategories(descriptorClone);
            // INSERT the input Categories to the DB
            insertCategories(categories, descriptorClone, true);
            
        } else {
            // make sure the operations are valid.
            validateCategoryOperation(categoryOp, categories);
            // process each category document based on the category operation.
            handleCategoryOperation(categoryOp, categories, descriptor);

        }

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
        List categoryList = categoriesDAO.selectEntryCategories(descriptor);
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

            public String getContentHashCode() {
                return descriptor.getContentHashCode();
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

        List categoryList = getWriteReadCategoriesDAO().selectEntryCategories(descriptor);
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

        deleteCategoriesBatch(categoriesToDelete, descriptor);
    }

    private void deleteCategoriesBatch(Categories categoriesToDelete, EntryDescriptor descriptor) {
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
        getWriteReadCategoriesDAO().deleteEntryCategoryBatch(entryCatList);
    }

    /**
     * This method uses whatever workspace is in the descriptor you pass in
     * NOTE: the Categories are stored as Entries using the "tags:workspace" scheme (e.g. "tags:widgets")
     * But are stored in the EntryCategories table with the unadorned "workspace" (e.g. "widgets")
     * so that we can do proper SELECTs against the actual workspace Entries (e.g. "widgets")
     */
    private void insertCategories(Categories categories, EntryDescriptor descriptor, boolean wasDeletedFirst) {
        if (!wasDeletedFirst) {
            String msg = "Requires that all Records be deleted first";
            log.error(msg);
            throw new AtomServerException(msg);
        }

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

    private int updateCategory(Category cat, String oldTerm, EntryDescriptor descriptor){
        List<Category> catList = new ArrayList<Category>();
        catList.add(cat);
        List<EntryCategory> categoryList = getEntryCategoryList(descriptor, catList);
        int rc = getWriteReadCategoriesDAO().updateEntryCategory(categoryList.get(0), oldTerm );

        if (isLoggingAllCategoryEvents) {
            getWriteReadCategoryLogEventsDAO().insertEntryCategoryLogEventBatch(categoryList);
        }
        return rc;
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
        if (sizeLimit != null && !sizeLimit.isValidScheme(scheme)) {
            String msg = "A Category SCHEME must NOT be longer than " + sizeLimit.getSchemeSize() +
                         " characters. The Category [" + category + "] was not properly formatted";
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
        if (sizeLimit != null && !sizeLimit.isValidTerm(term)) {
            String msg = "A Category TERM must NOT be longer than " + sizeLimit.getTermSize() +
                         " characters. The Category [" + category + "] was not properly formatted";
            log.error(msg);
            throw new BadRequestException(msg);
        }

        String label = category.getLabel();
        if (!StringUtils.isEmpty(label) && sizeLimit != null && !sizeLimit.isValidLabel(label)) {
            String msg = "A Category LABEL must NOT be longer than " + sizeLimit.getLabelSize() +
                         " characters. The Category [" + category + "] was not properly formatted";
            log.error(msg);
            throw new BadRequestException(msg);
        }
    }

    private Categories getCategoriesFromContent(String contentXml, EntryDescriptor descriptor) {
        Parser parser = getServiceContext().getAbdera().getParser();
        Document<Categories> doc = parser.parse(new StringReader(contentXml));
        Categories categories = doc.getRoot();
        List<Category> categoryList = categories.getCategories();
        if (categoryList == null) {
            String msg = "A Category List is NULL for entry= " + descriptor;
            log.error(msg);
            throw new BadRequestException(msg);
        }
        return categories;
    }

    private CategoryOperation validateCategoryOperation(CategoryOperation op, Categories categories) {

        if (!(op.getType().equals(CategoryOperation.MODIFY))) {
            throw new BadRequestException(" Invalid Category Operation");
        }
        List<Category> catList = categories.getCategories();
        if(catList.size() != 1) {
            String msg = "Cannot have more than one category to insert, update or delete.";
            log.error(msg);
            throw new BadRequestException(msg);
        }
        Category category = catList.get(0);
        String modifyType = category.getAttributeValue(AtomServerConstants.CATEGORY_OP_ATTR_MODIFYTYPE);
        if(modifyType == null) {
            String msg = "Category does not specify modification type in its attributes.";
            log.error(msg);
            throw new BadRequestException(msg);
        }
        if (CategoryOperation.UPDATE.equals(modifyType)) {
            String oldTerm = category.getAttributeValue(AtomServerConstants.CATEGORY_OP_ATTR_OLD_TERM);
            if (oldTerm == null) {
                String msg = "Category does not specify old term to update in its attributes.";
                log.error(msg);
                throw new BadRequestException(msg);
            }
        } 
        return op;
    }

    private void handleCategoryOperation(CategoryOperation op, Categories categories, EntryDescriptor descriptor) {

        // TODO: need to handle error condition as a group. It works currently beause there is 1 category only.
        for(Category category: categories.getCategories()) {

            String modifyType = category.getAttributeValue(AtomServerConstants.CATEGORY_OP_ATTR_MODIFYTYPE);

            if (CategoryOperation.INSERT.equals(modifyType)) {
                Categories existingCategories = getCategories(descriptor);
                if (existingCategories != null) {
                    for (Category cat : existingCategories.getCategories()) {
                        if (cat.getScheme().equals(category.getScheme()) && (cat.getTerm().equals(category.getTerm()))) {
                            String msg = "Error: Attempting to insert an existing category";
                            log.error(msg);
                            throw new BadRequestException(msg);
                        }
                    }
                }
                List<EntryCategory> entryCatList = getEntryCategoryList(descriptor, categories.getCategories());
                insertEntryCategoryBatch(entryCatList);
            } else if (CategoryOperation.DELETE.equals(modifyType)) {
                // go ahead and delete categories
                deleteCategoriesBatch(categories, descriptor);

            } else if (CategoryOperation.UPDATE.equals(modifyType)) {

                String oldTerm = category.getAttributeValue(AtomServerConstants.CATEGORY_OP_ATTR_OLD_TERM);
                int rc = updateCategory(category, oldTerm, descriptor);
                // It should return 1, otherwise something is wrong.
                if (rc != 1) {
                    boolean matchedSchemeExists = false;
                    boolean categoryToUpdateFound = false;
                    Categories existingCategories = getCategories(descriptor);
                    if (existingCategories != null) {
                        for (Category cat : existingCategories.getCategories()) {
                            boolean matchedScheme = cat.getScheme().equals(category.getScheme());
                            if (!matchedSchemeExists && matchedScheme) {
                                matchedSchemeExists = true;
                            }
                            boolean matchedTerm = cat.getTerm().equals(category.getTerm());
                            if (matchedScheme && matchedTerm) {
                                categoryToUpdateFound = true;
                                break;
                            }
                        }
                    }
                    if (!categoryToUpdateFound) {
                        if (matchedSchemeExists) {
                            String editURI = atomService.getURIHandler().constructURIString(descriptor.getWorkspace(),
                                                                                            descriptor.getCollection(),
                                                                                            descriptor.getEntryId(),
                                                                                            descriptor.getLocale(),
                                                                                            (descriptor.getRevision() + 1));
                            String msg = "Optimistic Concurrency Exception in category update operation at " + editURI;
                            log.error(msg);
                            // If the scheme match but not the term, this is an optimistic concurrency exception.
                            throw new OptimisticConcurrencyException("Optimistic Concurrency Exception in category update operation", editURI);

                        } else {
                            throw new BadRequestException("Category to update not found");
                        }
                    }
                }
            }
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
