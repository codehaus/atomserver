package org.atomserver.core.filestore;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.ContentStorageWrapper;
import org.atomserver.EntryDescriptor;
import org.atomserver.exceptions.AtomServerException;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.annotation.ManagedAttribute;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Locale;

@ManagedResource(description = "Testing Content Storage")
// TODO: make this extend FBCS, not wrap - refactor so that getContent code is *really* tested.
public class TestingContentStorage extends ContentStorageWrapper {

    private static final Log log = LogFactory.getLog(TestingContentStorage.class);

    public TestingContentStorage() {
        System.out.println("\n\n\n\n\n\n\n\nCONSTRUCTION\n\n\n\n\n\n\n\n\n\n\n\n");
    }

    // Used by IOC container to enable/disable sweeping excess revisions to a separate trash dir
    @ManagedAttribute
    public void setSweepToTrash( boolean sweepToTrash ) {
        ((FileBasedContentStorage)getStorage()).setSweepToTrash(sweepToTrash);
    }
    @ManagedAttribute
    public boolean getSweepToTrash() {
        return ((FileBasedContentStorage)getStorage()).getSweepToTrash();
    }

    // Used by IOC container to set the time (in Seconds) to lag when sweeping excess revisions
    // to a separate trash dir
    @ManagedAttribute
    public void setSweepToTrashLagTimeSecs(int sweepToTrashLagTimeSecs) {
        ((FileBasedContentStorage)getStorage()).setSweepToTrashLagTimeSecs(sweepToTrashLagTimeSecs);
    }
    @ManagedAttribute
    public int getSweepToTrashLagTimeSecs() {
        return ((FileBasedContentStorage)getStorage()).getSweepToTrashLagTimeSecs();
    }

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

        while ( !finished && (retries < FileBasedContentStorage.MAX_RETRIES) ) {
            result = null;
            exceptionThrown = null;
            try {
                File file = (File) getPhysicalRepresentation(descriptor.getWorkspace(),
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
                try { Thread.sleep( FileBasedContentStorage.SLEEP_BETWEEN_RETRY ); }
                catch( InterruptedException ee ) {
                    // never interrupted
                }
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


    public Object getPhysicalRepresentation(String workspace, String collection, String entryId, Locale locale, int revision) {
        if ( isAlternatelyPass( testingAlternatelyFailOnFileReadNull,
                                ++testingAlternatelyFailOnFileReadNullCount,
                                testingAlternatelyFailOnFileReadNullPassCount  ) ) {
            return null;
        }

        return super.getPhysicalRepresentation(workspace, collection, entryId, locale, revision);
    }

    public void putContent(String contentXml, EntryDescriptor descriptor) {
        if ( isAlternatelyFail( testingAlternatelyFailOnPut, ++testingAlternatelyFailOnPutCount, 2 ) ) {
            throw new AtomServerException( "THIS IS A FAKE FAILURE FROM testingAlternatelyFailOnPut" );
        }

        super.putContent(contentXml, descriptor);
    }

    


    private boolean testingFailOnGet = false;

    public void setTestingFailOnGet(boolean tORf) {
        testingFailOnGet = tORf;
    }

    private boolean testingAlternatelyFailOnFileReadException = false;
    private int testingAlternatelyFailOnFileReadExceptionCount = -1;
    private int testingAlternatelyFailOnFileReadExceptionPassCount = 3;

    public void setTestingAlternatelyFailOnFileReadException(boolean tORf) {
        testingAlternatelyFailOnFileReadException = tORf;
        if (tORf) {
            testingAlternatelyFailOnFileReadExceptionCount = -1;
        }
    }

    public void setTestingAlternatelyFailOnFileReadExceptionPassCount(int passCount) {
        testingAlternatelyFailOnFileReadExceptionPassCount = passCount;
    }

    private boolean testingAlternatelyFailOnFileReadNull = false;
    private int testingAlternatelyFailOnFileReadNullCount = -1;
    private int testingAlternatelyFailOnFileReadNullPassCount = 3;

    public void setTestingAlternatelyFailOnFileReadNull(boolean tORf) {
        testingAlternatelyFailOnFileReadNull = tORf;
        if (tORf) {
            testingAlternatelyFailOnFileReadNullCount = -1;
        }
    }

    public void setTestingAlternatelyFailOnFileReadNullPassCount(int passCount) {
        testingAlternatelyFailOnFileReadNullPassCount = passCount;
    }

    private boolean testingAlternatelyFailOnPut = false;
    private int testingAlternatelyFailOnPutCount = -1;

    public void setTestingAlternatelyFailOnPut(boolean tORf) {
        testingAlternatelyFailOnPut = tORf;
        if (tORf) {
            testingAlternatelyFailOnPutCount = -1;
        }
    }

    private boolean isAlternatelyFail(boolean tORf, int count, int mod) {
        return tORf && (count % mod == 1);
    }

    private boolean isAlternatelyPass(boolean tORf, int count, int mod) {
        return tORf && !isAlternatelyFail(tORf, count, mod);
    }

}
