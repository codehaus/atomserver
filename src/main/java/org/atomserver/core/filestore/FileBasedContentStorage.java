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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.ContentStorage;
import org.atomserver.EntryDescriptor;
import org.atomserver.core.BaseEntryDescriptor;
import org.atomserver.exceptions.AtomServerException;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.annotation.ManagedAttribute;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * File-based implementation of physical storage for entries.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
@ManagedResource(description = "File-Based Content Storage")
public class FileBasedContentStorage implements ContentStorage {

    static private Log log = LogFactory.getLog(FileBasedContentStorage.class);
    static private final String UTF_8 = "UTF-8";

    static private final int MAX_RETRIES = 3; 
    static private final int SLEEP_BETWEEN_RETRY = 750;

    static private final Pattern FILE_PATH_PATTERN =
        Pattern.compile("/?(\\w+)/(\\w+)/\\w+/(\\w+)/?((?:\\w+)?(?:/\\w+)?)/\\w+\\.xml.r(\\d+)");

    static public final String TRASH_DIR_NAME = "_trash";

    static private final int NO_REVISION = -777;

    static private final int SWEEP_TO_TRASH_LAG_TIME_SECS_DEFAULT = 120;

    //============================================
    private File nfsTempFile = null;
    private String rootDirAbsPath = null;

    private boolean sweepToTrash = true;

    private int sweepToTrashLagTimeSecs = SWEEP_TO_TRASH_LAG_TIME_SECS_DEFAULT;

    //============================================
    static public int getMaxRetries() {
        return MAX_RETRIES;
    }

    //============================================
    //    FOR TESTING ONLY
    static private boolean testingFailOnGet = false;
    static public void setTestingFailOnGet( boolean tORf ) {
        testingFailOnGet = tORf;
    }
    static private boolean testingAlternatelyFailOnFileReadException = false;
    static private int testingAlternatelyFailOnFileReadExceptionCount = -1 ; 
    static private int testingAlternatelyFailOnFileReadExceptionPassCount = 3 ; 
    static public void setTestingAlternatelyFailOnFileReadException( boolean tORf ) {
        testingAlternatelyFailOnFileReadException = tORf;
        if ( tORf ) testingAlternatelyFailOnFileReadExceptionCount = -1;
    }
    static public void setTestingAlternatelyFailOnFileReadExceptionPassCount( int passCount ) {
        testingAlternatelyFailOnFileReadExceptionPassCount = passCount;
    }
    static private boolean testingAlternatelyFailOnFileReadNull = false;
    static private int testingAlternatelyFailOnFileReadNullCount = -1 ; 
    static private int testingAlternatelyFailOnFileReadNullPassCount = 3 ; 
    static public void setTestingAlternatelyFailOnFileReadNull( boolean tORf ) {
        testingAlternatelyFailOnFileReadNull = tORf;
        if ( tORf ) testingAlternatelyFailOnFileReadNullCount = -1;
    }
    static public void setTestingAlternatelyFailOnFileReadNullPassCount( int passCount ) {
        testingAlternatelyFailOnFileReadNullPassCount = passCount;
    }
    static private boolean testingAlternatelyFailOnPut = false;
    static private int testingAlternatelyFailOnPutCount = -1 ; 
    static public void setTestingAlternatelyFailOnPut( boolean tORf ) {
        testingAlternatelyFailOnPut = tORf;
        if ( tORf ) testingAlternatelyFailOnPutCount = -1;
    }
    static private boolean isAlternatelyFail( boolean tORf, int count, int mod ) { 
        if ( tORf ) {
            if (count % mod == 1)  
                return true;
        }
        return false;
    }
    static private boolean isAlternatelyPass( boolean tORf, int count, int mod ) { 
        if ( tORf ) 
            return ! isAlternatelyFail( tORf, count, mod );
        return false;
    }

    //=========================
    // The following methods are for testing purposes ONLY
    private boolean successfulAvailabiltyFileWrite = false;

    public void testingResetNFSTempFile() {
        setNFSTempFile( (new File(rootDirAbsPath)) );
    }
    public File testingGetNFSTempFile() {
        return nfsTempFile;
    }
    public void testingSetRootDirAbsPath( String rootDirAbsPath ) {
        this.rootDirAbsPath = rootDirAbsPath;
    }
    public boolean testingWroteAvailabiltyFile() {
        return successfulAvailabiltyFileWrite;
    }

