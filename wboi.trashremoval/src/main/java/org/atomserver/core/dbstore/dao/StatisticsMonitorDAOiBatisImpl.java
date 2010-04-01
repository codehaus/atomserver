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

package org.atomserver.core.dbstore.dao;

import org.atomserver.monitor.WorkspaceCollectionMaxIndex;
import org.atomserver.monitor.WorkspaceCollectionDocumentCount;

import java.util.List;

/**
 * StatisticsMonitorDAOibatisImpl - Implementation class of StatisticsMonitorDAO.
 */
public class StatisticsMonitorDAOiBatisImpl
        extends AbstractDAOiBatisImpl
        implements StatisticsMonitorDAO {

    public List<WorkspaceCollectionDocumentCount> getDocumentCountPerWorkspaceCollection() {
        return getSqlMapClientTemplate().queryForList("selectDocumentsPerWorkspaceCollection");

    }

    public List<WorkspaceCollectionMaxIndex> getLastIndexPerWorkspaceCollection() {
        return getSqlMapClientTemplate().queryForList("selectMaxIndexPerWorkspaceCollection");
    }
}
