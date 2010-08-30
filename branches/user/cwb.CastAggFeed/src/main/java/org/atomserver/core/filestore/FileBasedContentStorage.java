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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.ContentStorage;
import org.atomserver.EntryDescriptor;
import org.atomserver.core.BaseEntryDescriptor;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.utils.PartitionPathGenerator;
import org.atomserver.utils.PrefixPartitionPathGenerator;
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * File-based implementation of physical storage for entries.
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
@ManagedResource(description = "Content Storage")
public class FileBasedContentStorage implements ContentStorage {

    static private Log log = LogFactory.getLog(FileBasedContentStorage.class);
    static private final String UTF_8 = "UTF-8";

    public static final int MAX_RETRIES = 3;
    public static final int SLEEP_BETWEEN_RETRY = 750;

    private static final String GZIP_EXTENSION = ".gz";

    private static final Pattern FILE_PATH_WORKSPACE_COLLECTION_PATTERN =
            Pattern.compile("^/?(\\w+)/(\\w+)/.*$");

    private static final Pattern FILE_PATH_LOCALE_REV_PATTERN =
            Pattern.compile("^/?((?:[a-z]{2})?(?:/[A-Z]{2})?)/\\w+\\.xml.r(\\d+)(?:" + GZIP_EXTENSION + ")?$");

    static public final String TRASH_DIR_NAME = "_trash";

    static private final int NO_REVISION = -777;

    static private final int SWEEP_TO_TRASH_LAG_TIME_SECS_DEFAULT = 120;

    //============================================
    private File nfsTempFile = null;
    private String rootDirAbsPath = null;

    private boolean sweepToTrash = true;

    private int sweepToTrashLagTimeSecs = SWEEP_TO_TRASH_LAG_TIME_SECS_DEFAULT;

    static private final String TRASH_LOG_NAME = "org.atomserver.trash";
    static private Log trashLog = LogFactory.getLog(TRASH_LOG_NAME);

    //=========================
    // The following methods are for testing purposes ONLY
    private boolean successfulAvailabiltyFileWrite = false;

    public void testingResetNFSTempFile() {
        setNFSTempFile((new File(rootDirAbsPath)));
    }

    public File testingGetNFSTempFile() {
        return nfsTempFile;
    }

    public void testingSetRootDirAbsPath(String rootDirAbsPath) {
        this.rootDirAbsPath = rootDirAbsPath;
    }

    public boolean testingWroteAvailabiltyFile() {
        return successfulAvailabiltyFileWrite;
    }

    public static Log getTrashLog() {
        return trashLog;
    }//====================================

    public static void setTrashLog(Log trashLog) {
        FileBasedContentStorage.trashLog = trashLog;
    }

    /**
     * This method is used by the isAliveHandler to determine if the ContentStorage is alive and well
     */
    public void testAvailability() {
        // check the root dir, see if any exceptions get thrown.
        File rootDir = getRootDir();

        // we must actually write a file, because we may have "disk is full" problems
        writeTestFile(rootDir);
    }

    private static byte[] testBytes = new byte[1024];

