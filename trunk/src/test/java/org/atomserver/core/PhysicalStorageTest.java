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


package org.atomserver.core;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.EntryDescriptor;
import org.atomserver.ContentStorage;
import org.atomserver.core.filestore.FileBasedContentStorage;
import org.atomserver.core.filestore.TestingContentStorage;
import org.atomserver.exceptions.AtomServerException;

import java.io.File;
import java.util.Locale;

/**
 * Test the ContentStorage.
 */
public class PhysicalStorageTest extends TestCase {

    static private Log log = LogFactory.getLog(PhysicalStorageTest.class);

    public void testPhysicalStorage() throws Exception {
        File root = new File(System.getProperty("java.io.tmpdir"), "storage");

        try {
            TestingContentStorage fileBasedStorage = new TestingContentStorage(root);
            TestingContentStorage storage = fileBasedStorage;
            storage.setTestingFailOnGet(false);
            
            assertEquals(root, fileBasedStorage.getRootDir());
            assertTrue(storage.canRead());

            EntryDescriptor my_1234_en = new BaseEntryDescriptor("widgets", "mine", "1234", Locale.ENGLISH, 1);     
            EntryDescriptor my_1234_en_GB = new BaseEntryDescriptor("widgets", "mine", "1234", Locale.UK, 0);

            assertFalse(storage.contentExists(my_1234_en));

            storage.putContent("mine:1234:en", my_1234_en);
            storage.putContent("mine:1234:en", my_1234_en_GB);

            assertTrue(storage.contentExists(my_1234_en));
            assertEquals("mine:1234:en", storage.getContent(my_1234_en));
            
            assertTrue(storage.contentExists(my_1234_en_GB));
            assertEquals("mine:1234:en", storage.getContent(my_1234_en_GB));
            //assertEquals(storage.lastModified(my_1234_en), storage.lastModified(my_1234_en_GB));

            Thread.sleep(1100); // sleep for just over a second, to make sure the mod times are different.

            storage.putContent("mine:1234:en_GB", my_1234_en_GB);

            assertTrue(storage.contentExists(my_1234_en));
            assertEquals("mine:1234:en", storage.getContent(my_1234_en));
            assertTrue(storage.contentExists(my_1234_en_GB));
            assertEquals("mine:1234:en_GB", storage.getContent(my_1234_en_GB));
            assertTrue(fileBasedStorage.lastModified(my_1234_en) < fileBasedStorage.lastModified(my_1234_en_GB));

            File file_1234_en = (File) storage.getPhysicalRepresentation("widgets", "mine", "1234", Locale.ENGLISH, 1);
            File file_1234_en_GB = (File) storage.getPhysicalRepresentation("widgets", "mine", "1234", Locale.UK, 0);

            log.debug( "PATH= " + file_1234_en.getAbsolutePath() );
            EntryDescriptor out_1234_en = fileBasedStorage.getEntryMetaData(file_1234_en.getAbsolutePath());
            assertNotNull( out_1234_en );
            assertEquals(my_1234_en.getWorkspace(), out_1234_en.getWorkspace());
            assertEquals(my_1234_en.getCollection(), out_1234_en.getCollection());
            assertEquals(my_1234_en.getEntryId(), out_1234_en.getEntryId());
            assertEquals(my_1234_en.getLocale(), out_1234_en.getLocale());

            // we started at rev1, so the file should match
            assertEquals(1, out_1234_en.getRevision());

            EntryDescriptor out_1234_en_GB = fileBasedStorage.getEntryMetaData(file_1234_en_GB.getAbsolutePath());
            assertEquals(my_1234_en_GB.getWorkspace(), out_1234_en_GB.getWorkspace());
            assertEquals(my_1234_en_GB.getCollection(), out_1234_en_GB.getCollection());
            assertEquals(my_1234_en_GB.getEntryId(), out_1234_en_GB.getEntryId());
            assertEquals(my_1234_en_GB.getLocale(), out_1234_en_GB.getLocale());

            // we started at rev0, and PUT twice, so the file should match
            assertEquals(1, out_1234_en.getRevision());

            storage.deleteContent(null, my_1234_en);
            assertFalse(storage.contentExists(my_1234_en));
            assertEquals(null, storage.getContent(my_1234_en));
            assertTrue(storage.contentExists(my_1234_en_GB));
            assertEquals("mine:1234:en_GB", storage.getContent(my_1234_en_GB));

        } finally {
            FileUtils.deleteDirectory(root);
        }
    }

