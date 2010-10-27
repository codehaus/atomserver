package org.atomserver;

import org.apache.abdera.i18n.iri.IRISyntaxException;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.impl.BaseResponseContext;
import org.apache.abdera.util.EntityTag;
import org.atomserver.core.etc.AtomServerConstants;
import org.atomserver.exceptions.BadRequestException;
import org.atomserver.uri.FeedTarget;
import org.atomserver.uri.URIHandler;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;

/**
 * AtomServerV2 - V2 of the AtomServer API.
 * <p/>
 * V2 of the AtomServer API - contains non-backwards-compatible API changes.
 */
public class AtomServerV2 extends AtomServer {

    /**
     * return the feed page indicated by the request context.
     * <p/>
     * this extends the getFeed method to return an empty feed document with the endIndex
     * extension, instead of the 304 NOT MODIFIED used in the first version of the API.
     *
     * @param request the request context
     * @return the feed page indicate by the request context
     */
    public ResponseContext getFeed(RequestContext request) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            URIHandler uriHandler = getAtomService().getURIHandler();
            FeedTarget feedTarget = uriHandler.getFeedTarget(request);
            long maxIndex = getAtomService().getMaxIndex(feedTarget.getUpdatedMaxParam(), feedTarget.getNoLatency());
            if (feedTarget.getEndIndexParam() > 0 && feedTarget.getEndIndexParam() < maxIndex) {
                maxIndex = feedTarget.getEndIndexParam();
            }

            ResponseContext responseContext = super.getFeed(request);
            if (responseContext.getStatus() == 304) {
                String collection = feedTarget.getCollection();

                Feed feed = AtomServer.getFactory(request.getAbdera()).newFeed();
                feed.addAuthor("AtomServer APP Service");
                feed.setTitle(collection + " entries");
                feed.setUpdated(new java.util.Date());
                feed.setId("tag:atomserver.org,2008:v2:" + collection);

                feed.addSimpleExtension(AtomServerConstants.END_INDEX, String.valueOf(maxIndex));

                responseContext = new BaseResponseContext<Document<Feed>>(feed.<Feed>getDocument());
                try {
                    ((BaseResponseContext) responseContext).setEntityTag(new EntityTag(feed.getId().toString()));
                } catch (IRISyntaxException e) {
                    throw new BadRequestException(e);
                }
            }
            return responseContext;
        }
        finally {
            stopWatch.stop("GET.feedV2", "") ;
        }
    }
}
