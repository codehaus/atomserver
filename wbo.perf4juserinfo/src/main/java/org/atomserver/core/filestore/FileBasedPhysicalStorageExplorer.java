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

import org.atomserver.exceptions.AtomServerException;
import org.atomserver.EntryDescriptor;
import org.atomserver.core.filestore.FileBasedContentStorage;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.perf4j.StopWatch;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Base class to encapsulate logic for walking a file-based physical entry storage hierarchy.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public abstract class FileBasedPhysicalStorageExplorer {
    private static final Log log = LogFactory.getLog(FileBasedPhysicalStorageExplorer.class);

    /**
     * The store for the purposes of this explorer instance
     */
    private FileBasedContentStorage physicalStorage = null;

    private List<String> childDirectories;


    public void setChildDirectories(List<String> childDirectories) {
        this.childDirectories = childDirectories;
    }

    public void setContentStorage(FileBasedContentStorage physicalStorage) {
        this.physicalStorage = physicalStorage;
    }

    public FileBasedContentStorage getContentStorage() {
        return physicalStorage;
    }

    /**
     * Override this method to create your own implementation of the StoreExplorer.
     * Called each time a file is encountered during the exploration process.
     *
     * @param pid  the pid
     * @param file the file
     */
    protected abstract void handleFile(EntryDescriptor pid, File file) throws AtomServerException;

    /**
     * Override to perform operations prior to the exploration process.  By default, does nothing.
     *
     * @throws org.atomserver.exceptions.AtomServerException
     */
    protected void beforeExploration() throws AtomServerException {
    }

    /**
     * Override to perform opertations after the exploration process.  By default, does nothing.
     *
     * @throws org.atomserver.exceptions.AtomServerException
     */
    protected void afterExploration() throws AtomServerException {
    }

    /**
     * Internal handler for each file encountered by the Explorer.
     *
     * @param pid  the id of the document
     * @param file the file
     */
    private void handleFileInternal(EntryDescriptor pid, File file) throws AtomServerException {
        log.info(MessageFormat.format("Exploring document {0} - {1}", pid, file.getPath()));
        handleFile(pid, file);
    }

    /**
     * starts the exploration process.  requires that you set a physicalStorage
     *
     * @throws org.atomserver.exceptions.AtomServerException
     */
    public void explore() throws AtomServerException {

        if (this.physicalStorage == null) {
            throw new IllegalArgumentException("Store is null.  You must provide a store before calling the explore() method");
        }

        File rootDir = this.physicalStorage.getRootDir();

        log.debug(MessageFormat.format("Walking the store at {0}", rootDir.getPath()));

//        SimpleStopWatch stopWatch = new SimpleStopWatch();
//        stopWatch.start();
        StopWatch stopWatch = new AtomServerStopWatch();

        try {
            beforeExploration();

            List<String> childDirs = null;

            //allows the user to specify a child directory, otherwise it walks all top level directories
            if (this.childDirectories != null) {
                log.info(MessageFormat.format("User specified child directories found ({0}) will not walk top level directories", this.childDirectories));
                childDirs = childDirectories;
            } else {
                log.info("No user specified child directory.  Exploring all top level directories.");
                childDirs = new ArrayList<String>();
                Collection files = FileUtils.listFiles(rootDir, null, false);
                for (Iterator fileIter = files.iterator(); fileIter.hasNext();) {
                    File subFile = (File) fileIter.next();
                    if (subFile.isDirectory()
                        && !subFile.isHidden()) {
                        log.info(MessageFormat.format("Discovered top level directory {0}", subFile.getName()));
                        childDirs.add(subFile.getName());
                    }
                }
            }

            //iterate over each child dir and explore each one
            for (String childDir : childDirs) {
                log.info(MessageFormat.format("Exploring store {0}, child {1}", rootDir.getPath(), childDir));

                boolean foundFiles = explore(rootDir, childDir);
                if (!foundFiles) {
                    throw new AtomServerException("Did not find any files in " + rootDir + " " + childDir);
                }
            }

            afterExploration();
        }
        finally {
            stopWatch.stop("Exploring the property store","");
        }

//        stopWatch.stop();
//        log.warn("Exploring the property store took " + stopWatch.getElapsed() + " secs");
    }

    /**
     * Recursively explore the property store while triggering handleFileInternal events
     *
     * @return boolean indicating whether any files have been found
     */
    private boolean explore(File baseDir, String dirToProcess) throws AtomServerException {
        boolean foundFile = false;

        File[] files = new File(baseDir, dirToProcess).listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; ++i) {
                if (files[i].isDirectory()) {
                    String pathInfo[] = files[i].getPath().split("/");
                    String thisDir = pathInfo[pathInfo.length - 1];

                    foundFile = (explore(files[i].getParentFile(), thisDir)) ? true : foundFile;

                } else if (files[i].isFile()
                           && !files[i].isHidden()
                           && (FilenameUtils.getExtension(files[i].getName())).equals("xml")) {

                    File actualFile = files[i];
                    EntryDescriptor pid = ((FileBasedContentStorage)physicalStorage)
                            .getEntryMetaData(actualFile.getAbsolutePath());
                    if (log.isDebugEnabled()) {
                        log.debug("actualFile= " + actualFile + " pid = " + pid);
                    }

                    if (pid == null) {
                        continue;
                    }

                    foundFile = true;

                    // handles the discovery of a store file
                    handleFileInternal(pid, actualFile);
                }
            }
        }
        return foundFile;
    }
}
