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


package org.atomserver.core.dbstore.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.ContentStorage;
import org.atomserver.FeedDescriptor;
import org.atomserver.core.dbstore.dao.EntriesDAO;
import org.atomserver.core.dbstore.dao.EntryCategoriesDAO;
import org.atomserver.core.filestore.FileBasedContentStorage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;

/**
 * DBPurger - a class through which the user can purger Workspaces and Collectiosn from the system.
 * It is meant to be used from a bash script. 
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class DBPurger extends DBTool {

    private static final Log log = LogFactory.getLog(DBPurger.class);

    static private ClassPathXmlApplicationContext springFactory = null;

    private EntriesDAO entriesDAO;
    private EntryCategoriesDAO entryCategoriesDAO;

    private ContentStorage contentStorage = null;

    //--------------------------------
    //      static public methods
    //--------------------------------

    // NOTE: we do minimal error checking here because it is assumed that the driving script takes care of this.
    public static void main(String[] args) {
        if ( args.length < 1 || args.length > 2 ) {
            throw new IllegalArgumentException( "args.length < 1 || args.length > 2" );
        }

        String workspace = args[0];
        String collection = null;
        if ( args.length == 2 ) {
            collection = args[1];
        }
        if ( log.isDebugEnabled() ) 
            log.debug( "workspace= " + workspace + " collection= " + collection ); 

        try {
            getInstance().purge(workspace, collection);
        } catch( Exception ee ) {
            System.out.println( "Could NOT purge " + workspace + " " + collection );
            System.exit(123);
        }
    }

    public static ApplicationContext getSpringFactory() { return springFactory; }

    public static DBPurger getInstance(ApplicationContext parentContext) {
        return (DBPurger) getToolContext(parentContext).getBean("dbpurger");
    }

    public static DBPurger getInstance() {
        return (DBPurger) getToolContext().getBean("dbpurger");
    }

    //--------------------------------
    //      public methods
    //--------------------------------

    private DBPurger() {}

    public void setEntriesDAO( EntriesDAO entriesDAO ) {
        this.entriesDAO = entriesDAO;
    }
    public void setEntryCategoriesDAO( EntryCategoriesDAO entryCategoriesDAO) {
        this.entryCategoriesDAO = entryCategoriesDAO;
    }

    public void setContentStorage(ContentStorage contentStorage) {
        this.contentStorage = contentStorage;
    }

    static private final int SLEEP_TIME = 2000;

    public void purge( String workspace, String collection ) throws Exception {
        // delete the rows 
        purgeRows( workspace, collection );
        Thread.sleep(SLEEP_TIME);

        // delete the store files
        purgeDirs( workspace, collection );
        Thread.sleep(SLEEP_TIME);
    }

    private void purgeRows( final String workspace, final String collection ) throws Exception  {
        log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        log.info("==========> DELETING ROWS for ( " + workspace + ", " + collection + ") in EntryCategory !!!!!!!!!!!!!");
        log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        entryCategoriesDAO.deleteAllEntryCategories( workspace, collection );

        log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        log.info("==========> DELETING ROWS ( " + workspace + ", " + collection + ") in EntryStore  !!!!!!!!!!!!!");
        log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        entriesDAO.deleteAllEntries(new FeedDescriptor() {
            public String getWorkspace() {
                return workspace;
            }

            public String getCollection() {
                return collection;
            }
        });
    }

    private void purgeDirs( String workspace, String collection ) throws Exception {
        if (this.contentStorage instanceof FileBasedContentStorage) {
            File rootDir = ((FileBasedContentStorage)this.contentStorage).getRootDir();
            File workspaceDir = new File( rootDir, workspace );
            if ( collection != null ) {
                 File collectionDir = new File( workspaceDir, collection );
                log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
                log.info("==========> DELETING Directory " +  collectionDir );
                log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
                FileUtils.deleteDirectory( collectionDir );
            } else {
                log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
                // WE can't delete workspace dirs (they aren't created magically), so delete all the subdirs
                File[] files = workspaceDir.listFiles();
                if ( files != null ) {
                    for (File subFile : files) {
                        if (subFile.isDirectory() && !subFile.isHidden()) {
                            log.info("==========> DELETING Directory " + subFile);
                            FileUtils.deleteDirectory(subFile);
                        }
                    }
                }
                log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
            }
        }
    }
}
