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
        return Response.status(created ? CREATED : OK)
                .entity(entry).tag(new EntityTag(entry.getSimpleExtension(ETAG)))
                .build();
    }

    public static Response feedResponse(Feed feed) {
        return Response.status(OK).entity(feed)
                .tag(new EntityTag(feed.getSimpleExtension(ETAG), true))
                .build();
    }
}
