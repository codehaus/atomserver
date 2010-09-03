/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.impl;

import org.atomserver.EntryDescriptor;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.dbstore.dao.ReadCategoriesDAO;
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class ReadCategoriesDAOiBatisImpl
        extends AbstractDAOiBatisImpl
        implements ReadCategoriesDAO {

    //-----------------------
    //       SELECT
    //-----------------------

    public EntryCategory selectEntryCategory(EntryCategory entryQuery) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoriesDAOiBatisImpl SELECT ==> " + entryQuery);
        }
        try {
            return (EntryCategory) (getSqlMapClientTemplate().queryForObject("selectEntryCategory", entryQuery));
        }
        finally {

            stopWatch.stop("DB.selectEntryCategory",
                           AtomServerPerfLogTagFormatter.getPerfLogEntryCategoryString(entryQuery));
        }
    }

    //======================================
    //          LIST QUERIES
    //======================================

    public List<EntryCategory> selectEntriesCategories(String workspace, String collection, Set<String> entryIds) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            return getSqlMapClientTemplate().queryForList(
                    "selectCategoriesForEntries",
                    paramMap()
                            .param("workspace", workspace)
                            .param("collection", collection)
                            .param("entryIds", new ArrayList<String>(entryIds)));
        }
        finally {
            stopWatch.stop("DB.selectEntriesForCategories", "[" + workspace + "." + collection + "]");
        }
    }

    public List<EntryCategory> selectEntryCategories(EntryDescriptor entryQuery) {
        return selectEntryCategoriesInScheme(entryQuery, null);
    }

    public List<EntryCategory> selectEntryCategoriesInScheme(EntryDescriptor entryQuery, String scheme) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            EntryCategory paramMap = new EntryCategory();
            if (entryQuery instanceof EntryMetaData) {
                paramMap.setEntryStoreId(((EntryMetaData) entryQuery).getEntryStoreId());
            } else {
                paramMap.setWorkspace(entryQuery.getWorkspace());
                paramMap.setCollection(entryQuery.getCollection());
                paramMap.setEntryId(entryQuery.getEntryId());
                paramMap.setLocale(entryQuery.getLocale());
            }
            paramMap.setScheme(scheme);
            return getSqlMapClientTemplate().queryForList("selectEntryCategories", paramMap);
        }
        finally {
            stopWatch.stop("DB.selectEntryCategoriesInScheme",
                           AtomServerPerfLogTagFormatter.getPerfLogEntryString(entryQuery));
        }
    }

    public List<String> selectDistictCollections(String workspace) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            return getSqlMapClientTemplate().queryForList("selectDistinctCollections", workspace);
        }
        finally {
            stopWatch.stop("DB.selectDistinctCollections", "[" + workspace + "]");
        }
    }

    public List<Map<String, String>> selectDistictCategoriesPerCollection(String workspace, String collection) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoriesDAOiBatisImpl::selectDistictCategoriesPerCollection [ " + workspace + " " + collection + " ]");
        }
        try {
            return getSqlMapClientTemplate().queryForList(
                    "selectDistinctCategoriesPerCollection",
                    paramMap()
                            .param("workspace", workspace)
                            .param("collection", collection));
        }
        finally {
            stopWatch.stop("DB.selectDistinctCollections", "[" + workspace + "." + collection + "]");
        }
    }

    //======================================
    //          COUNT QUERIES
    //======================================

    public int getTotalCount(String workspace) {
        return super.getTotalCountInternal(workspace, null, "countEntryCategoriesTotal");
    }

    public int getTotalCount(String workspace, String collection) {
        return super.getTotalCountInternal(workspace, collection, "countEntryCategoriesTotal");
    }

}
