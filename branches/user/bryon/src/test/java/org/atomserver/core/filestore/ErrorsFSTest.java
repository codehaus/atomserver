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


package org.atomserver.core.filestore;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.atomserver.exceptions.AtomServerException;
import org.atomserver.core.ErrorsAtomServerTestCase;

import java.io.File;

/**
 */
public class ErrorsFSTest extends ErrorsAtomServerTestCase {

    public static Test suite() { return new TestSuite(ErrorsFSTest.class); }

    private String prevConfDir;

    public void setUp() throws Exception {
        File confDir = new File(getClass().getClassLoader().getResource("filestore-conf").toURI());
        prevConfDir = System.getProperty("atomserver.conf.dir");
        System.setProperty("atomserver.conf.dir", confDir.getAbsolutePath());
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        if (prevConfDir == null) {
            System.clearProperty("atomserver.conf.dir");
        } else {
            System.setProperty("atomserver.conf.dir", prevConfDir);
        }
    }

    protected String getStoreName() {
        return "filestore";
    }

    public void NOtestNothing() {}

    public void testErrorHandling() throws Exception {
        runErrorHandlingTests();
    }

    public void testBadRootDir() throws Exception {
        if (contentStorage instanceof FileBasedContentStorage) {
            FileBasedContentStorage fileBasedContentStorage = (FileBasedContentStorage) contentStorage;
            File rootDir = fileBasedContentStorage.getRootDir();
            assertTrue( rootDir != null );
            String initialAbsPath = rootDir.getAbsolutePath();

            fileBasedContentStorage.testingSetRootDirAbsPath( userdir + "/foobar" );
            try {
                rootDir = fileBasedContentStorage.getRootDir();
                fail( "we should have thrown an Exception" );
            } catch ( AtomServerException ee ) {
                log.debug( "we expect to get an Exception" );
            }

            fileBasedContentStorage.testingSetRootDirAbsPath( initialAbsPath );
            rootDir = fileBasedContentStorage.getRootDir();
            assertTrue( rootDir != null );
        }
    }

    public void testNFSFailure() throws Exception {
        if (contentStorage instanceof FileBasedContentStorage) {
            FileBasedContentStorage fileBasedContentStorage = (FileBasedContentStorage) contentStorage;
            File rootDir = fileBasedContentStorage.getRootDir();
            assertNotNull( rootDir );
            File nfsTempFile = fileBasedContentStorage.testingGetNFSTempFile();
            String nfsTempFileName = nfsTempFile.getAbsolutePath();
            assertNotNull( nfsTempFile );
            assertTrue( nfsTempFile.exists() );

            nfsTempFile.delete();
            Thread.sleep( 300 );

            try {
                rootDir = fileBasedContentStorage.getRootDir();
                fail( "we should have thrown an Exception" );
            } catch ( AtomServerException ee ) {
                log.debug( "we expect to get an Exception" );
            }

            nfsTempFile = new File( nfsTempFileName );
            org.apache.commons.io.FileUtils.touch( nfsTempFile );
            assertNotNull( nfsTempFile );
            assertTrue( nfsTempFile.exists() );

            rootDir = fileBasedContentStorage.getRootDir();
            assertTrue( rootDir != null );
        }
    }

}