    public void testFaultyStorage() throws Exception {
        File root = new File(System.getProperty("java.io.tmpdir"), "faultystorage");
        try {
            FileBasedContentStorage storage = new FileBasedContentStorage(root);
            FileUtils.deleteDirectory(root);
            try { 
                storage.canRead(); 
                fail( "Should have thrown an Exception" );
            } catch ( AtomServerException ee ) {}

            root.mkdirs();
            storage.testingResetNFSTempFile();

            assertTrue(storage.canRead());
        } finally {
            FileUtils.deleteDirectory(root);
        }
    }

    public void testStorageErrors() throws Exception {
        File root = new File(System.getProperty("java.io.tmpdir"), "storage");
        try {
            FileBasedContentStorage storage = new FileBasedContentStorage(root);

            EntryDescriptor my_1234_en = new BaseEntryDescriptor("widgets", "mine", "1234", Locale.ENGLISH, 0);
            File file = new File(root, "widgets/mine/12/1234/en/1234.xml.r0");
            FileUtils.writeStringToFile(file, "JUNK", "UTF-8");
            file.setReadOnly();
            try {
                storage.putContent("STUFF", my_1234_en);
                assertTrue("we expected an exception!", false);
            } catch (Exception e) {
                // do nothing, we expected this.
            }

            assertEquals("JUNK", storage.getContent(my_1234_en));

            // replacing the file with a directory is the easiest way I can think of to programmatically
            // make the getContent throw an exception - but in real life this would probably happen because
            // of permissions issues.
            FileUtils.forceDelete(file);
            file.mkdirs();
            try {
                String entryData = storage.getContent(my_1234_en);
                assertTrue("we expected an exception, not " + entryData + "!", false);
            } catch(Exception e) {
                // do nothing, we expected this.
            }

            FileUtils.forceDelete(file);
            FileUtils.writeStringToFile(file, "JUNK", "UTF-8");

            assertEquals("JUNK", storage.getContent(my_1234_en));
            
        } finally {
            FileUtils.deleteDirectory(root);
        }
    }


