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
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.ContentStorage;
import org.atomserver.core.BaseEntryDescriptor;
import org.atomserver.core.BaseServiceDescriptor;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.dbstore.DBBasedContentStorage;
import org.atomserver.core.dbstore.dao.ContentDAO;
import org.atomserver.core.dbstore.dao.EntriesDAO;
import org.atomserver.core.dbstore.dao.EntryCategoriesDAO;
import org.atomserver.core.dbstore.dao.EntryCategoryLogEventDAO;
import org.atomserver.core.filestore.FileBasedContentStorage;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.utils.io.JarUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;

/**
 * DBSeeder - a funky class that really doesn't belong in the "main" tree of AtomServer.
 * It would be better if it could live down in the test tree. But we use it from a bash script
 * to seed DBs on staging servers when we want to smoke test an installation before the real data
 * is available. It is NOT a general purpose database seeder. It knows ONLY about the small
 * "widgets' workspace we use in out JUnits and about the "pets" workspace we use in the demonstration,
 * standalone AtomServer.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class DBSeeder extends DBTool {

    static private final Log log = LogFactory.getLog(DBSeeder.class);
    static private final int SLEEP_TIME = 2500;

    static private final String SAMPLE_WIDGETS_DIR = "widgets";

    static private ClassPathXmlApplicationContext springFactory = null;

    private EntriesDAO entriesDAO;
    private EntryCategoryLogEventDAO entryCategoryLogEventDAO;
    private EntryCategoriesDAO entryCategoriesDAO;
    private ContentStorage contentStorage;
    private ContentDAO contentDAO;

    //--------------------------------
    //      static methods
    //--------------------------------
    public static void main(String[] args) {
        getInstance().seedWidgets();
    }

    public static ApplicationContext getSpringFactory() {
        return springFactory;
    }

    public static DBSeeder getInstance() {
        return (DBSeeder) getToolContext().getBean("dbseeder");
    }

    public static DBSeeder getInstance(ApplicationContext parentContext) {
        return (DBSeeder) getToolContext(parentContext).getBean("dbseeder");
    }

    //--------------------------------
    //      instance methods
    //--------------------------------
    private DBSeeder() {}

    public void setEntriesDAO(EntriesDAO entriesDAO) {
        this.entriesDAO = entriesDAO;
    }

    public void setContentDAO(ContentDAO contentDAO) {
        this.contentDAO = contentDAO;
    }

    public void setEntryCategoriesDAO(EntryCategoriesDAO entryCategoriesDAO) {
        this.entryCategoriesDAO = entryCategoriesDAO;
    }

    public void setEntryCategoryLogEventDAO(EntryCategoryLogEventDAO entryCategoryLogEventDAO) {
        this.entryCategoryLogEventDAO = entryCategoryLogEventDAO;
    }

    public void setContentStorage(ContentStorage physicalStorage) {
        this.contentStorage = physicalStorage;
    }

    public ContentStorage getContentStorage() {
        return contentStorage;
    }

    public void clearDB() throws AtomServerException {
        log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        log.info("==========> DELETING *ALL* ROWS in EntryContent !!!!!!!!!!!!!");
        log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        //contentDAO.deleteAllContent();
        contentDAO.deleteAllRowsFromContent();

        log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        log.info("==========> DELETING *ALL* ROWS in EntryCategoryLogEvent !!!!!!!!!!!!!");
        log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        entryCategoryLogEventDAO.deleteAllRowsFromEntryCategoryLogEvent();

        log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        log.info("==========> DELETING *ALL* ROWS in EntryCategory !!!!!!!!!!!!!");
        log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        entryCategoriesDAO.deleteAllRowsFromEntryCategories();

        log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        log.info("==========> DELETING *ALL* ROWS in EntryStore  !!!!!!!!!!!!!");
        log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        entriesDAO.deleteAllRowsFromEntries();
    }

    public void createWidgetsDir() throws AtomServerException {
        try {
            if (contentStorage instanceof FileBasedContentStorage) {
                FileBasedContentStorage fileStorage = (FileBasedContentStorage) contentStorage;
                File rootDir = fileStorage.getRootDir();
                log.debug( "ROOT DIR = " + rootDir );

                File widgetsDir = new File(rootDir, "widgets");

                FileUtils.forceMkdir(widgetsDir);
            }
        } catch (Exception ee) {
            throw new AtomServerException("Unknown Exception when seeding the DB", ee);
        }
    }

    public void seedWidgets() throws AtomServerException {
        try {
            Locale en = new Locale("en");

            if (contentStorage instanceof FileBasedContentStorage) {
                FileBasedContentStorage fileStorage = (FileBasedContentStorage) contentStorage;
                File rootDir = fileStorage.getRootDir();
                log.debug( "ROOT DIR = " + rootDir );

                // root dir is the actual data dir. In tests this is "target/var"
                File widgetsDir = new File(rootDir, "widgets");

                FileUtils.deleteDirectory(widgetsDir);

                URL widgetsORIGURL = getClass().getClassLoader().getResource( SAMPLE_WIDGETS_DIR );
                if ( JarUtils.isJarURL(widgetsORIGURL) ) {
                    URL widgetsORIGJarURL = JarUtils.getJarFromJarURL( widgetsORIGURL );
                    File jarFile = new File(widgetsORIGJarURL.toURI());
                    JarUtils.copyJarFolder(jarFile, SAMPLE_WIDGETS_DIR, rootDir );
                } else {
                    File widgetsOrigDir = new File(widgetsORIGURL.toURI());
                    FileUtils.copyDirectory( widgetsOrigDir, widgetsDir);
                }

            }

            entriesDAO.deleteAllEntries(new BaseServiceDescriptor("widgets"));
            entryCategoriesDAO.deleteAllEntryCategories("widgets");

            entriesDAO.ensureCollectionExists("widgets", "acme");
            insertWidget(new BaseEntryDescriptor("widgets", "acme", "2787", en, 0));
            insertWidget(new BaseEntryDescriptor("widgets", "acme", "2788", en, 0));
            insertWidget(new BaseEntryDescriptor("widgets", "acme", "2797", en, 0));
            insertWidget(new BaseEntryDescriptor("widgets", "acme", "2799", en, 0));
            insertWidget(new BaseEntryDescriptor("widgets", "acme", "4", en, 0));
            insertWidget(new BaseEntryDescriptor("widgets", "acme", "9991", en, 0));
            insertWidget(new BaseEntryDescriptor("widgets", "acme", "9993", en, 0));
            insertWidget(new BaseEntryDescriptor("widgets", "acme", "9995", en, 0));
            insertWidget(new BaseEntryDescriptor("widgets", "acme", "9998", en, 0));
            insertWidget(new BaseEntryDescriptor("widgets", "acme", "9999", en, 0));
        } catch (Exception ee) {
            throw new AtomServerException("Unknown Exception when seeding the DB", ee);
        }
    }

    public void insertWidget(BaseEntryDescriptor entryDescriptor) throws Exception {
        entriesDAO.insertEntry(entryDescriptor);
        if (contentStorage instanceof DBBasedContentStorage) {
            String entryId = entryDescriptor.getEntryId();

            String filename = MessageFormat.format(SAMPLE_WIDGETS_DIR + "/{0}/{1}/{2}/{3}/{2}.xml.r0",
                                                   entryDescriptor.getCollection(),
                                                   entryId.length() <= 2 ? entryId : entryId.substring(0, 2),
                                                   entryId,
                                                   entryDescriptor.getLocale().toString());
            log.debug( "LOOKING FOR " + filename );

            InputStream is = getClass().getClassLoader().getResource( filename ).openStream();

            String content = IOUtils.toString( is );
            contentStorage.putContent(content, entryDescriptor);
        }
    }


    public void seedPets() throws AtomServerException {
        try {
            if (contentStorage instanceof FileBasedContentStorage) {
                FileBasedContentStorage fileStorage = (FileBasedContentStorage) contentStorage;
                File rootDir = fileStorage.getRootDir();

                File petsDir = new File(rootDir, "pets");
                if (!petsDir.exists()) {
                    petsDir.mkdirs();
                }
            }

            entryCategoriesDAO.deleteAllEntryCategories("pets");
            entriesDAO.deleteAllEntries(new BaseServiceDescriptor("pets"));

            entriesDAO.ensureCollectionExists("pets", "cats");
            insertPet("pets", "cats", "fluffy", "Mixed" );
            insertPet("pets", "cats", "martha", "Mixed" );
            insertPet("pets", "cats", "marie", "Siamese" );
            insertPet("pets", "cats", "johndoe", "Coon" );

            entriesDAO.ensureCollectionExists("pets", "dogs");
            insertPet("pets", "dogs", "fido", "Mixed" );
            insertPet("pets", "dogs", "sparky", "Collie" );
            insertPet("pets", "dogs", "spike", "Mixed" );

        } catch (Exception ee) {
            throw new AtomServerException("Unknown Exception when seeding the DB", ee);
        }
    }

    public void insertPet( String workspace, String collection, String id, String term) throws IOException {
        Integer internalId =
                (Integer)(entriesDAO.insertEntry(new BaseEntryDescriptor(workspace, collection, id, null, 0)));
        log.debug( "Inserted PET = " + internalId + " " + workspace + " " + collection + " " + id );
        EntryCategory entryCategory = new EntryCategory();
        entryCategory.setEntryStoreId( internalId.longValue() );
        entryCategory.setWorkspace( workspace );
        entryCategory.setCollection( collection );
        entryCategory.setEntryId( id );
        entryCategory.setScheme( "urn:pets.breeds" );
        entryCategory.setTerm( term );
        entryCategoriesDAO.insertEntryCategory( entryCategory );
    }


    public void seedEntriesClearingFirst() throws AtomServerException {
        clearDB();
        try { Thread.sleep(SLEEP_TIME); }
        catch (InterruptedException ee) {/*NOOP*/}
        seedWidgets();
        try { Thread.sleep(SLEEP_TIME); }
        catch (InterruptedException ee) {/*NOOP*/}
    }

}
