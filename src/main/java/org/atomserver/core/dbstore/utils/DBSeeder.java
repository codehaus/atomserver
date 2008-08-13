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
import org.atomserver.core.BaseEntryDescriptor;
import org.atomserver.core.BaseServiceDescriptor;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.dbstore.DBBasedContentStorage;
import org.atomserver.core.dbstore.dao.EntriesDAO;
import org.atomserver.core.dbstore.dao.EntryCategoriesDAO;
import org.atomserver.core.filestore.FileBasedContentStorage;
import org.atomserver.exceptions.AtomServerException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.IOException;
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
    static private final int SLEEP_TIME = 2000;

    static private ClassPathXmlApplicationContext springFactory = null;

    private EntriesDAO entriesDAO;
    private EntryCategoriesDAO entryCategoriesDAO;
    private ContentStorage contentStorage = null;

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

    public void setEntryCategoriesDAO(EntryCategoriesDAO entryCategoriesDAO) {
        this.entryCategoriesDAO = entryCategoriesDAO;
    }

    public void setContentStorage(ContentStorage physicalStorage) {
        this.contentStorage = physicalStorage;
    }

    public ContentStorage getContentStorage() {
        return contentStorage;
    }

    public void clearDB() throws AtomServerException {
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

                File widgetsDir = new File(rootDir, "widgets");
                File widgetsOrigDir = new File(rootDir, "widgets-ORIG");

                // when used by the dbseed.sh to add a few test widgets to a Server for smoke testing
                //  the widgets-ORIG is not there. It has already been copied to widgets,
                //  so we just skip this step
                if (widgetsOrigDir.exists()) {
                    FileUtils.deleteDirectory(widgetsDir);
                    FileUtils.copyDirectory(widgetsOrigDir, widgetsDir);
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

    public void insertWidget(BaseEntryDescriptor entryDescriptor) throws IOException {
        entriesDAO.insertEntry(entryDescriptor);
        if (contentStorage instanceof DBBasedContentStorage) {
            String entryId = entryDescriptor.getEntryId();
            File f = new File(MessageFormat.format("var/{0}/{1}/{2}/{3}/{4}/{3}.xml",
                                                   entryDescriptor.getWorkspace(),
                                                   entryDescriptor.getCollection(),
                                                   entryId.length() <= 2 ? entryId : entryId.substring(0, 2),
                                                   entryId,
                                                   entryDescriptor.getLocale().toString()));

            String content = FileUtils.readFileToString(f);
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
