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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.*;
import org.atomserver.core.AbstractAtomService;
import org.atomserver.core.BaseEntryDescriptor;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.WorkspaceOptions;
import org.atomserver.core.dbstore.dao.CategoriesDAO;
import org.atomserver.core.dbstore.dao.EntriesDAO;
import org.atomserver.utils.locale.LocaleUtils;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
@ManagedResource(description = "DB-Based Atom Service")
public class DBBasedAtomService extends AbstractAtomService {

    static private final Log log = LogFactory.getLog(DBBasedAtomCollection.class);

    static private final Pattern ENTRY_ID_PATTERN = Pattern.compile(
            "([^!/\\?]+)(?:/([^!/\\?]+)(?:/([^!/\\?]+))?)?(?:\\?locale=([a-z]{2}(?:_[A-Z]{2})?))?\\!?");
    static private final int DEFAULT_OBLITERATE_THRESHOLD = 10;

    private EntriesDAO entriesDAO = null;
    private TransactionTemplate transactionTemplate;
    private int obliterateThreshold = DEFAULT_OBLITERATE_THRESHOLD;

    public void setEntriesDAO(EntriesDAO entriesDAO) {
        this.entriesDAO = entriesDAO;
    }

    public EntriesDAO getEntriesDAO() {
        return entriesDAO;
    }

    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }

    public long getMaxIndex(Date updatedMax, boolean noLatency) {
        return this.entriesDAO.selectMaxIndex(updatedMax, noLatency);
    }

    @ManagedAttribute
    public int getObliterateThreshold() {
        return obliterateThreshold;
    }

    @ManagedAttribute
    public void setObliterateThreshold(int obliterateThreshold) {
        this.obliterateThreshold = obliterateThreshold;
    }

    @ManagedOperation(description = "obliterate entries.")
    public String obliterateEntries(String entriesQueries) {
        StringBuilder builder = new StringBuilder();

        String[] queries = entriesQueries.split(",");
        for (String query : queries) {
            Matcher matcher = ENTRY_ID_PATTERN.matcher(query);
            builder.append("(").append(query).append(" : ");
            if (matcher.matches()) {
                EntryDescriptor descriptor =
                        new BaseEntryDescriptor(
                                matcher.group(1),
                                matcher.groupCount() >= 2 ? matcher.group(2) : null,
                                matcher.groupCount() >= 3 ? matcher.group(3) : null,
                                matcher.groupCount() >= 4 ? LocaleUtils.toLocale(matcher.group(4)) : null);
                List<EntryMetaData> list = entriesDAO.selectEntries(descriptor);
                if (list.size() > obliterateThreshold && !query.endsWith("!")) {
                    builder.append("would have obliterated more than ")
                            .append(obliterateThreshold).append(" entries (")
                            .append(list.size()).append(") - try ").append(query).append("! instead.");
                } else {
                    for (EntryMetaData entry : list) {
                        ((DBBasedAtomCollection) getAtomWorkspace(descriptor.getWorkspace()).
                                getAtomCollection(descriptor.getCollection())).obliterateEntry(entry);
                    }
                    builder.append("obliterated ").append(list.size()).append(" entries.");
                }
            } else {
                builder.append("error - doesn't match workspace/collection/entryId?locale=xx_XX");
            }
            builder.append(")");
        }
        return builder.toString();
    }

    protected AtomWorkspace getJoinWorkspace(List<String> joinWorkspaces) {
        log.debug("returning an JoinWorkspace for " + joinWorkspaces);
        return new DBBasedJoinWorkspace(this, joinWorkspaces);
    }

    public AtomWorkspace newAtomWorkspace(AtomService parentService, String name) {
        return new DBBasedAtomWorkspace(parentService, name);
    }

    public void initialize() {
        super.initialize();

        if (log.isTraceEnabled()) {
            log.trace("Initializing Virtual workspaces for = " + workspaces);
        }
        java.util.Map<String, AtomWorkspace> wspaceMap = new java.util.HashMap<String, AtomWorkspace>(workspaces);

        for (AtomWorkspace wspace : wspaceMap.values()) {
            WorkspaceOptions options = wspace.getOptions();

            Set<String> allowedVirtualWorkspaces = options.getAllowedVirtualWorkspaces();

            for ( String allowedVirtualWorkspaceId : allowedVirtualWorkspaces )  {
                VirtualWorkspaceHandler handler = getVirtualWorkspaceHandler(allowedVirtualWorkspaceId);
                AtomWorkspace virtualWorkspace = handler.newVirtualWorkspace(this, options);
                this.workspaces.put(virtualWorkspace.getName(), virtualWorkspace);
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("workspaces after initialization for = " + workspaces);
        }
    }

    //>>>>>>>>>>>>>>>>>>>>>>>>>>
    // DEPRECATED OPTIONS -- remove in 2.0.5
    /**
     * @deprecated
     */
    public void setEntryCategoriesDAO(CategoriesDAO categoriesDAO) {
        log.error("setCategoriesDAO() is no longer valid on Atomservice. It is ignored");
    }

    /**
     * @deprecated
     */
    public void setCategoriesContentStorage(ContentStorage categoriesContentStorage) {
        log.error("setCategoriesDAO() is no longer valid on Atomservice. It is DEPRECATED");
        addVirtualWorkspaceHandler( VirtualWorkspaceHandler.CATEGORIES,
                                    (CategoriesHandler) categoriesContentStorage );
    }

    /**
     * @deprecated
     */
    public void setCategoriesHandler(CategoriesHandler categoriesHandler) {
        log.error("setCategoriesHandler() is no longer valid on AtomService. It is ignored");
        addVirtualWorkspaceHandler( VirtualWorkspaceHandler.CATEGORIES,
                                    categoriesHandler );
    }
    //<<<<<<<<<<<<<<<<<<<<<<<<<<
}
