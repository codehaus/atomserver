package org.atomserver.core.filestore;

import org.atomserver.EntryDescriptor;
import org.atomserver.exceptions.AtomServerException;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.annotation.ManagedAttribute;

import java.io.File;
import java.io.IOException;

@ManagedResource(description = "Testing Content Storage")
public class TestingContentStorage extends FileBasedContentStorage {

    /**
     * construct a EntryStore object to store entry data.  <br/>
     * NOTE: This CTOR is intended for use by the IOC container
     *
     * @param rootDir the root directory for the file-based store
     *                this is the dir at which you would find the workspaces (e.g. "widgets")
     */
    public TestingContentStorage(File rootDir) {
        super(rootDir);
    }

    // Used by IOC container to set the time (in Seconds) to lag when sweeping excess revisions
    // to a separate trash dir
    @ManagedAttribute
    public void setDeleteLagTimeSecs(int DeleteLagTimeSecs) {
        super.setDeleteLagTimeSecs(DeleteLagTimeSecs);
    }
    @ManagedAttribute
    public int getDeleteLagTimeSecs() {
        return super.getDeleteLagTimeSecs();
    }

    public String getContent(EntryDescriptor descriptor)  {
        if ( testingFailOnGet ) {
            throw new RuntimeException( "THIS IS A FAKE FAILURE FROM testingFailOnGet" );
        }
        return super.getContent(descriptor);
    }

    protected String readFileToString(File file) throws IOException {
        if ( isAlternatelyPass( testingAlternatelyFailOnFileReadException,
                                ++testingAlternatelyFailOnFileReadExceptionCount,
                                testingAlternatelyFailOnFileReadExceptionPassCount ) ) {
            throw new IOException( "THIS IS A FAKE FAILURE FROM testingFailOnFileReadException" );
        }
        return super.readFileToString(file);
    }

    protected File findExistingEntryFile(EntryDescriptor entry, int revisionsBack) {
        if ( isAlternatelyPass( testingAlternatelyFailOnFileReadNull,
                                ++testingAlternatelyFailOnFileReadNullCount,
                                testingAlternatelyFailOnFileReadNullPassCount  ) ) {
            return null;
        }
        return super.findExistingEntryFile(entry, revisionsBack);
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
