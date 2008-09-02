/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.utils.io;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;


public class JarUtilsTest extends TestCase {
    static private Log log = LogFactory.getLog(JarUtilsTest.class);
    static private final String userdir = System.getProperty( "user.dir" );

    // -------------------------------------------------------
    public static Test suite() { return new TestSuite(JarUtilsTest.class); }

    // -------------------------------------------------------
    protected void setUp() throws Exception { super.setUp(); }

    // -------------------------------------------------------
    protected void tearDown() throws Exception { super.tearDown(); }

    //----------------------------
    //          Tests
    //----------------------------
    public void test1() throws Exception {
        URL url = JarUtils.getURLForClass("org.atomserver.utils.io.JarUtils");
        assertNotNull(url);
        log.debug( "URL for JarUtils= " + url );
        assertTrue( JarUtils.isFileURL( url ) );

        //-------------
        URL url2 = JarUtils.getURLForClass("org.apache.commons.logging.Log");
        assertNotNull(url2);
        log.debug( "URL for JarUtils= " + url2 );
        assertTrue( JarUtils.isJarURL( url2 ) );

        //-------------
        URL urls[] = {};
        JarFileLoader cl = new JarFileLoader(urls);
        String jarFilename = userdir + "/target/test-classes/testJarUtils.jar" ;
        cl.addFile( jarFilename );
        log.debug( "jarFilename= "+ jarFilename );

        File jarFile = new File( jarFilename );
        assertNotNull( jarFile );

        String destDirname = userdir + "/target/testJarUtils";
        File destDir = new File( destDirname );

        JarUtils.copyJarFolder(cl, jarFile, "testjar/var", destDir );

        // spot check a file
        File chkFile = new File( destDirname + "/testjar/var/widgets-ORIG/acme/27/2787/en/2787.xml.r0" );
        assertNotNull( chkFile );
        assertTrue( chkFile.exists() );
    }

    public void test2() throws Exception {
        URL testEntriesURL = getClass().getClassLoader().getResource( "widgets");
        log.debug( "testEntriesURL = " + testEntriesURL );
        File testEntries = new File(testEntriesURL.toString());
        log.debug( "testEntries = " + testEntries );
        assertTrue( JarUtils.isFileURL( testEntriesURL ) );

        URL testJarURL = getClass().getClassLoader().getResource( "junit/swingui/icons" );
        log.debug( "testJarURL = " + testJarURL );
        File testJarEntries = new File(testJarURL.toString());
        log.debug( "testJarEntries = " + testJarEntries );
        assertTrue( JarUtils.isJarURL( testJarURL ) );

        URL jarURL = JarUtils.getJarFromJarURL( testJarURL );
        assertNotNull( jarURL );
        File jarFile = new File(jarURL.toURI());
        assertTrue( jarFile.exists() );

        String destDirname = userdir + "/target/testJarUtils2";
        File destDir = new File( destDirname );

        JarUtils.copyJarFolder(jarFile, "junit/swingui/icons", destDir );

        // spot check a file
        File chkFile = new File( destDirname + "/junit/swingui/icons/error.gif" );
        assertNotNull( chkFile );
        assertTrue( chkFile.exists() );
    }

    static class JarFileLoader extends URLClassLoader {
        public JarFileLoader(URL[] urls) {
            super(urls);
        }

        public void addFile(String path) throws MalformedURLException {
            String urlPath = "jar:file://" + path + "!/";
            addURL(new URL(urlPath));
        }
    }

}
