/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.cache;

import org.atomserver.core.dbstore.DBSTestCase;
import org.atomserver.core.BaseServiceDescriptor;
import org.atomserver.uri.URIHandler;
import org.atomserver.testutils.conf.TestConfUtil;
import org.atomserver.AtomService;
import org.springframework.context.ApplicationContext;
import org.apache.abdera.model.Feed;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Performnace test based on generated data. This test assumes that the database and entry contents are already
 * set up.
 * Some flags to note:
 * useExistingData  if true, it will use the existing data in the postgres sql and the generated contents.
 *                  if false, it will generate new data, which usually takes about 3 hrs.
 * INSERTTestRunIteration   bump up this number after each run so that the inserts will not conflicts with
 *                          existing entries
 *
 */

public class CachedAggregateFeedPerf extends DBSTestCase {

    boolean useExistingData = true;
    final int STARTID =  1000;
    final int ENDID =   20000;
    final int MAXINCRE = 10;

    int INSERTTestRunIteration = 1;

    AggregateFeedCacheManager cacheManager = null;
    List<String> cachedFeedIds = null;
    protected URIHandler entryURIHelper = null;

    public void setUp() throws Exception {
        TestConfUtil.preSetup("aggregates3");
        super.setUp();

        ApplicationContext appCtx = super.getSpringFactory();
        cacheManager = (AggregateFeedCacheManager) appCtx.getBean("org.atomserver-aggregatefeedcachemanager");
        entryURIHelper = ((AtomService) appCtx.getBean("org.atomserver-atomService")).getURIHandler();

        removeCaching();

        if(!useExistingData) {
            setupData();
        }
        System.out.println("Setup Done");
    }

    public void tearDown() throws Exception {
//        cacheManager.removeCachedAggregateFeedsByFeedIds(cachedFeedIds);
        super.tearDown();
        TestConfUtil.postTearDown();
    }

    public void testPerf() throws Exception {
//        cachedGET();
        cached_INSERT_UPDATE_DEL();
    }

    private void cachedGET() {
       removeCaching();
       String [] urls = new String [] {
               "$join/urn:hue?locale=en_US",
               "$join/urn:hue",
               "$join(reds,greens)/urn:hue?locale=en_US",
               "$join(reds,greens)/urn:hue",
               "$join(reds,greens)/urn:hue/-/(urn:tint)even"
       };

       long [] e1 = new long[urls.length];

       int loopCount = 10;
       System.out.println("Running without cache");

       for(int i = 0; i < urls.length; i++) {
           e1[i] = runGET(urls[i], loopCount);
       }


       // turn on cache
       enableCaching();

       System.out.println("Running with cache") ;
       long [] e2 = new long[urls.length];
       for(int i = 0; i < urls.length; i++) {
           e2[i] = runGET(urls[i], loopCount);
       }


       for(int i = 0; i < urls.length; i++) {
           int numtabs = 5 - urls[i].length()/9 ;
           String tabs = "\t";
           for(int k=0; k < numtabs; k++) tabs += "\t";
           System.out.println( urls[i] + tabs + e1[i]/loopCount + "\t" + e2[i]/loopCount);
       }
    }