    //====================================
    /**
     * This method is used by the isAliveHandler to determine if the ContentStorage is alive and well
     */
    public void testAvailability() {
        // check the root dir, see if any exceptions get thrown.
        File rootDir = getRootDir();

        // we must actually write a file, because we may have "disk is full" problems
        writeTestFile( rootDir );
    }

    private static byte[] testBytes = new byte[1024];

    private void writeTestFile( File dir ){
        File testFile = null;
        try {
            testFile= File.createTempFile("testFS", ".txt", dir);
            successfulAvailabiltyFileWrite = false;
            FileUtils.writeByteArrayToFile(testFile, testBytes);
            FileUtils.forceDelete(testFile);
            successfulAvailabiltyFileWrite = true;
        } catch ( IOException e ) {
            String msg = "An IOException occured while writing the testFile; " + String.valueOf(testFile);
            log.error( msg );
            throw new AtomServerException( msg, e );
        }
    }

    /**
     * {@inheritDoc}
     */
    public File getRootDir() {
        File rootDir = new File(rootDirAbsPath);
        
        if ( rootDir == null || !( rootDir.exists() && rootDir.canRead() && rootDir.isDirectory()) ) {
            String msg = "The root data directory (" + rootDirAbsPath + ") does NOT exist, or is NOT readable" ;
            log.error( msg );
            throw new AtomServerException( msg );
        }
        if ( !nfsTempFile.exists() ) {
            String msg = "The NFS tempfile - which indicates that NFS is still up -- is missing (in "
                + rootDirAbsPath + ")" + "\n NFS is DOWN" ;
            log.error( msg );
            throw new AtomServerException( msg );
        }
        return rootDir;
    }  

    /** We should NEVER keep the real File around for rootDir.
     * This is because we use NFS (at least in PRD) so there is the very real
     * possibility that the rootDir could vanish. Thus, we MUST recreate the File
     * every time we use it. This way we can recover without requiring a restart.
     *
     * Also, along these lines, we create a temporary file at the rootDir, and then check 
     * for its existence. If it vanishes, then NFS has vanished. And it'll be there when it comes back...
     */
    private void initializeRootDir( File rootDir ) {
        rootDir.mkdirs();
        this.rootDirAbsPath = rootDir.getAbsolutePath();
        setNFSTempFile( rootDir );
    }

    private void setNFSTempFile( File rootDir ) {
        try {
            this.nfsTempFile = File.createTempFile( "NFS-indicator-", ".tmp", rootDir );
            this.nfsTempFile.deleteOnExit();
        } catch ( IOException ee ) {           
            String msg = "The NFS tempfile - which indicates that NFS is still up -- cannot be created (in "
                + rootDirAbsPath + ")";
            log.error( msg );
            throw new AtomServerException( msg, ee );
        }
    }

