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

package org.atomserver.core.dbstore.dao.impl;

import org.atomserver.AtomCategory;
import org.atomserver.EntryDescriptor;
import org.atomserver.FeedDescriptor;
import org.atomserver.ServiceDescriptor;
import org.atomserver.core.AggregateEntryMetaData;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.dbstore.dao.ContentDAO;
import org.atomserver.core.dbstore.dao.EntriesDAO;
import org.atomserver.core.dbstore.dao.CategoriesDAO;
import org.atomserver.core.dbstore.dao.CategoryLogEventsDAO;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.utils.logic.BooleanExpression;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
@ManagedResource(description = "EntriesDAO")
public class EntriesDAOiBatisImpl
        extends AbstractDAOiBatisImplDelegator
        implements EntriesDAO, InitializingBean {

    static public final int UNDEFINED = -1;

    private WriteReadEntriesDAOiBatisImpl writeEntriesDAO;
    private ReadEntriesDAOiBatisImpl readEntriesDAO;
    private AggregateEntriesDAOiBatisImpl aggregateEntriesDAO;

    private ContentDAO contentDAO;
    private CategoriesDAO categoriesDAO;
    private CategoryLogEventsDAO categoryLogEventsDAO;

    private int latencySeconds = UNDEFINED;

    public void afterPropertiesSet() throws Exception {
        if (dataSource != null) {
            if (writeEntriesDAO == null) {
                writeEntriesDAO = new WriteReadEntriesDAOiBatisImpl();
                setupDAO(writeEntriesDAO);
            }
            if (readEntriesDAO == null) {
                readEntriesDAO = new ReadEntriesDAOiBatisImpl();
                setupDAO(readEntriesDAO);
            }
            if (aggregateEntriesDAO == null) {
                aggregateEntriesDAO = new AggregateEntriesDAOiBatisImpl();
                setupDAO(aggregateEntriesDAO);
            }
        }
    }

    public AbstractDAOiBatisImpl getReadDAO() { return readEntriesDAO; }

    public WriteReadEntriesDAOiBatisImpl getWriteEntriesDAO() { return writeEntriesDAO; }

    public void setWriteEntriesDAO(WriteReadEntriesDAOiBatisImpl writeEntriesDAO) { this.writeEntriesDAO = writeEntriesDAO; }

    public ReadEntriesDAOiBatisImpl getReadEntriesDAO() { return readEntriesDAO; }

    public void setReadEntriesDAO(ReadEntriesDAOiBatisImpl readEntriesDAO) { this.readEntriesDAO = readEntriesDAO; }

    public AggregateEntriesDAOiBatisImpl getAggregateEntriesDAO() { return aggregateEntriesDAO; }

    public void setAggregateEntriesDAO(AggregateEntriesDAOiBatisImpl aggregateEntriesDAO) { this.aggregateEntriesDAO = aggregateEntriesDAO; }

    private void setupDAO(BaseEntriesDAOiBatisImpl dao) {
        dao.setContentDAO(contentDAO);
        dao.setCategoriesDAO(categoriesDAO);
        dao.setCategoryLogEventsDAO(categoryLogEventsDAO);
        dao.setLatencySeconds(latencySeconds);
        dao.setSqlMapClient(sqlMapClient);
        dao.setDatabaseType(dbType);
        dao.setDataSource(dataSource);
        dao.afterPropertiesSet();
    }

    public void setContentDAO(ContentDAO contentDAO) { this.contentDAO = contentDAO; }

    public void setEntryCategoriesDAO(CategoriesDAO categoriesDAO) { this.categoriesDAO = categoriesDAO; }

    public void setEntryCategoryLogEventDAO(CategoryLogEventsDAO categoryLogEventsDAO) { this.categoryLogEventsDAO = categoryLogEventsDAO; }

    public void setLatencySeconds(int latencySeconds) {
        this.latencySeconds = latencySeconds;
        // For testing
        if (writeEntriesDAO != null) {
            writeEntriesDAO.setLatencySeconds(latencySeconds);
        }
        if (readEntriesDAO != null) {
            readEntriesDAO.setLatencySeconds(latencySeconds);
        }
        if (aggregateEntriesDAO != null) {
            aggregateEntriesDAO.setLatencySeconds(latencySeconds);
        }
    }

    public int getLatencySeconds() { return latencySeconds; }

    //-------------------
    //   WriteReadEntriesDAO
    //-------------------

    public void acquireLock() throws AtomServerException {writeEntriesDAO.acquireLock();}

    public Object insertEntry(EntryDescriptor entry) {return writeEntriesDAO.insertEntry(entry);}

    public Object insertEntry(EntryDescriptor entry, boolean isSeedingDB) {
        return writeEntriesDAO.insertEntry(entry, isSeedingDB);
    }

    public Object insertEntry(EntryDescriptor entry, boolean isSeedingDB, Date published, Date lastModified) {
        return writeEntriesDAO.insertEntry(entry, isSeedingDB, published, lastModified);
    }

    public int updateEntry(EntryDescriptor entry, boolean deleted) {return writeEntriesDAO.updateEntry(entry, deleted);}

    public int deleteEntry(EntryDescriptor entryQuery) {return writeEntriesDAO.deleteEntry(entryQuery);}

    public int deleteEntry(EntryDescriptor entryQuery, boolean setDeletedFlag) {
        return writeEntriesDAO.deleteEntry(entryQuery, setDeletedFlag);
    }

    public int insertEntryBatch(String workspace, Collection<? extends EntryDescriptor> entries) {
        return writeEntriesDAO.insertEntryBatch(workspace, entries);
    }

    public int updateEntryBatch(String workspace, Collection<? extends EntryDescriptor> entries) {
        return writeEntriesDAO.updateEntryBatch(workspace, entries);
    }

    public int deleteEntryBatch(String workspace, Collection<? extends EntryDescriptor> entries) {
        return writeEntriesDAO.deleteEntryBatch(workspace, entries);
    }

    public List<EntryMetaData> updateLastModifiedSeqNumForAllEntries(ServiceDescriptor service) {
        return writeEntriesDAO.updateLastModifiedSeqNumForAllEntries(service);
    }

    public void deleteAllEntries(ServiceDescriptor service) {writeEntriesDAO.deleteAllEntries(service);}

    public void deleteAllEntries(FeedDescriptor feed) {writeEntriesDAO.deleteAllEntries(feed);}

    public void deleteAllRowsFromEntries() {writeEntriesDAO.deleteAllRowsFromEntries();}

    public void obliterateEntry(EntryDescriptor entryQuery) {writeEntriesDAO.obliterateEntry(entryQuery);}

    //-------------------
    //   ReadEntriesDAO
    //-------------------

    @ManagedAttribute(description = "Set use the workspace, collection cache")
    public void setUseWorkspaceCollectionCache(boolean useWorkspaceCollectionCache) {
        readEntriesDAO.setUseWorkspaceCollectionCache(useWorkspaceCollectionCache);
    }

    @ManagedAttribute(description = "Use the workspace, collection cache")
    public boolean isUseWorkspaceCollectionCache() {
        return readEntriesDAO.isUseWorkspaceCollectionCache();
    }

    public EntryMetaData selectEntry(EntryDescriptor entry) {return readEntriesDAO.selectEntry(entry);}

    public Object selectEntryInternalId(EntryDescriptor entryDescriptor) {
        return readEntriesDAO.selectEntryInternalId(entryDescriptor);
    }

    public EntryMetaData selectEntryByInternalId(Object internalId) {
        return readEntriesDAO.selectEntryByInternalId(internalId);
    }

    public List<EntryMetaData> selectEntryBatch(Collection<? extends EntryDescriptor> entries) {
        return readEntriesDAO.selectEntryBatch(entries);
    }

    public List<EntryMetaData> selectEntries(EntryDescriptor entry) {return readEntriesDAO.selectEntries(entry);}

    public List<EntryMetaData> selectFeedPage(Date updatedMin, Date updatedMax, int startIndex, int endIndex, int pageSize, String locale, FeedDescriptor feed, Collection<BooleanExpression<AtomCategory>> categoryQuery) {
        return readEntriesDAO.selectFeedPage(updatedMin, updatedMax, startIndex, endIndex, pageSize, locale, feed, categoryQuery);
    }

    public List<EntryMetaData> selectEntriesByLastModifiedSeqNum(FeedDescriptor feed, Date updatedMin) {
        return readEntriesDAO.selectEntriesByLastModifiedSeqNum(feed, updatedMin);
    }

    public List<EntryMetaData> selectEntriesByLastModified(String workspace, String collection, Date updatedMin) {
        return readEntriesDAO.selectEntriesByLastModified(workspace, collection, updatedMin);
    }

    public int getTotalCount(ServiceDescriptor service) {return readEntriesDAO.getTotalCount(service);}

    public int getTotalCount(FeedDescriptor feed) {return readEntriesDAO.getTotalCount(feed);}

    public int getCountByLastModified(ServiceDescriptor service, Date updatedMin) {
        return readEntriesDAO.getCountByLastModified(service, updatedMin);
    }

    public int getCountByLastModified(FeedDescriptor feed, Date updatedMax) {
        return readEntriesDAO.getCountByLastModified(feed, updatedMax);
    }

    public void ensureWorkspaceExists(String name) {readEntriesDAO.ensureWorkspaceExists(name);}

    public void ensureCollectionExists(String workspace, String collection) {
        readEntriesDAO.ensureCollectionExists(workspace, collection);
    }

    public List<String> listWorkspaces() {return readEntriesDAO.listWorkspaces();}

    public List<String> listCollections(String workspace) {return readEntriesDAO.listCollections(workspace);}

    public void clearWorkspaceCollectionCaches() { readEntriesDAO.clearWorkspaceCollectionCaches(); }

    public long selectMaxIndex(Date updatedMax) {return readEntriesDAO.selectMaxIndex(updatedMax);}

    //-------------------
    //   AggregateEntriesDAO
    //-------------------

    public List<AggregateEntryMetaData> selectAggregateEntriesByPage(FeedDescriptor feed, Date updatedMin, Date updatedMax, Locale locale, int startIndex, int endIndex, int pageSize, Collection<BooleanExpression<AtomCategory>> categoriesQuery, List<String> joinWorkspaces) {
        return aggregateEntriesDAO.selectAggregateEntriesByPage(feed, updatedMin, updatedMax, locale, startIndex, endIndex, pageSize, categoriesQuery, joinWorkspaces);
    }

    public AggregateEntryMetaData selectAggregateEntry(EntryDescriptor entryDescriptor, List<String> joinWorkspaces) {
        return aggregateEntriesDAO.selectAggregateEntry(entryDescriptor, joinWorkspaces);
    }
}
