package org.atomserver;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Base;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.protocol.error.Error;
import org.apache.abdera.protocol.server.Provider;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.TargetType;
import org.apache.abdera.protocol.server.impl.AbstractResponseContext;
import org.apache.abdera.protocol.server.impl.BaseResponseContext;
import org.apache.abdera.protocol.server.impl.EmptyResponseContext;
import org.apache.abdera.util.Messages;
import org.apache.abdera.util.MimeTypeHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Date;

/**
 * AtomServerWrapper - an Abdera provider that delegates methods to an AtomServer instance.
 * <p/>
 * This object wraps an AtomServer, and delegates all Abdera Provider methods to it - subclasses
 * can extend this class and override just the required methods.
 */
public abstract class AtomServerWrapper implements Provider {
    static protected Log log = LogFactory.getLog(AtomServer.class);

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

    /**
     * Clone and owned from {@link org.apache.abdera.protocol.server.impl.AbstractProvider}.  Since
     * AbstractProvider.request() calls methods on itself we have to provide these methods here or else the
     * wrapped implementations will not work.
     * <p/>
     * NOTE - upgrading to a new Abdera may cause problems here since this is cloned
     */
    public ResponseContext request(RequestContext request) {
        TargetType type = request.getTarget().getType();
        String method = request.getMethod();
        log.debug(Messages.format("TARGET.TYPE", type));
        log.debug(Messages.format("TARGET.ID", request.getTarget().getIdentity()));
        log.debug(Messages.format("METHOD", method));
        if (method.equals("GET")) {
            if (type == TargetType.TYPE_SERVICE) {
                return getService(request);
            }
            if (type == TargetType.TYPE_COLLECTION) {
                return getFeed(request);
            }
            if (type == TargetType.TYPE_ENTRY) {
                return getEntry(request);
            }
            if (type == TargetType.TYPE_MEDIA) {
                return getMedia(request);
            }
            if (type == TargetType.TYPE_CATEGORIES) {
                return getCategories(request);
            }
        } else if (method.equals("HEAD")) {
            if (type == TargetType.TYPE_SERVICE) {
                return getService(request);
            }
            if (type == TargetType.TYPE_COLLECTION) {
                return getFeed(request);
            }
            if (type == TargetType.TYPE_ENTRY) {
                return getEntry(request);
            }
            if (type == TargetType.TYPE_MEDIA) {
                return getMedia(request);
            }
            if (type == TargetType.TYPE_CATEGORIES) {
                return getCategories(request);
            }
        } else if (method.equals("POST")) {
            if (type == TargetType.TYPE_COLLECTION) {
                return createEntry(request);
            }
            if (type == TargetType.TYPE_ENTRY) {
                return entryPost(request);
            }
            if (type == TargetType.TYPE_MEDIA) {
                return mediaPost(request);
            }
        } else if (method.equals("PUT")) {
            if (type == TargetType.TYPE_ENTRY) {
                return updateEntry(request);
            }
            if (type == TargetType.TYPE_MEDIA) {
                return updateMedia(request);
            }
        } else if (method.equals("DELETE")) {
            if (type == TargetType.TYPE_ENTRY) {
                return deleteEntry(request);
            }
            if (type == TargetType.TYPE_MEDIA) {
                return deleteMedia(request);
            }
        } else if (method.equals("OPTIONS")) {
            AbstractResponseContext rc = new EmptyResponseContext(200);
            rc.addHeader("Allow", combine(getAllowedMethods(type)));
            return rc;
        }
        return notallowed(
                request.getAbdera(),
                request,
                Messages.get("NOT.ALLOWED"),
                getAllowedMethods(
                        request.getTarget().getType()));

    }

    // more cloned methods to support request()

    /**
     * Return a 405 method not allowed error
     */
    protected ResponseContext notallowed(
            Abdera abdera,
            RequestContext request,
            String reason,
            String... methods) {
        log.debug(Messages.get("NOT.ALLOWED"));
        BaseResponseContext resp =
                (BaseResponseContext) returnBase(
                        createErrorDocument(
                                abdera, 405,
                                reason, null),
                        405, null);
        resp.setAllow(methods);
        return resp;
    }

    /**
     * Return a document
     */
    @SuppressWarnings("unchecked")
    protected ResponseContext returnBase(
            Base base,
            int status,
            Date lastModified) {
        log.debug(Messages.get("RETURNING.DOCUMENT"));
        BaseResponseContext response = new BaseResponseContext(base);
        response.setStatus(status);
        if (lastModified != null) response.setLastModified(lastModified);
        response.setContentType(MimeTypeHelper.getMimeType(base));
        Document doc = base instanceof Document ? (Document) base : ((Element) base).getDocument();
        if (doc.getEntityTag() != null) {
            response.setEntityTag(doc.getEntityTag());
        } else if (doc.getLastModified() != null) {
            response.setLastModified(doc.getLastModified());
        }
        return response;
    }

    protected Document<Error> createErrorDocument(
            Abdera abdera,
            int code,
            String message,
            Throwable e) {
        Error error = Error.create(abdera, code, message);
        return error.getDocument();
    }

    protected String combine(String... vals) {
        StringBuffer buf = new StringBuffer();
        for (String val : vals) {
            if (buf.length() > 0) buf.append(", ");
            buf.append(val);
        }
        return buf.toString();
    }
}