    public void testTrashDir() throws Exception {
         File root = new File(System.getProperty("java.io.tmpdir"), "trashTest");
         try {
             FileBasedContentStorage storage = new FileBasedContentStorage(root);

             int origTime = storage.getSweepToTrashLagTimeSecs();
             storage.setSweepToTrashLagTimeSecs(1);

             File baseDir = new File(root, "widgets/mine/12/1234/en");
             for (int ii = 7; ii < 14; ii++) {
                 File file = new File(baseDir, "1234.xml.r" + ii);
                 FileUtils.writeStringToFile(file, "JUNK", "UTF-8");
             }
             assertEquals( 7, baseDir.listFiles().length);

             Thread.sleep( 2000 );

             // do the actual put, which will sweep to trash
             EntryDescriptor my_1234_en = new BaseEntryDescriptor("widgets", "mine", "1234", Locale.ENGLISH, 14);
             storage.putContent("STUFF", my_1234_en);

             File trash = new File(baseDir, FileBasedContentStorage.TRASH_DIR_NAME );
             assertTrue( trash != null );
             assertTrue( trash.exists() );
             assertTrue( trash.isDirectory() );

             assertEquals( 6, trash.listFiles().length);

             // should find the current file and the _trash dir
             assertEquals( 3, baseDir.listFiles().length);
             for ( File file : baseDir.listFiles() ) {
                assertTrue( file.getName().equals( "1234.xml.r13") ||
                            file.getName().equals( "1234.xml.r14") ||
                            file.getName().equals( FileBasedContentStorage.TRASH_DIR_NAME ));
             }

             // add more revisions
             for (int ii = 15; ii < 19; ii++) {
                 File file = new File(baseDir, "1234.xml.r" + ii);
                 FileUtils.writeStringToFile(file, "JUNK", "UTF-8");
             }
             assertEquals( 7, baseDir.listFiles().length);

             Thread.sleep( 2000 );

             // do the actual put
             my_1234_en = new BaseEntryDescriptor("widgets", "mine", "1234", Locale.ENGLISH, 19);
             storage.putContent("STUFF", my_1234_en);

             // check it out
             assertEquals( 11, trash.listFiles().length);
             assertEquals( 3, baseDir.listFiles().length);
             for ( File file : baseDir.listFiles() ) {
                assertTrue( file.getName().equals( "1234.xml.r18") ||
                            file.getName().equals( "1234.xml.r19") ||
                            file.getName().equals( FileBasedContentStorage.TRASH_DIR_NAME ));
             }
             storage.setSweepToTrashLagTimeSecs(origTime);

         } finally {
             FileUtils.deleteDirectory(root);
         }
     }

    public void testLagTime() throws Exception {
         File root = new File(System.getProperty("java.io.tmpdir"), "trashTest");
         try {
             FileBasedContentStorage storage = new FileBasedContentStorage(root);

             int origTime = storage.getSweepToTrashLagTimeSecs();
             storage.setSweepToTrashLagTimeSecs(1);

             File baseDir = new File(root, "widgets/thiers/12/1234/en");
             for (int ii = 7; ii < 10; ii++) {
                 File file = new File(baseDir, "1234.xml.r" + ii);
                 FileUtils.writeStringToFile(file, "JUNK", "UTF-8");
             }
             assertEquals( 3, baseDir.listFiles().length);

             Thread.sleep( 2000 );

             for (int ii = 10; ii < 14; ii++) {
                 File file = new File(baseDir, "1234.xml.r" + ii);
                 FileUtils.writeStringToFile(file, "JUNK", "UTF-8");
             }
             assertEquals( 7, baseDir.listFiles().length);

             // do the actual put, which will sweep to trash
             EntryDescriptor my_1234_en = new BaseEntryDescriptor("widgets", "thiers", "1234", Locale.ENGLISH, 14);
             storage.putContent("STUFF", my_1234_en);

             File trash = new File(baseDir, FileBasedContentStorage.TRASH_DIR_NAME );
             assertTrue( trash != null );
             assertTrue( trash.exists() );
             assertTrue( trash.isDirectory() );

             assertEquals( 3, trash.listFiles().length);

             // including the trash dir itself and the one we just PUT
             assertEquals( 6, baseDir.listFiles().length);

             storage.setSweepToTrashLagTimeSecs(origTime);

         } finally {
             FileUtils.deleteDirectory(root);
         }
     }


    public void testZeroRev() throws Exception {
        File root = new File(System.getProperty("java.io.tmpdir"), "trashTest2");
        try {
            FileBasedContentStorage storage = new FileBasedContentStorage(root);
            File baseDir = new File(root, "widgets/yours/12/1234/en");

            EntryDescriptor my_1234_en = new BaseEntryDescriptor("widgets", "yours", "1234", Locale.ENGLISH, 0);
            storage.putContent("STUFF", my_1234_en);

            File trash = new File(baseDir, FileBasedContentStorage.TRASH_DIR_NAME);
            assertTrue(trash != null);
            assertFalse( trash.exists() );
        }
        finally {
            FileUtils.deleteDirectory(root);
        }
    }

}
