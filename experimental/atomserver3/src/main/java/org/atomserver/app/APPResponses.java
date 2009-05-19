package org.atomserver.app;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import static org.atomserver.AtomServerConstants.ETAG;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

public class APPResponses {
    public static Response entryResponse(Entry entry) {
        return entryResponse(entry, false);
    }

    public static Response entryResponse(Entry entry, boolean created) {
        Response.ResponseBuilder builder =
                Response.status(created ? CREATED : OK)
                        .entity(entry);
        String etag = entry.getSimpleExtension(ETAG);
        // TODO: the etag should never be null - we should verify that and remove this null check
        if (etag != null) {
            builder.tag(new EntityTag(etag));
        }
        return builder.build();
    }

    public static Response feedResponse(Feed feed) {
        return Response.status(OK).entity(feed)
                .tag(new EntityTag(feed.getSimpleExtension(ETAG), true))
                .build();
    }
}