    //=========================
    /**
     * {@inheritDoc}
     */
    public List<String> listCollections(String workspace) {
        List<String> collections = new ArrayList<String>();
        for (File file : pathFromRoot(workspace).listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.exists() &&
                       pathname.isDirectory() &&
                       pathname.canRead() &&
                       pathname.canWrite() &&
                       !pathname.isHidden();
            }
        })) {
            collections.add(file.getName());
        }
        return collections;
    }

    public void initializeWorkspace(String workspace) {
        pathFromRoot(workspace).mkdirs();
    }

    /**
     * {@inheritDoc}
     */
    public boolean workspaceExists( String workspace ) {
        File workspaceDir = pathFromRoot(workspace);
        return (workspaceDir.exists() && workspaceDir.isDirectory());
    }

    /**
     * {@inheritDoc}
     */
    public boolean collectionExists(String workspace, String collection) {
        File collectionDir = pathFromRoot(workspace, collection);
        return (collectionDir.exists() && collectionDir.isDirectory());
    }

    /**
     * {@inheritDoc}
     */
    public void createCollection( String workspace, String collection ) {
        if ( collectionExists( workspace, collection ) )
            return;           
        File collectionDir = pathFromRoot(workspace, collection);
        try {
            collectionDir.mkdirs() ;
        } catch (SecurityException e) {
            String msg = "collection " + workspace + "/" + collection +
                " does not exist and could not be created.";
            log.error(msg, e);
            throw new AtomServerException( msg, e );
        }
    }

    /**
     * construct a EntryStore object to store entry data.  <br/>
     * NOTE: This CTOR is intended for use by the IOC container
     *
     * @param rootDir the root directory for the file-based store
     *                this is the dir at which you would find the workspaces (e.g. "widgets")
     */
    public FileBasedContentStorage( File rootDir) {
        // set up the root directory
        initializeRootDir( rootDir );
    }

    /**
     * Used by IOC container to enable/disable sweeping excess revisions to a separate trash dir
     * @param sweepToTrash
     */
    @ManagedAttribute
    public void setSweepToTrash( boolean sweepToTrash ) {
        this.sweepToTrash = sweepToTrash;
    }
    @ManagedAttribute
    public boolean getSweepToTrash() {
        return sweepToTrash;
    }

    /**
     * Used by IOC container to set the time (in Seconds) to lag when sweeping excess
     *  revisions to a separate trash dir
     * @param sweepToTrashLagTimeSecs
     */
    @ManagedAttribute
    public void setSweepToTrashLagTimeSecs(int sweepToTrashLagTimeSecs) {
        this.sweepToTrashLagTimeSecs = sweepToTrashLagTimeSecs;
    }
    @ManagedAttribute
    public int getSweepToTrashLagTimeSecs() {
        return sweepToTrashLagTimeSecs;
    }

    public boolean canRead() {
        return getRootDir().exists() && getRootDir().canRead();
    }


    /**
     * {@inheritDoc}
     *
     * We have added retries to the getContent() method. This addresses a problem we have 
     * seen with NFS file systems, where a GET that closely follows a PUT (within a second) may incorrectly 
     * report that the file does not exist, resulting in a 500 Error. We now retry MAX_RETRIES times, 
     * sleeping SLEEP_BETWEEN_RETRY ms between each attempt
     */
    public String getContent(EntryDescriptor descriptor) {

        // FOR TESTING ONLY
        if ( testingFailOnGet ) {
            throw new RuntimeException( "THIS IS A FAKE FAILURE FROM testingFailOnGet" );
        }

        if ( descriptor.getRevision() == EntryDescriptor.UNDEFINED_REVISION )  {
            String msg = "The revision number is UNDEFINED when attempting to GET the XML file for " 
                         + descriptor;
            log.error(msg);
            throw new AtomServerException(msg);
        }

        String result = null;
        int retries = 0; 
        boolean finished = false; 
        IOException exceptionThrown = null;

        while ( !finished && (retries < MAX_RETRIES) ) { 
            result = null;
            exceptionThrown = null;  
            try {
                File file = getFileLocation(descriptor.getWorkspace(),
                                            descriptor.getCollection(),
                                            descriptor.getEntryId(),
                                            descriptor.getLocale(),
                                            descriptor.getRevision());
                if ( file == null ) {
                    log.warn( "getFileLocation() returned NULL getting XML data for entry::  " + descriptor );
                } else { 
                    // FOR TESTING ONLY
                    if ( isAlternatelyPass( testingAlternatelyFailOnFileReadException,
                                            ++testingAlternatelyFailOnFileReadExceptionCount, 
                                            testingAlternatelyFailOnFileReadExceptionPassCount ) ) {
                        throw new IOException( "THIS IS A FAKE FAILURE FROM testingFailOnFileReadException" );
                    }

                    result = FileUtils.readFileToString(file, "UTF-8");
                }
            } catch ( IOException ioe ) {
                log.warn( "IOException getting XML data for entry " + descriptor + " Caused by " +  ioe.getMessage() );
                exceptionThrown = ioe; 
            }

            if ( (exceptionThrown == null) && (result != null) ) {
                finished = true; 
            } else { 
                try { Thread.sleep( SLEEP_BETWEEN_RETRY ); } 
                catch( InterruptedException ee ) {} 
                retries++; 
            }
        }

        if ( exceptionThrown != null ) {
            String msg = MessageFormat.format("IOException getting XML data for entry {0} :: Reason {1}",
                                              descriptor, exceptionThrown.getMessage() );
            log.error(msg, exceptionThrown);
            throw new AtomServerException(msg, exceptionThrown);
        }      
        return result;
    }


    /**
     * {@inheritDoc}
     */
    public void putContent(String entryXml, EntryDescriptor descriptor) {

        if ( descriptor.getRevision() == EntryDescriptor.UNDEFINED_REVISION )  {
            String msg = "The revision number is UNDEFINED when attempting to PUT the XML file for "
                         + descriptor;
            log.error(msg);
            throw new AtomServerException(msg);
        }

        File xmlFile = null;
        try {
            // compute the filename
            xmlFile = convertToFile(descriptor, true);

            if ( log.isTraceEnabled() ){
                log.trace( "Preparing to write XML file:: "+ descriptor + " XML file:: " + xmlFile );
            }

            // FOR TESTING ONLY
            if ( isAlternatelyFail( testingAlternatelyFailOnPut, ++testingAlternatelyFailOnPutCount, 2 ) ) {
                throw new RuntimeException( "THIS IS A FAKE FAILURE FROM testingAlternatelyFailOnPut" );
            }

            // marshall the entry to that file
            FileUtils.writeStringToFile( xmlFile, entryXml, UTF_8 );

            // move ANY files except the just created  revision to "_trash" dir
            // NOTE: cleanupExcessFiles will NOT throw any Exceptions
            cleanupExcessFiles( xmlFile, descriptor );

        } catch ( Exception ee ) {
            String errmsg = MessageFormat.format("Exception putting XML data for entry {0}   Reason:: {1}",
                                                 descriptor, ee.getMessage());
            if ( xmlFile != null && xmlFile.exists() ) {
                errmsg += "\n!!!!!!!!! WARNING !!!!!!!! The file (" + xmlFile + ") exists BUT the write FAILED";
            }            
            log.error(errmsg, ee);
 
            // must throw RuntimeExceptions for Spring Txn rollback...
            throw new AtomServerException(errmsg, ee);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean contentExists(EntryDescriptor descriptor) {
        File location = getFileLocation(descriptor.getWorkspace(),
                                        descriptor.getCollection(),
                                        descriptor.getEntryId(),
                                        descriptor.getLocale(),
                                        descriptor.getRevision());
        return location != null && location.exists();
    }

    /**
     * {@inheritDoc}
     */
    public void revisionChangedWithoutContentChanging(EntryDescriptor descriptor) {
        File fileSansRevision = convertToFileSansRevision( descriptor );

        int revision = descriptor.getRevision();
        File newFile = createRevisionFile( fileSansRevision, revision );


        int lastRev = revision - 1;
        if ( log.isTraceEnabled() )
           log.trace( "revision= " + revision );
        if ( lastRev < 0 ) {
            String msg = "Last revision is 0 or less, which should NOT be possible (" + descriptor + ")";
            log.error( msg );
            throw new AtomServerException( msg );
        }

        File prevFile = createRevisionFile( fileSansRevision, lastRev );

        if ( !(prevFile.exists()) ) {
            String msg = "Last revision file does NOT exist [" + prevFile + "] (" + descriptor + ")";
            log.error( msg );
            throw new AtomServerException( msg );
        }

        try {
            if ( log.isTraceEnabled() )
               log.trace( "COPYING previous file = " + prevFile + " to " + newFile );

            FileUtils.copyFile(prevFile, newFile);

            // move ANY files except the just created  revision to "_trash" dir
            cleanupExcessFiles( newFile, descriptor );

        } catch (IOException e) {
            log.error("ERROR COPYING TO FILE! (" + prevFile + ")", e);
        }
    }


    /**
     * {@inheritDoc}
     */
    public void deleteContent(String deletedContentXml, EntryDescriptor descriptor) {
        if ( deletedContentXml == null ) 
            convertToFile(descriptor, false).delete();
        else 
            putContent( deletedContentXml, descriptor );
    }

    public void obliterateContent(EntryDescriptor descriptor) {
        String workspace = descriptor.getWorkspace();
        String collection = descriptor.getCollection();
        String entryId = descriptor.getEntryId();
        Locale locale = descriptor.getLocale();

        // get the root dir for the entry
        File entryDir = getEntryDir(workspace, collection, entryId);
        // get the localized directory for the entry (this MAY be the same thing, if the entry is not localized)
        File localizedDir = getLocaleDirectory(workspace, collection, entryId, locale);
        try {
            // delete the localized entry
            FileUtils.deleteDirectory(localizedDir);
            // if the entry dir is now empty (because we just deleted the last thing in it) then delete it too
            if (entryDir.exists() && entryDir.listFiles().length == 0) {
                FileUtils.deleteDirectory(entryDir);
            }
        } catch (Exception e) {
            // if anything goes wrong, throw a AtomServerException, which will ultimately result in a 500 for the end user
            throw new AtomServerException("exception obliterating content from storage for " + descriptor + ".", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long lastModified(EntryDescriptor descriptor) {
        File file = getFileLocation(descriptor.getWorkspace(),
                                    descriptor.getCollection(),
                                    descriptor.getEntryId(),
                                    descriptor.getLocale(),
                                    descriptor.getRevision() );
        return file == null ? 0L : file.lastModified();
    }

    private File convertToFile(EntryDescriptor descriptor, boolean mkdirs) {
        File fileSansRevision = convertToFileSansRevision( descriptor, mkdirs );
        return createRevisionFile( fileSansRevision,  descriptor.getRevision() );
    }

    private File convertToFileSansRevision( EntryDescriptor descriptor) {
        return convertToFileSansRevision( descriptor, false );
    }

    private File convertToFileSansRevision( EntryDescriptor descriptor, boolean mkdirs) {
        String entryId = descriptor.getEntryId();

        // figure out the specific directory on disk where the file should be written
        File dir = getLocaleDirectory(descriptor.getWorkspace(), descriptor.getCollection(), entryId, descriptor.getLocale());

        // ensure that the directory exists
        if (mkdirs) {
            dir.mkdirs();
        }
        return new File(dir, getFileName( entryId, NO_REVISION )  );
    }

    private File createRevisionFile( File fileSansRevision,  int revision ) {
        String newFileName =  fileSansRevision.getAbsolutePath() + ".r" + revision;
        return new File( newFileName  );
    }

    private String getFileName( String entryId, int revision ) {
        String fileName = entryId + ".xml";
        if ( revision != NO_REVISION ) {
           fileName += ".r" + revision;
        }
        return fileName;
    }

    public EntryDescriptor getEntryMetaData(String filePath) {
        return getMetaDataFromFilePath(new File(filePath));
    }

    public File getFileLocation(String workspace,
                                String collection,
                                String entryId,
                                Locale locale,
                                int revision) {

        // FOR TESTING ONLY
        if ( isAlternatelyPass( testingAlternatelyFailOnFileReadNull, 
                                ++testingAlternatelyFailOnFileReadNullCount,
                                testingAlternatelyFailOnFileReadNullPassCount  ) ) {
            return null;
        }

        // figure out the collection dir and the most specific dir (based on locale) where the
        // entry might be written
        File dir = getLocaleDirectory(workspace, collection, entryId, locale);

        File systemDir = pathFromRoot(workspace, collection);

        // find the location of the entry file, walking backwards down the locale tree if needed
        if ( log.isDebugEnabled() ) 
            log.debug(MessageFormat.format("looking for entry {0} in {1}, rooted at {2}, revision {3}",
                                            entryId, dir, systemDir, revision));

        String fileName = getFileName( entryId, revision );
        File xmlFile = null;
        if ( dir.exists() ) {
            xmlFile = new File( dir, fileName );
            if ( ! xmlFile.exists() ) {
                xmlFile = null;
                if ( revision != -1 ) {
                    log.warn( "The file (" + fileName + ") does NOT exist in directory:: " + dir );
                }
            }
        } else {
            xmlFile = null;
            log.warn( "The directory:: " + dir + " does NOT exist. Could NOT locate file= " + fileName );
        }

        if ( log.isDebugEnabled() ) 
            log.debug(MessageFormat.format("Found entry file: {0}", xmlFile));
        return xmlFile;
    }


    /**
     * get the directory to store the entry with the given collection and entryId, in the given locale.
     *
     * @param workspace  the workspace of the entry to retrieve
     * @param collection the collection of the entry to retrieve
     * @param entryId    the entryId of the entry to retrieve
     * @param locale     the locale to use for localizing the directory
     * @return a File handle to the requested directory
     */
    private File getLocaleDirectory(String workspace,
                                    String collection,
                                    String entryId,
                                    Locale locale) {
        File dir = getEntryDir(workspace, collection, entryId);
        if (locale != null) {
            if (locale.getLanguage() != null) {
                dir = new File(dir, locale.getLanguage());
                if (locale.getCountry() != null) {
                    dir = new File(dir, locale.getCountry());
                    if (locale.getVariant() != null) {
                        dir = new File(dir, locale.getVariant());
                    }
                }
            }
        }
        return dir;
    }

    /**
     * get the root directory for the entry with the given collection and entryId.
     *
     * @param workspace  the workspace of the entry to retrieve
     * @param collection the collection of the entry to retrieve
     * @param entryId    the entryId of the entry to retrieve
     * @return a File handle to the requested directory
     */
    private File getEntryDir(String workspace,
                             String collection,
                             String entryId) {
        return pathFromRoot(workspace, collection, entryId.substring(0, Math.min(2, entryId.length())), entryId);
    }

    private EntryDescriptor getMetaDataFromFilePath(File file) {
        String filePath = file.getAbsolutePath();
        if ( log.isDebugEnabled() ) 
            log.debug("file path = " + filePath);
        String rootPath = getRootDir().getAbsolutePath();
        if ( log.isDebugEnabled() ) 
            log.debug("root path = " + rootPath);
        String relativePath = filePath.replace(rootPath, "");
        if ( log.isDebugEnabled() ) 
            log.debug("relative path = " + relativePath);
        Matcher matcher = FILE_PATH_PATTERN.matcher(relativePath);

        if ( matcher.matches() ) {
            String workspace = matcher.group(1);
            String collection = matcher.group(2);
            String entryId = matcher.group(3);
            Locale locale = StringUtils.isEmpty(matcher.group(4))
                            ? null
                            : LocaleUtils.toLocale(matcher.group(4).replace('/', '_'));
            if ( matcher.group(5) == null ) {
                String msg = "Revision is not specified for file= " + file;
                log.error( msg );
                throw new AtomServerException( msg );
            }
            int revision = EntryDescriptor.UNDEFINED_REVISION;
            try { revision = Integer.parseInt(matcher.group(5)); }
            catch ( NumberFormatException ee ) {
               String msg = "Could not parse revision from file= " + file;
               log.error( msg );
               throw new AtomServerException( msg );
            }
            return new BaseEntryDescriptor( workspace, collection, entryId, locale, revision );

        } else {
            return null;
        }
    }

    private File pathFromRoot(String... parts) {
        File file = getRootDir();
        for (String part : parts) {
            file = new File(file, part);
        }
        return file;
    }

    /**
     * removes all of the files, except the file passed in, to the trash bin for deletion.
     * The trash dir is meant to be cleaned up by some external process.
     * NOTE: this method does not throw an Exception. Instead, it simply logs errors and moves on.
     *
     * @param thisRev
     * @param descriptor
     */
    private void cleanupExcessFiles( final File thisRev, final EntryDescriptor descriptor ) {

        if ( !sweepToTrash ) {
            return;
        }

        String fullPath = FilenameUtils.getFullPath( thisRev.getAbsolutePath() );
        File baseDir = new File( fullPath );
        if ( log.isTraceEnabled() )
            log.trace("cleaning up excess files at " + baseDir + " based on " + thisRev );

        try {
            // get a file pointer at the file we just wrote, and the one for the prev rev
            final File oneRevBack = getFileLocation(descriptor.getWorkspace(),
                                                    descriptor.getCollection(),
                                                    descriptor.getEntryId(),
                                                    descriptor.getLocale(),
                                                    descriptor.getRevision() - 1);

            // list out all of the files in the directory that are (a) files, and (b) not one of
            // the last two revisions
            final File[] toDelete = baseDir.listFiles(new FileFilter() {
                public boolean accept(File fileToCheck) {

                    return fileToCheck != null &&
                           fileToCheck.exists() &&
                           fileToCheck.isFile() &&
                           fileToCheck.canRead() &&
                           fileToCheck.canWrite() &&
                           !fileToCheck.isHidden() &&
                           !thisRev.equals(fileToCheck) &&
                           (oneRevBack == null || !oneRevBack.equals(fileToCheck)) &&
                           ((System.currentTimeMillis() - fileToCheck.lastModified()) > sweepToTrashLagTimeSecs*1000L );

                }
            });

            // if there's anything to delete...
            if (toDelete != null && toDelete.length > 0) {

                // first of all, there needs to be a "_trash" subdirectory,
                // so we make sure that exists
                File trashDir = new File( baseDir, TRASH_DIR_NAME);
                trashDir.mkdirs();

                // and move the files into it
                for (File file : toDelete) {
                    File moveTo = new File(trashDir, file.getName());
                    if ( !file.renameTo( moveTo ) ) {
                        throw new IOException( "When cleaning up excess revisions, could not move the file ("
                                               + file + ") to (" + moveTo + ")");
                    }
                }
           }
        }
        catch (Exception e) {
            // if there was any exception in the move (including the one we might have just thrown
            // above) then we should log it
            log.error( "Error when cleaning up dir [" + baseDir + "] when writing file (" + thisRev + ")", e );
        }
    }

}