    private void writeTestFile(File dir) {
        File testFile = null;
        try {
            testFile = File.createTempFile("testFS", ".txt", dir);
            successfulAvailabiltyFileWrite = false;
            FileUtils.writeByteArrayToFile(testFile, testBytes);
            FileUtils.forceDelete(testFile);
            successfulAvailabiltyFileWrite = true;
        } catch (IOException e) {
            String msg = "An IOException occured while writing the testFile; " + String.valueOf(testFile);
            log.error(msg);
            throw new AtomServerException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public File getRootDir() {
        File rootDir = new File(rootDirAbsPath);

        if (!(rootDir.exists() && rootDir.canRead() && rootDir.isDirectory())) {
            String msg = "The root data directory (" + rootDirAbsPath + ") does NOT exist, or is NOT readable";
            log.error(msg);
            throw new AtomServerException(msg);
        }
        if (!nfsTempFile.exists()) {
            String msg = "The NFS tempfile - which indicates that NFS is still up -- is missing (in "
                         + rootDirAbsPath + ")" + "\n NFS is DOWN";
            log.error(msg);
            throw new AtomServerException(msg);
        }
        return rootDir;
    }

    /**
     * We should NEVER keep the real File around for rootDir.
     * This is because we use NFS (at least in PRD) so there is the very real
     * possibility that the rootDir could vanish. Thus, we MUST recreate the File
     * every time we use it. This way we can recover without requiring a restart.
     * <p/>
     * Also, along these lines, we create a temporary file at the rootDir, and then check
     * for its existence. If it vanishes, then NFS has vanished. And it'll be there when it comes back...
     *
     * @param rootDir the root directory where our content storage lives
     */
    private void initializeRootDir(File rootDir) {
        rootDir.mkdirs();
        this.rootDirAbsPath = rootDir.getAbsolutePath();
        setNFSTempFile(rootDir);
    }

    private void setNFSTempFile(File rootDir) {
        try {
            this.nfsTempFile = File.createTempFile("NFS-indicator-", ".tmp", rootDir);
            this.nfsTempFile.deleteOnExit();
        } catch (IOException ee) {
            String msg = "The NFS tempfile - which indicates that NFS is still up -- cannot be created (in "
                         + rootDirAbsPath + ")";
            log.error(msg);
            throw new AtomServerException(msg, ee);
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
    public boolean workspaceExists(String workspace) {
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
    public void createCollection(String workspace, String collection) {
        if (collectionExists(workspace, collection)) {
            return;
        }
        File collectionDir = pathFromRoot(workspace, collection);
        try {
            collectionDir.mkdirs();
        } catch (SecurityException e) {
            String msg = "collection " + workspace + "/" + collection +
                         " does not exist and could not be created.";
            log.error(msg, e);
            throw new AtomServerException(msg, e);
        }
    }

    /**
     * construct a EntryStore object to store entry data.  <br/>
     * NOTE: This CTOR is intended for use by the IOC container
     *
     * @param rootDir the root directory for the file-based store
     *                this is the dir at which you would find the workspaces (e.g. "widgets")
     */
    public FileBasedContentStorage(File rootDir) {
        // set up the root directory
        initializeRootDir(rootDir);
    }

    // Used by IOC container to enable/disable sweeping excess revisions to a separate trash dir
    @ManagedAttribute
    public void setSweepToTrash(boolean sweepToTrash) {
        this.sweepToTrash = sweepToTrash;
    }

    @ManagedAttribute
    public boolean getSweepToTrash() {
        return sweepToTrash;
    }

    // Used by IOC container to set the time (in Seconds) to lag when sweeping excess revisions
    // to a separate trash dir
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
     * <p/>
     * We have added retries to the getContent() method. This addresses a problem we have
     * seen with NFS file systems, where a GET that closely follows a PUT (within a second) may incorrectly
     * report that the file does not exist, resulting in a 500 Error. We now retry MAX_RETRIES times,
     * sleeping SLEEP_BETWEEN_RETRY ms between each attempt
     * <p/>
     * TODO: per Chris, we may be able to simplify this and remove the retries, because we never overwrite the same file multiple times anymore.
     */
    public String getContent(EntryDescriptor descriptor) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {

            if (descriptor.getRevision() == EntryDescriptor.UNDEFINED_REVISION) {
                String msg = "The revision number is UNDEFINED when attempting to GET the XML file for "
                             + descriptor;
                log.error(msg);
                throw new AtomServerException(msg);
            }

            String result = null;
            int retries = 0;
            boolean finished = false;
            IOException exceptionThrown = null;

            while (!finished && (retries < MAX_RETRIES)) {
                result = null;
                exceptionThrown = null;
                try {
                    File file = findExistingEntryFile(descriptor);

                    if (file == null) {
                        log.warn("getFileLocation() returned NULL getting XML data for entry::  " + descriptor);
                    } else {
                        result = readFileToString(file);
                    }
                } catch (IOException ioe) {
                    log.warn("IOException getting XML data for entry " + descriptor + " Caused by " + ioe.getMessage());
                    exceptionThrown = ioe;
                }

                if ((exceptionThrown == null) && (result != null)) {
                    finished = true;
                } else {
                    try { Thread.sleep(SLEEP_BETWEEN_RETRY); }
                    catch (InterruptedException ee) {
                        // never interrupted
                    }
                    retries++;
                }
            }

            if (exceptionThrown != null) {
                String msg = MessageFormat.format("IOException getting XML data for entry {0} :: Reason {1}",
                                                  descriptor, exceptionThrown.getMessage());
                log.error(msg, exceptionThrown);
                throw new AtomServerException(msg, exceptionThrown);
            }
            return result;

        } finally {
            stopWatch.stop("XML.fine.getFileContent", AtomServerPerfLogTagFormatter.getPerfLogEntryString(descriptor));
        }

    }


    /**
     * {@inheritDoc}
     */
    public void putContent(String entryXml, EntryDescriptor descriptor) {

        if (descriptor.getRevision() == EntryDescriptor.UNDEFINED_REVISION) {
            String msg = "The revision number is UNDEFINED when attempting to PUT the XML file for "
                         + descriptor;
            log.error(msg);
            throw new AtomServerException(msg);
        }

        File xmlFile = null;
        try {
            // compute the filename
            xmlFile = getEntryFileForWriting(descriptor);
            xmlFile.getParentFile().mkdirs();

            if (log.isTraceEnabled()) {
                log.trace("%> Preparing to write XML file:: " + descriptor + " XML file:: " + xmlFile);
            }

            // marshall the entry to that file
            writeStringToFile(entryXml, xmlFile);

            // move ANY files except the just created  revision to "_trash" dir
            // NOTE: cleanupExcessFiles will NOT throw any Exceptions
            cleanupExcessFiles(xmlFile, descriptor);

        } catch (Exception ee) {
            String errmsg = MessageFormat.format("Exception putting XML data for entry {0}   Reason:: {1}",
                                                 descriptor, ee.getMessage());
            if (xmlFile != null && xmlFile.exists()) {
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
        return findExistingEntryFile(descriptor) != null;
    }

    /**
     * {@inheritDoc}
     */
    public void revisionChangedWithoutContentChanging(EntryDescriptor descriptor) {
        File newFile = getEntryFileForWriting(descriptor);

        int lastRev = descriptor.getRevision() - 1;
        if (log.isTraceEnabled()) {
            log.trace("%> revision= " + descriptor.getRevision());
        }
        if (lastRev < 0) {
            String msg = "Last revision is 0 or less, which should NOT be possible (" + descriptor + ")";
            log.error(msg);
            throw new AtomServerException(msg);
        }

        File prevFile = findExistingEntryFile(descriptor, 1);

        if (prevFile == null) {
            String msg = "Last revision file does NOT exist [" + prevFile + "] (" + descriptor + ")";
            log.error(msg);
            throw new AtomServerException(msg);                                         
        }

        try {
            if (log.isTraceEnabled()) {
                log.trace("%> COPYING previous file = " + prevFile + " to " + newFile);
            }

            newFile.getParentFile().mkdirs();
            writeStringToFile(readFileToString(prevFile), newFile);

            cleanupExcessFiles(newFile, descriptor);

        } catch (IOException e) {
            log.error("ERROR COPYING TO FILE! (" + prevFile + ")", e);
        }
    }


    /**
     * {@inheritDoc}
     */
    public void deleteContent(String deletedContentXml, EntryDescriptor descriptor) {
        if (deletedContentXml == null) {
            File file = findExistingEntryFile(descriptor);
            if (file != null) {
                file.delete();
            }
        } else {
            putContent(deletedContentXml, descriptor);
        }
    }

    public void obliterateContent(EntryDescriptor descriptor) {
        File entryFile = findExistingEntryFile(descriptor);
        try {
            if(entryFile != null) {
                FileUtils.deleteDirectory(entryFile.getParentFile());
                cleanUpToCollection(descriptor, entryFile);
            }
        } catch (Exception e) {
            throw new AtomServerException("exception obliterating content from storage for " + descriptor + ".", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long lastModified(EntryDescriptor descriptor) {
        File file = findExistingEntryFile(descriptor);
        return file == null ? 0L : file.lastModified();
    }


    private String getFileName(String entryId, int revision, boolean gzipped) {
        String fileName = entryId + ".xml";
        if (revision != NO_REVISION) {
            fileName += ".r" + revision;
        }
        if (gzipped) {
            fileName += GZIP_EXTENSION;
        }
        return fileName;
    }

    public EntryDescriptor getEntryMetaData(String filePath) {
        return getMetaDataFromFilePath(new File(filePath));
    }

    public Object getPhysicalRepresentation(String workspace,
                                            String collection,
                                            String entryId,
                                            Locale locale,
                                            int revision) {
        return findExistingEntryFile(
                new BaseEntryDescriptor(workspace, collection, entryId, locale, revision));
    }

    private EntryDescriptor getMetaDataFromFilePath(File file) {
        String filePath = file.getAbsolutePath();
        if (log.isDebugEnabled()) {
            log.debug("file path = " + filePath);
        }
        String rootPath = getRootDir().getAbsolutePath();
        if (log.isDebugEnabled()) {
            log.debug("root path = " + rootPath);
        }
        String relativePath = filePath.replace(rootPath, "");
        if (log.isDebugEnabled()) {
            log.debug("relative path = " + relativePath);
        }
        Matcher matcher =
                FILE_PATH_WORKSPACE_COLLECTION_PATTERN.matcher(relativePath);
        if (matcher.matches()) {
            String workspace = matcher.group(1);
            String collection = matcher.group(2);
            File collectionRoot = pathFromRoot(workspace, collection);

            for (PartitionPathGenerator pathGenerator : partitionPathGenerators) {
                PartitionPathGenerator.ReverseMatch match =
                        pathGenerator.reverseMatch(collectionRoot, file);
                if (match != null) {
                    String entryId = match.getSeed();
                    Matcher localeRevMatcher =
                            FILE_PATH_LOCALE_REV_PATTERN.matcher(match.getRest());
                    Locale locale;
                    int revision;
                    if (localeRevMatcher.matches()) {
                        locale = StringUtils.isEmpty(localeRevMatcher.group(1)) ?
                                 null :
                                 LocaleUtils.toLocale(localeRevMatcher.group(1).replace("/", "_"));
                        try { revision = Integer.parseInt(localeRevMatcher.group(2)); }
                        catch (NumberFormatException ee) {
                            String msg = "Could not parse revision from file= " + file;
                            log.error(msg);
                            throw new AtomServerException(msg);
                        }
                        return new BaseEntryDescriptor(workspace, collection, entryId, locale, revision);
                    }
                }
            }
        }
        return null;
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
     * @param thisRev    the file pointint at the current revision
     * @param descriptor the entry that relates to the content
     */
    private void cleanupExcessFiles(final File thisRev, final EntryDescriptor descriptor) {

        if (!sweepToTrash) {
            return;
        }

        String fullPath = FilenameUtils.getFullPath(thisRev.getAbsolutePath());
        File baseDir = new File(fullPath);
        if (log.isTraceEnabled()) {
            log.trace("%> cleaning up excess files at " + baseDir + " based on " + thisRev);
        }

        try {
            File trashDir = findExistingTrashDir(descriptor);
            if (trashDir == null) {
                trashDir = new File(thisRev.getParentFile(), TRASH_DIR_NAME);
                if (log.isTraceEnabled()) {
                    log.trace("%> no trash dir, will create one at " + trashDir + " if needed");
                }
            } else if (!trashDir.getParentFile().equals(thisRev.getParentFile())) {
                File newTrashDir = new File(thisRev.getParentFile(), TRASH_DIR_NAME);
                if (log.isTraceEnabled()) {
                    log.trace("%> trash dir " + trashDir + " will be migrated to " + newTrashDir);
                }
                trashDir.renameTo(newTrashDir);
                trashDir = newTrashDir;
            }

            // get a file pointer at the previous revision of the file -- we DON'T want to delete it
            final File oneRevBack = findExistingEntryFile(descriptor, 1);

            // the set of directories to clean is the directories that contain (A) the previous
            // revision, which may or may not be the same as the current rev, and (B) the one two
            // revisions back, which may or may not be the same as the previous rev
            Set<File> directoriesToClean = new HashSet<File>(2);

            // if there was a previous rev
            if (oneRevBack != null) {
                // add it's dir to the ones to clean
                directoriesToClean.add(oneRevBack.getParentFile());
                // and if the revision is greater than 1
                if (descriptor.getRevision() > 1) {
                    // then two revs back is a reasonable thing to ask for...
                    File twoRevsBack = findExistingEntryFile(descriptor, 2);
                    // and if it exists, add its parent to the ones to clean
                    if (twoRevsBack != null) {
                        directoriesToClean.add(twoRevsBack.getParentFile());
                    }
                }
            }

            // list out all of the files in the directory that are (a) files, and (b) not one of
            // the last two revisions
            for (File directoryToClean : directoriesToClean) {
                final File[] toDelete = directoryToClean.listFiles(new FileFilter() {
                    public boolean accept(File fileToCheck) {

                        return fileToCheck != null &&
                               fileToCheck.exists() &&
                               fileToCheck.isFile() &&
                               fileToCheck.canRead() &&
                               fileToCheck.canWrite() &&
                               !fileToCheck.isHidden() &&
                               !thisRev.equals(fileToCheck) &&
                               (oneRevBack == null || !oneRevBack.equals(fileToCheck)) &&
                               ((System.currentTimeMillis() - fileToCheck.lastModified()) > sweepToTrashLagTimeSecs * 1000L);

                    }
                });

                // if there's anything to delete...
                if (toDelete != null && toDelete.length > 0) {

                    // first of all, there needs to be a "_trash" subdirectory,
                    // so we make sure that exists
                    trashDir.mkdirs();

                    File root = getRootDir();
                    int rootDirLen = (root != null) ? root.getCanonicalPath().length() : 0;
                    // and move the files into it
                    for (File file : toDelete) {
                        File moveTo = new File(trashDir, file.getName());
                        if (!file.renameTo(moveTo)) {
                            throw new IOException("When cleaning up excess revisions, could not move the file ("
                                                  + file + ") to (" + moveTo + ")");
                        }
                        // log the deleted files so that external scripts can locate them
                        if(trashLog != null) {
                            String relativePath = moveTo.getCanonicalPath().substring(rootDirLen + 1) ; // get relateivePath
                            trashLog.info(System.currentTimeMillis()/1000 + " " + relativePath); // seconds timestamp 
                        }
                    }
                    cleanUpToCollection(descriptor, directoryToClean);
                }
            }
        }
        catch (Exception e) {
            // if there was any exception in the move (including the one we might have just thrown
            // above) then we should log it
            log.error("Error when cleaning up dir [" + baseDir + "] when writing file (" + thisRev + ")", e);
        }
    }

    private void cleanUpToCollection(EntryDescriptor descriptor,
                                     File cleanDir)
            throws IOException {
        // under no circumstances do we want to clean the collection directory.
        File stopDir = pathFromRoot(descriptor.getWorkspace(), descriptor.getCollection());

        if (log.isTraceEnabled()) {
            log.trace("cleaning " + cleanDir + ", stopping at " + stopDir);
        }

        // if the stop dir is not an ancestor of the clean dir, we are in trouble.
        if (!cleanDir.getAbsolutePath().startsWith(stopDir.getAbsolutePath())) {
            throw new AtomServerException("the directory to clean (" + cleanDir + ") is not " +
                                          "within the collection of the provided entry (" +
                                          descriptor + ").");
        }

        // as long as we are underneath the stop dir, and we are pointing at a directory that has
        // no files at all, we should delete the directory and walk up to our parent.
        while (!cleanDir.equals(stopDir) &&
               cleanDir.isDirectory() &&
               cleanDir.listFiles().length == 0) {
            if (log.isTraceEnabled()) {
                log.trace("deleting empty directory " + cleanDir);
            }
            FileUtils.deleteDirectory(cleanDir);
            cleanDir = cleanDir.getParentFile();
        }
    }

    private File findExistingTrashDir(EntryDescriptor entry) {
        if (log.isTraceEnabled()) {
            log.trace("%> looking for trash directory for entry " + entry);
        }
        for (PartitionPathGenerator pathGenerator : partitionPathGenerators) {
            File trashDir = new File(generateEntryDir(entry, pathGenerator), TRASH_DIR_NAME);
            if (log.isTraceEnabled()) {
                log.trace("%> checking trash directory path " + trashDir);
            }
            if (trashDir.exists() && trashDir.isDirectory()) {
                if (log.isTraceEnabled()) {
                    log.trace("%> trash directory " + trashDir + " exists.");
                }
                return trashDir;
            }
        }
        return null;
    }

    protected File findExistingEntryFile(EntryDescriptor entry) {
        return findExistingEntryFile(entry, 0);
    }

    protected File findExistingEntryFile(EntryDescriptor entry,
                                         int revisionsBack) {
        if (log.isTraceEnabled()) {
            log.trace("%> looking for entry file for " + entry);
        }
        for (PartitionPathGenerator pathGenerator : partitionPathGenerators) {
            if (isGzipEnabled()) {
                File entryFile = generateEntryFilePath(entry, pathGenerator, true,
                                                       entry.getRevision() - revisionsBack);
                if (log.isTraceEnabled()) {
                    log.trace("%> checking file path " + entryFile);
                }
                if (entryFile.exists()) {
                    if (log.isTraceEnabled()) {
                        log.trace("%> file path " + entryFile + " exists.");
                    }
                    return entryFile;
                }
            }
            File entryFile = generateEntryFilePath(entry, pathGenerator, false,
                                                   entry.getRevision() - revisionsBack);
            if (log.isTraceEnabled()) {
                log.trace("%> checking file path " + entryFile);
            }
            if (entryFile.exists()) {
                if (log.isTraceEnabled()) {
                    log.trace("%> file path " + entryFile + " exists.");
                }
                return entryFile;
            }
        }
        return null;
    }

    private File getEntryFileForWriting(EntryDescriptor entry) {
        return generateEntryFilePath(entry, partitionPathGenerators.get(0), isGzipEnabled(), entry.getRevision());
    }


    public File generateEntryFilePath(EntryDescriptor entry,
                                      PartitionPathGenerator pathGenerator,
                                      boolean gzipped,
                                      int revision) {
        return new File(generateEntryDir(entry, pathGenerator),
                        getFileName(entry.getEntryId(), revision, gzipped));
    }

    private File generateEntryDir(EntryDescriptor entry,
                                  PartitionPathGenerator pathGenerator) {
        File entryDir = new File(
                pathGenerator.generatePath(
                        pathFromRoot(entry.getWorkspace(), entry.getCollection()),
                        entry.getEntryId()),
                entry.getEntryId());
        if (entry.getLocale() != null) {
            if (entry.getLocale().getLanguage() != null) {
                entryDir = new File(entryDir, entry.getLocale().getLanguage());
                if (entry.getLocale().getCountry() != null) {
                    entryDir = new File(entryDir, entry.getLocale().getCountry());
                }
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("%> generated file path " + entryDir + " for entry " + entry);
        }
        return entryDir;
    }

    private List<PartitionPathGenerator> partitionPathGenerators =
            Collections.<PartitionPathGenerator>singletonList(new PrefixPartitionPathGenerator());

    public List<PartitionPathGenerator> getPartitionPathGenerators() {
        return partitionPathGenerators;
    }

    public void setPartitionPathGenerators(List<PartitionPathGenerator> partitionPathGenerators) {
        this.partitionPathGenerators = partitionPathGenerators;
    }

    private boolean gzipEnabled = false;

    public boolean isGzipEnabled() {
        return gzipEnabled;
    }

    public void setGzipEnabled(boolean gzipEnabled) {
        this.gzipEnabled = gzipEnabled;
    }

    protected String readFileToString(File file) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("reading file : " + file);
        }
        if (file.getName().endsWith(GZIP_EXTENSION)) {
            GZIPInputStream inputStream = new GZIPInputStream(new FileInputStream(file));
            String content = IOUtils.toString(inputStream, UTF_8);
            inputStream.close();
            return content;
        } else {
            return FileUtils.readFileToString(file, UTF_8);
        }
    }

    protected void writeStringToFile(String content, File file) throws IOException {
        if (file.getName().endsWith(GZIP_EXTENSION)) {
            GZIPOutputStream outputStream = new GZIPOutputStream(new FileOutputStream(file));
            IOUtils.write(content, outputStream, UTF_8);
            outputStream.close();
        } else {
            FileUtils.writeStringToFile(file, content, UTF_8);
        }
    }
}