    public void cached_INSERT_UPDATE_DEL() throws Exception  {
        removeCaching();
        int incre = MAXINCRE;

        int startId = ENDID + INSERTTestRunIteration*MAXINCRE*2;
        System.out.println("Start Id=" + startId);
        int cachedStartId = startId + incre;

        String [] doc = new String[incre];
        for(int i = 0; i < incre; i++) doc[i] = redXml(startId + i);
        String locale = Locale.US.toString();
        long start = System.currentTimeMillis();
        String entryId;
        // Add
        for(int i = 0; i < incre; i++)  {
            entryId = "" + (startId + i);
            modifyEntry("reds", "shades", entryId, locale, doc[i], true, "0");
        }
        long e11 = System.currentTimeMillis() - start;

        // updates
        for(int i = 0; i < incre; i++) doc[i] = modRedXml(startId + i);

        start = System.currentTimeMillis();
        for(int i = 0; i < incre; i++)  {
            entryId = "" + (startId + i);
            modifyEntry("reds", "shades", entryId, locale, doc[i], false, "1");
        }
        long e12 = System.currentTimeMillis() - start;

        // deletes
        start = System.currentTimeMillis();
        int first = incre/2;

        for(int i = 0; i < first; i++) {
            entryId = "" + (startId + i);
//            System.out.println(" deleteEntry2 EntryId:" + entryId);
            deleteEntry2("http://localhost:60080" + widgetURIHelper.constructURIString("reds", "shades", entryId, Locale.US) + "/*");
        }
        long e13 = System.currentTimeMillis() - start;

         // obliterate
        start = System.currentTimeMillis();
        for(int i = first; i < incre; i++) {
            entryId = "" + (startId + i);
//            System.out.println("deleteEntry EntryId:" + entryId);
            deleteEntry("reds", "shades", entryId, Locale.US.toString());
        }
        long e14 = System.currentTimeMillis() - start;

        // ---- With Cache ----
        enableCaching();

        // Add
        for(int i = 0; i < incre; i++) doc[i] = redXml(cachedStartId + i);
        start = System.currentTimeMillis();
        for( int i = 0; i < incre; i++) {
            entryId = "" + (cachedStartId + i);
            modifyEntry("reds", "shades", entryId, locale, doc[i], true, "0");
        }
        long e21 = System.currentTimeMillis() - start;
        printSlower(" ##insert:",e11, e21);

        // Updates
        for(int i = 0; i < incre; i++) doc[i] = modRedXml(cachedStartId + i);

        for(int i = 0; i < incre; i++) doc[i] = redXml(cachedStartId + i);
        start = System.currentTimeMillis();
        for( int i = 0; i < incre; i++) {
            entryId = "" + (cachedStartId + i);
            modifyEntry("reds", "shades", entryId, locale, doc[i], false, "1");
        }
        long e22 = System.currentTimeMillis() - start;
        printSlower(" ##update:",e12 , e22);

        // Deletes
        first = incre/2;
        start = System.currentTimeMillis();
        for(int i = 0; i < first; i++) {
            entryId = "" + (cachedStartId + i);
            deleteEntry2("http://localhost:60080" + widgetURIHelper.constructURIString("reds", "shades", entryId, Locale.US) + "/*");
        }
        long e23 = System.currentTimeMillis() - start;
        printSlower(" ##delete:", e13, e23);

        // obliterate
        start = System.currentTimeMillis();
//        for(int i = first; i < incre; i++) {
//            entryId = "" + (cachedStartId + i);
//            deleteEntry("reds", "shades", entryId, Locale.US.toString());
//        }
        for(int i = incre-1; i >= first; i--) {
            entryId = "" + (cachedStartId + i);
            deleteEntry("reds", "shades", entryId, Locale.US.toString());
        }
        long e24 = System.currentTimeMillis() - start;
        printSlower(" ##obliterate:", e14, e24);
    }
    
    // ---------- Utilities ----------
    
    private void removeCaching() {
        System.out.println("Removing Caching");
     // aggregate feeds of interest
        List<String> feedList = new ArrayList<String>();
        feedList.add("$join(reds,greens,blues), urn:hue, en_US");
        feedList.add("$join(reds,greens), urn:hue, en_US");
        feedList.add("$join(reds,greens), urn:hue");
        feedList.add("$join,urn:hue,en_US");
        feedList.add("$join,urn:hue");

        // clear cache from previous state
        cacheManager.removeCachedAggregateFeeds(feedList);    
    }

