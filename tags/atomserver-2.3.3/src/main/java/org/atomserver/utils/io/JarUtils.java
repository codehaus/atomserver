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

package org.atomserver.utils.io;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * JarUtils - a utility class to assist in pulling resources from a Jar,
 * treating them as a folder in the Jar.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class JarUtils {
    static private Log log = LogFactory.getLog(JarUtils.class);

    /**
     * Is this URL a Jar URL? (i.e starts with jar: or ends with .jar)
     * @param url
     * @return
     */
    static public boolean isJarURL( URL url ) {
        String surl = url.toString();
        if ( surl.endsWith( ".jar" )  || surl.startsWith( "jar:" )) {
            return true;
        }
        return false;
    }

    /**
     * Is this a file URL that is not also a Jar URL?
     * @param url
     * @return
     */
    static public boolean isFileURL( URL url ) {
        String surl = url.toString();
        if ( surl.startsWith( "file:" ) && !isJarURL(url) ) {
            return true;
        }
        return false;
    }

    /**
     * Return the Jar URL within this URL. A jar URL looks like this;
     * jar:file:/foo/bar/xxx.jar!aaa/bbb/resource
     * This method strips out the jar: and everthing past the !
     * @param url
     * @return  Null if this is not a Jar URL, otherwise return the Jar URL
     */
    static public URL getJarFromJarURL( URL url ) {
        if ( !isJarURL(url) ) {
            return null;
        }
        String surl = url.toString();
        log.debug( "Start URL = " + surl );
        if ( surl.startsWith( "jar:" ) ) {
            int stopIndex = surl.indexOf( "!" );
            surl = surl.substring( 4, stopIndex );
            log.debug( "New URL= " + surl );
            URL newURL = null;
            try {
                newURL = new URL( surl );
            } catch ( MalformedURLException ee ) {
                log.error( ee );  
            }
            return newURL;
        } else {
            return url;
        }
    }

    /**
     * Find out where a class on the classpath will be loaded from.
     * This will be either a Jar URL (e.g. )
     * or a File URL (e.g. file:/foo/bar/target/classes)
     * @param className name of fully qualified class to find, using dots, but no dot class.
     *                  e.g. org.atomserver.utils.io.JarUtils
     * @return The URL associated with this class
     * @throws ClassNotFoundException
     */
    static public URL getURLForClass(String className) throws ClassNotFoundException {
        Class qc = Class.forName(className);
        CodeSource source = qc.getProtectionDomain().getCodeSource();
        URL location = null;
        if (source != null) {
            location = source.getLocation();
            log.debug(className + " : " + location);
        }
        return location;
    }

    /**
     * Copies an entire folder out of a jar to a physical location.
     * Uses the ClassLoader associated with this class to load resources from
     * @param jar  The jar opened as a File
     * @param folderName  The folder name in the jar (e.g. test-resources/var)
     * @param destDir The root directory to copy into (will be created if req'd)
     * @throws IOException
     */
    static public void copyJarFolder(File jar, String folderName, File destDir) throws IOException {
        copyJarFolder(JarUtils.class.getClassLoader(), jar, folderName, destDir);
    }

    /**
     * Copies an entire folder out of a jar to a physical location (i.e. to a directory).
     * @param cl The ClassLoader to use when pulling resources from the Jar
     * @param jar  The jar opened as a File
     * @param folderName  The folder name in the jar (e.g. test-resources/var)
     * @param destDir The root directory to copy into (will be created if req'd)
     * @throws IOException
     */
    static public void copyJarFolder(ClassLoader cl, File jar, String folderName, File destDir ) throws IOException {

        log.debug("jar= " + jar + " destDir= " + destDir + " folderName= " + folderName);

        FileUtils.forceMkdir(destDir);
        
        ZipFile zipfile = new ZipFile(jar);

        Enumeration entries = zipfile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            log.trace("examining ENTRY = " + entry.getName());

            if (entry.getName().contains(folderName)) {
                File fileDest = new File(destDir, entry.getName());
                if (!fileDest.exists()) {
                    if (entry.isDirectory()) {
                        log.trace("Creating Directory :: " + fileDest );
                        FileUtils.forceMkdir(fileDest);
                    } else {
                        copyFromJar(cl, entry.getName(), fileDest);
                    }
                } else {
                    log.trace("file (" + fileDest + ") already exists");
                }

            }
        }
    }

    /**
     * Copies a file out of the jar to a physical location.
     *
     * @param resource
     * @param fileDest
     * @return
     */
    static public void copyFromJar(ClassLoader cl, String resource, File fileDest) throws IOException {
        if (resource == null) {
            throw new IllegalArgumentException("resource is NULL");
        }
        if (fileDest == null) {
            throw new IllegalArgumentException("fileDest is NULL");
        }
        log.trace("COPYING " + resource + " TO " + fileDest);
        IOUtils.copy(cl.getResourceAsStream(resource),
                     new FileOutputStream( fileDest ) );
    }
}
