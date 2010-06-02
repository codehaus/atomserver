package org.atomserver.test;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import junit.framework.Assert;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.log4j.Logger;
import org.atomserver.AtomServerConstants;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Map;

public class FeedFollower {
    private static final Logger log = Logger.getLogger(FeedFollower.class);

    private WebResource root;
    private String path;
    private int pageSize;
    private int timestamp;

    public FeedFollower(WebResource root,
                        String path,
                        int pageSize,
                        int timestamp) {
        log.debug(String.format("creating new follower for %s, starting at %d",
                path, timestamp));
        this.root = root;
        this.path = path;
        this.pageSize = pageSize;
        this.timestamp = timestamp;
    }

    public Feed nextPage(EntryChecker entryChecker, PageChecker pageChecker) throws Exception {
        log.debug(String.format("next page of %s : pageSize = %d, timestamp = %d",
                path, pageSize, timestamp));
        MultivaluedMap params = new MultivaluedMapImpl();
        params.add("max-results", String.valueOf(pageSize));
        params.add("start-index", String.valueOf(timestamp));
        params.add("entry-type", "full");
        Feed page =
                root.path(path).queryParams(params)
                        .accept(MediaType.APPLICATION_XML).get(Feed.class);

        if (pageChecker != null) {
            pageChecker.check(page);
        }

        if (entryChecker != null) {
            for (Entry entry : page.getEntries()) {
                entryChecker.check(entry);
            }
        }

        timestamp = Integer.valueOf(page.getSimpleExtension(AtomServerConstants.END_INDEX));
        Link nextLink = page.getLink("next");
        if (nextLink != null) {
            Assert.assertEquals(nextLink.getHref().getPath(), String.format("/app/%s", path));// TODO: /app/
            String[] parts = nextLink.getHref().getQuery().split("&");
            Map<String, String> queryParams = new HashMap<String, String>();
            for (String queryParam : parts) {
                String[] nameValue = queryParam.split("=", 2);
                queryParams.put(nameValue[0], nameValue[1]);
            }
            Assert.assertEquals(
                    page.getSimpleExtension(AtomServerConstants.END_INDEX),
                    queryParams.get("start-index"));
            Assert.assertEquals(
                    "full",
                    queryParams.get("entry-type"));
            Assert.assertEquals(
                    String.valueOf(pageSize),
                    queryParams.get("max-results"));
        }
        
        return page;
    }

    public int follow() throws Exception {
        return follow(null, null);
    }

    public int follow(EntryChecker entryChecker, PageChecker pageChecker) throws Exception {
        log.debug(String.format("following %s starting at %d", path, timestamp));
        int count = 0;
        Feed page;
        while (!(page = nextPage(entryChecker, pageChecker)).getEntries().isEmpty()) {
            count += page.getEntries().size();
        }
        return count;
    }

    public void reset(int timestamp) {
        log.debug(String.format("resetting %s follower to %d", path, timestamp));
        this.timestamp = timestamp;
    }

    public void reset() {
        reset(0);
    }

    public int getTimestamp() {
        return timestamp;
    }
}