    private void enableCaching() {
        System.out.println("Enable Caching");
        List<String> feedList = new ArrayList<String>();
        feedList.add("$join,urn:hue,en_US");
        feedList.add("$join,urn:hue");
        feedList.add("$join(reds,greens), urn:hue, en_US");
        feedList.add("$join(reds,greens), urn:hue");
        long start = System.currentTimeMillis();
        cachedFeedIds = cacheManager.cacheAggregateFeed(feedList);
        System.out.println(" enableCaching:" + (System.currentTimeMillis() - start)/(1000) + " secs.");
    }

    private void setupData() throws Exception {
        
        int startId = STARTID;
        int endId = ENDID;

        System.out.println("Setting up Data:  start:" + startId + " end:" + endId);
        entryCategoriesDAO.deleteAllEntryCategories("reds");
        entryCategoriesDAO.deleteAllEntryCategories("greens");
        entryCategoriesDAO.deleteAllEntryCategories("blues");

        entriesDao.deleteAllEntries(new BaseServiceDescriptor("reds"));
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("greens"));
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("blues"));

        // workspace purples is not used in this test but needs to be cleaned up.
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("purples"));

        long start = System.currentTimeMillis();
        for (int i = startId; i < endId; i++) {
            String entryId = "" + i;
            modifyEntry("reds", "shades", entryId, Locale.US.toString(), redXml(i), true, "0");
            if (i % 2 == 0) {
                modifyEntry("greens", "shades", entryId, null, greenXml(i), true, "0");
            }
            if (i % 3 == 0) {
                modifyEntry("blues", "shades", entryId, null, blueXml(i), true, "0");
            }
        }
        System.out.println("It takes " + (System.currentTimeMillis() - start)/(1000*60) + " mins to generate test data.");
    }

    private long runGET(String url, int loopCount) {
       Feed feed = null;
       long start = System.currentTimeMillis();
       for(int i = 0; i < loopCount; i++) {
            feed = getPage(url);
       }
       long e = System.currentTimeMillis() - start;
       return e;
    }

    private void printFaster( String msg, long e1, long e2) {
        System.out.println( msg + "\t" + e1 + "\t" + e2 +  " improvement:" + e1/(e2 * 1.0));
    }

    private void printSlower( String msg, long e1, long e2) {
        System.out.println( msg + "\t" + e1 + "\t" + e2 +  " slower:" + e2/(e1 * 1.0));
    }

    private static String redXml(int id) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<red xmlns='http://schemas.atomserver.org/aggregate-tests'>");
        stringBuilder.append("<id>").append(id).append("</id>");
        stringBuilder.append("<group>").append(id % 2 == 0 ? "even" : "odd").append("</group>");
        stringBuilder.append("</red>");
        return stringBuilder.toString();
    }

    private static String modRedXml(int id) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<red xmlns='http://schemas.atomserver.org/aggregate-tests'>");
        stringBuilder.append("<id>").append(id).append("</id>");
        stringBuilder.append("<group>").append(id % 2 == 0 ? "black" : "white").append("</group>");
        stringBuilder.append("</red>");
        return stringBuilder.toString();
    }

    private static String greenXml(int id) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<green xmlns='http://schemas.atomserver.org/aggregate-tests'>");
        stringBuilder.append("<red>").append(id).append("</red>");
        stringBuilder.append("<red>").append(id + 1).append("</red>");
        stringBuilder.append("<group>").append(id % 3 == 0 ? "reds" : "blues").append("</group>");
        stringBuilder.append("</green>");
        return stringBuilder.toString();
    }

    private static String blueXml(int id) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<blue xmlns='http://schemas.atomserver.org/aggregate-tests'>");
        stringBuilder.append("<red>").append(id).append("</red>");
        stringBuilder.append("<red>").append(id + 1).append("</red>");
        stringBuilder.append("<red>").append(id + 2).append("</red>");
        stringBuilder.append("<group>").append(id % 5 == 0 ? "heavy" : "light").append("</group>");
        stringBuilder.append("</blue>");
        return stringBuilder.toString();
    }
}
