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

import org.atomserver.CategoriesHandler;
import org.atomserver.EntryAutoTagger;
import org.atomserver.core.dbstore.dao.EntryCategoriesDAO;
import org.atomserver.core.etc.AtomServerPerformanceLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * BaseAutoTagger - provides an abstract base class for EntryAutoTaggers that allows for a spring-
 * configured EntryCategoriesDAO.
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public abstract class BaseAutoTagger implements EntryAutoTagger {
    protected static final Log log = LogFactory.getLog(BaseAutoTagger.class);

    private CategoriesHandler categoriesHandler;

    public CategoriesHandler getCategoriesHandler() {
        return categoriesHandler;
    }

    public void setCategoriesHandler(CategoriesHandler categoriesHandler) {
        this.categoriesHandler = categoriesHandler;
    }

    /**
     * Set from Spring
     */
    protected AtomServerPerformanceLog perflog;

    public void setPerformanceLog(AtomServerPerformanceLog perflog) {
        this.perflog = perflog;
    }


    //>>>>>>>>>>>>>>>>>>>>>>>>>>
    // DEPRECATED OPTIONS -- remove in 2.0.5
    /**
     * @deprecated
     */
    public void setEntryCategoriesDAO(EntryCategoriesDAO entryCategoriesDAO) {
        log.error("setEntryCategoriesDAO is DEPRECATED and does nothing. You MUST use setCategoriesHandler");
    }
    //<<<<<<<<<<<<<<<<<<<
}
