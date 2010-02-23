/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.cache;

import org.atomserver.core.dbstore.DBSTestCase;
import org.atomserver.testutils.conf.TestConfUtil;
import org.atomserver.uri.URIHandler;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Test case to check cache add/delete updates the revision number.
 */
public class CacheConfigChangeTest extends DBSTestCase {


    AggregateFeedCacheManager cacheManager = null;
    List<String> cachedFeedIds = null;
    protected URIHandler entryURIHelper = null;

    public void setUp() throws Exception {
        TestConfUtil.preSetup("aggregates3");
        super.setUp();

        ApplicationContext appCtx = super.getSpringFactory();
        cacheManager = (AggregateFeedCacheManager) appCtx.getBean("org.atomserver-aggregatefeedcachemanager");
        cacheManager.removeExistingCaches();
    }

    public void testCacheChange() {
        cachedFeedIds = new ArrayList<String>();

        List<String> list = new ArrayList<String>();
        list.add("$join(reds,greens), urn:hue, en_US");
        List<String> ids = cacheManager.cacheAggregateFeed(list);
        cachedFeedIds.addAll(ids);  // for cleanup

        long configRevision1 = cacheManager.getCacheConfigRevision();
        assertFalse(cacheManager.isWorkspaceInCachedFeeds("blues"));

        List<String> workspaceList = new ArrayList<String>();
        workspaceList.add("greens");
        workspaceList.add("reds");
        Locale locale = new Locale("en","US");
        assertTrue(cacheManager.isFeedCached(workspaceList, locale, "urn:hue") != null);
        long configRevision2 = cacheManager.getCacheConfigRevision();
        assertEquals(configRevision1, configRevision2);

        list.clear();
        list.add("$join(reds,greens,blues), urn:hue, en_US");
        ids = cacheManager.cacheAggregateFeed(list);
        cachedFeedIds.addAll(ids);  // for cleanup

        long configRevision3 = cacheManager.getCacheConfigRevision();
        assertTrue(configRevision1 != configRevision3 );
        assertTrue(cacheManager.isWorkspaceInCachedFeeds("blues"));
        assertTrue(cacheManager.isFeedCached(workspaceList, locale, "urn:hue") != null);
        
        long configRevision4 = cacheManager.getCacheConfigRevision();
        assertEquals(configRevision3,configRevision4);

        // remove last feed
        cacheManager.removeCachedAggregateFeedsByFeedIds(ids);
        long configRevision5 = cacheManager.getCacheConfigRevision();
        assertTrue(configRevision4 != configRevision5);
    }

    public void tearDown() throws Exception {
        cacheManager.removeCachedAggregateFeedsByFeedIds(cachedFeedIds);
        super.tearDown();
        TestConfUtil.postTearDown();
    }
    
}


