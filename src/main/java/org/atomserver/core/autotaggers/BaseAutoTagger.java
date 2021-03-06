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
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.dbstore.dao.CategoriesDAO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

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

    public boolean tag(EntryMetaData entry, String contentXML) {
        Document doc = parseContentXml( entry, contentXML );
        if ( doc == null ){
            return false;
        }
        return tag(entry, doc);
    }
        
    protected Document parseContentXml( EntryMetaData entry, String contentXML ) {
        // Parse the contentXML
        Document doc = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(new InputSource(new StringReader(contentXML)));
        } catch (Exception ee) {
            // TODO : should we really be eating these??
            String errmsg = "Exception : Unable to complete auto-tagging - exception parsing content for " + entry ;
            log.error(errmsg, ee);
            return null;
        }
        return doc;
    }

    //>>>>>>>>>>>>>>>>>>>>>>>>>>
    // DEPRECATED OPTIONS -- remove in 2.0.5
    /**
     * @deprecated
     */
    public void setEntryCategoriesDAO(CategoriesDAO categoriesDAO) {
        log.error("setCategoriesDAO is DEPRECATED and does nothing. You MUST use setCategoriesHandler");
    }
    //<<<<<<<<<<<<<<<<<<<
}
