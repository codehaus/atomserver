package org.atomserver;

import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.TargetType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * AtomServerWrapper - an Abdera provider that delegates methods to an AtomServer instance.
 * <p/>
 * This object wraps an AtomServer, and delegates all Abdera Provider methods to it - subclasses
 * can extend this class and override just the required methods.
 * <p/>
 * We explicitly extend AtomServer so we can duplicate the functionality of the request() method
 * without having to copy a bunch of code.
 */
public abstract class AtomServerWrapper extends AtomServer {
    static protected Log log = LogFactory.getLog(AtomServerWrapper.class);

    private AtomServer atomServer;

    public void setAtomServer(AtomServer atomServer) {
        this.atomServer = atomServer;
    }

    public AtomServer getAtomServer() {
        return atomServer;
    }

    public ResponseContext createEntry(RequestContext requestContext) {
        return atomServer.createEntry(requestContext);
    }

    public ResponseContext deleteEntry(RequestContext requestContext) {
        return atomServer.deleteEntry(requestContext);
    }

    public ResponseContext deleteMedia(RequestContext requestContext) {
        return atomServer.deleteMedia(requestContext);
    }

    public ResponseContext updateEntry(RequestContext requestContext) {
        return atomServer.updateEntry(requestContext);
    }

    public ResponseContext updateMedia(RequestContext requestContext) {
        return atomServer.updateMedia(requestContext);
    }

    public ResponseContext getService(RequestContext requestContext) {
        return atomServer.getService(requestContext);
    }

    public ResponseContext getFeed(RequestContext requestContext) {
        return atomServer.getFeed(requestContext);
    }

    public ResponseContext getEntry(RequestContext requestContext) {
        return atomServer.getEntry(requestContext);
    }

    public ResponseContext getMedia(RequestContext requestContext) {
        return atomServer.getMedia(requestContext);
    }

    public ResponseContext getCategories(RequestContext requestContext) {
        return atomServer.getCategories(requestContext);
    }

    public ResponseContext entryPost(RequestContext requestContext) {
        return atomServer.entryPost(requestContext);
    }

    public ResponseContext mediaPost(RequestContext requestContext) {
        return atomServer.mediaPost(requestContext);
    }

    public String[] getAllowedMethods(TargetType targetType) {
        return atomServer.getAllowedMethods(targetType);
    }
}