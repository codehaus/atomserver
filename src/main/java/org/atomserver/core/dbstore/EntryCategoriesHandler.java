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

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Categories;
import org.atomserver.AtomServer;

import org.atomserver.EntryDescriptor;
import org.atomserver.CategoriesHandler;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class EntryCategoriesHandler
    extends EntryCategoriesContentStorage
    implements CategoriesHandler {

    static private final Log log = LogFactory.getLog(EntryCategoriesHandler.class);

    //--------------------------------
    //      public methods
    //--------------------------------
    /**
     * Write the Categories element to the Entry for this EntryDescriptor.
     * <p/>
     * NOTE: The workspace entering this method (in the EntryDescriptor) 
     * is assumed to be a "regular" workspace
     * <p/>
     * i.e. NOT a "Categories workspace" (e.g. widgets vs. tags:widgets)
     */
    public void writeEntryCategories( Entry entry, EntryDescriptor descriptor ) {

        Categories categoriesToWrite = getCategories(descriptor);
        if ( categoriesToWrite == null ) 
            return;

        List<Category> categoryList = categoriesToWrite.getCategories();
        for ( Category category : categoryList ) {
            entry.addCategory( category );
        }         
    }

    /**
     * Get a List of all Categories for this Workspace/Collection
     * <p/>
     * NOTE: The workspace entering this method is assumed to be a "regular" workspace
     * <p/>
     * i.e. NOT a "Categories workspace" (e.g. widgets vs. tags:widgets)
     */
    public List<Category> listCategories( String workspace, String collection ) {
        if ( log.isTraceEnabled() )
            log.trace( "EntryCategoriesContentStorage:: listCategories:: [" + workspace + ", " + collection +"]" );

        List schemeTermMapList = entryCategoriesDAO.selectDistictCategoriesPerCollection( workspace, collection );
        if ( schemeTermMapList == null || schemeTermMapList.size() <= 0 ) 
            return null; 

        Factory factory = AtomServer.getFactory(getServiceContext().getAbdera());
        List<Category> categoryList = new ArrayList<Category>();
        for ( Object obj : schemeTermMapList ) {
            Map schemeTermMap = (Map)obj;

            Category category = factory.newCategory();
            
            category.setScheme( (String)(schemeTermMap.get( "scheme" )) );
            category.setTerm( (String)(schemeTermMap.get( "term" )) );            
            // FIXME: deal w/ labels
             
            categoryList.add( category );
        }
        return categoryList;
    }
}
