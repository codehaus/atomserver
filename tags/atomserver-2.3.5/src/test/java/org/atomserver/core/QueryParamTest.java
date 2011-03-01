/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.protocol.server.Target;
import org.apache.abdera.protocol.server.TargetType;
import org.atomserver.EntryType;
import org.atomserver.exceptions.BadRequestException;
import org.atomserver.uri.URITarget;
import org.atomserver.utils.AtomDate;

import java.util.Date;
import java.util.Locale;


public class QueryParamTest extends URITargetTestCase {

    public static Test suite() { return new TestSuite(QueryParamTest.class); }

    public void setUp() throws Exception { super.setUp(); }

    protected void tearDown() throws Exception { super.tearDown(); }

    //----------------------------
    //          Tests
    //----------------------------
    private static final String updatedMinDateString = "2007-10-09T22:42:26.000Z";
    private static final Date updatedMinDate = AtomDate.parse(updatedMinDateString);

    private static final String updatedMaxDateString = "2007-10-09T23:43:36.000Z";
    private static final Date updatedMaxDate = AtomDate.parse(updatedMaxDateString);

    private static final String localeString = "en";
    private static final Locale locale = new Locale(localeString);

    private static final int startIndex = 1234;
    private static final int endIndex = 4567;

    private static final EntryType entryType = EntryType.full;

    private static final int maxResults = 1111;

    public void testCollectionUpdateMinMax() throws Exception {
        Target target = checkTarget("/" + servletMapping + "/widgets/acme?updated-min=" + updatedMinDateString + "&locale=" + localeString,
                                    TargetType.TYPE_COLLECTION);
        checkUpdatedMin((URITarget) target);
        checkLocale((URITarget) target);

        target = checkTarget("/" + servletMapping + "/widgets/acme/-/cat1?updated-min=" + updatedMinDateString + "&locale=" + localeString,
                             TargetType.TYPE_COLLECTION);
        checkUpdatedMin((URITarget) target);
        checkLocale((URITarget) target);

        target = checkTarget("/" + servletMapping + "/widgets/acme/-/cat1?updated-max=" + updatedMaxDateString + "&locale=" + localeString,
                             TargetType.TYPE_COLLECTION);
        checkUpdatedMax((URITarget) target);
        checkLocale((URITarget) target);

        target = checkTarget("/" + servletMapping + "/widgets/acme/-/cat1?updated-min=" + updatedMinDateString +
                             "&updated-max=" + updatedMaxDateString + "&locale=" + localeString,
                             TargetType.TYPE_COLLECTION);
        checkUpdatedMin((URITarget) target);
        checkUpdatedMax((URITarget) target);
        checkLocale((URITarget) target);

        target = checkTarget("/" + servletMapping + "/widgets/acme/-/cat1/cat2/cat3?updated-min=" + updatedMinDateString + "&locale=" + localeString,
                             TargetType.TYPE_COLLECTION);
        checkUpdatedMin((URITarget) target);
        checkLocale((URITarget) target);
        
        try {
            target = checkTarget("/" + servletMapping + "/widgets/acme?updated-min=2007-10&locale=" + localeString,
                                 TargetType.TYPE_COLLECTION);
            checkUpdatedMin((URITarget) target);
            fail( "should not get here");
        } catch (BadRequestException ee) {}
    }

    public void testCollectionEntryType() throws Exception {
        Target target = checkTarget("/" + servletMapping + "/widgets/acme?entry-type=" + entryType + "&locale=" + localeString,
                                    TargetType.TYPE_COLLECTION);
        checkEntryType((URITarget) target);
        checkLocale((URITarget) target);

        try {
            target = checkTarget("/" + servletMapping + "/widgets/acme?entry-type=WRONG&locale=" + localeString,
                                 TargetType.TYPE_COLLECTION);
            checkEntryType((URITarget) target);
            fail( "should not get here");
        } catch (BadRequestException ee) {}
    }

    public void testCollectionMaxResults() throws Exception {
        Target target = checkTarget("/" + servletMapping + "/widgets/acme?max-results=" + maxResults + "&locale=" + localeString,
                                    TargetType.TYPE_COLLECTION);
        checkMaxResults((URITarget) target);
        checkLocale((URITarget) target);

        try {
            target = checkTarget("/" + servletMapping + "/widgets/acme?max-results=FOO&locale=" + localeString,
                                 TargetType.TYPE_COLLECTION);
            checkMaxResults((URITarget) target);
            fail( "should not get here");
        } catch (BadRequestException ee) {}
    }

    public void testEntryUpdatedMaxMin() throws Exception {
        Target target = checkTarget("/" + servletMapping + "/widgets/acme/123?updated-min=" + updatedMinDateString + "&locale=" + localeString,
                                    TargetType.TYPE_ENTRY);
        checkUpdatedMin((URITarget) target);
        checkLocale((URITarget) target);

        target = checkTarget("/" + servletMapping + "/widgets/acme/123/3?updated-min=" + updatedMinDateString + "&locale=" + localeString,
                             TargetType.TYPE_ENTRY);
        checkUpdatedMin((URITarget) target);
        checkLocale((URITarget) target);

        target = checkTarget("/" + servletMapping + "/widgets/acme/123/3?updated-max=" + updatedMaxDateString + "&locale=" + localeString,
                             TargetType.TYPE_ENTRY);
        checkUpdatedMax((URITarget) target);
        checkLocale((URITarget) target);

        target = checkTarget("/" + servletMapping + "/widgets/acme/123/3?updated-min=" + updatedMinDateString +
                             "&updated-max=" + updatedMaxDateString  + "&locale=" + localeString,
                             TargetType.TYPE_ENTRY);
        checkUpdatedMin((URITarget) target);
        checkUpdatedMax((URITarget) target);
        checkLocale((URITarget) target);

        target = checkTarget("/" + servletMapping + "/widgets/acme/123.en.xml/3?updated-min=" + updatedMinDateString + "&locale=" + localeString,
                             TargetType.TYPE_ENTRY);
        checkUpdatedMin((URITarget) target);
        checkLocale((URITarget) target);
    }

    public void testCollectionStartEndIndex() throws Exception {
        Target target = checkTarget("/" + servletMapping + "/widgets/acme?start-index=" + startIndex + "&locale=" + localeString,
                                    TargetType.TYPE_COLLECTION);
        checkStartIndex((URITarget) target);
        checkLocale((URITarget) target);

        target = checkTarget("/" + servletMapping + "/widgets/acme/-/cat1?start-index=" + startIndex + "&locale=" + localeString,
                             TargetType.TYPE_COLLECTION);
        checkStartIndex((URITarget) target);
        checkLocale((URITarget) target);

        target = checkTarget("/" + servletMapping + "/widgets/acme/-/cat1?end-index=" + endIndex + "&locale=" + localeString,
                             TargetType.TYPE_COLLECTION);
        checkEndIndex((URITarget) target);
        checkLocale((URITarget) target);

        target = checkTarget("/" + servletMapping + "/widgets/acme/-/cat1?start-index=" + startIndex +
                             "&end-index=" + endIndex + "&locale=" + localeString,
                             TargetType.TYPE_COLLECTION);
        checkStartIndex((URITarget) target);
        checkEndIndex((URITarget) target);
        checkLocale((URITarget) target);

        target = checkTarget("/" + servletMapping + "/widgets/acme/-/cat1/cat2/cat3?start-index=" + startIndex + "&locale=" + localeString,
                             TargetType.TYPE_COLLECTION);
        checkStartIndex((URITarget) target);
        checkLocale((URITarget) target);
    }

    public void testEntryStartEndIndex() throws Exception {
        Target target = checkTarget("/" + servletMapping + "/widgets/acme/123?start-index=" + startIndex + "&locale=" + localeString,
                                    TargetType.TYPE_ENTRY);
        checkStartIndex((URITarget) target);
        checkLocale((URITarget) target);

        target = checkTarget("/" + servletMapping + "/widgets/acme/123/3?start-index=" + startIndex + "&locale=" + localeString,
                             TargetType.TYPE_ENTRY);
        checkStartIndex((URITarget) target);
        checkLocale((URITarget) target);

        target = checkTarget("/" + servletMapping + "/widgets/acme/123/3?end-index=" + endIndex + "&locale=" + localeString,
                             TargetType.TYPE_ENTRY);
        checkEndIndex((URITarget) target);
        checkLocale((URITarget) target);

        target = checkTarget("/" + servletMapping + "/widgets/acme/123/3?start-index=" + startIndex +
                             "&end-index=" + endIndex + "&locale=" + localeString,
                             TargetType.TYPE_ENTRY);
        checkStartIndex((URITarget) target);
        checkEndIndex((URITarget) target);
        checkLocale((URITarget) target);

        target = checkTarget("/" + servletMapping + "/widgets/acme/123.en.xml/3?start-index=" + startIndex + "&locale=" + localeString,
                             TargetType.TYPE_ENTRY);
        checkStartIndex((URITarget) target);
        checkLocale((URITarget) target);
        checkNoLatency((URITarget) target, false);

        target = checkTarget("/" + servletMapping + "/widgets/acme/123.en.xml/3?start-index=" + startIndex +
                             "&locale=" + localeString  + "&no-latency=" + true,
                             TargetType.TYPE_ENTRY);
        checkStartIndex((URITarget) target);
        checkLocale((URITarget) target);
        checkNoLatency((URITarget) target, true);
        
        target = checkTarget("/" + servletMapping + "/widgets/acme/123.en.xml/3?start-index=" + startIndex +
                             "&locale=" + localeString  + "&obliterate=" + false,
                             TargetType.TYPE_ENTRY);
        checkStartIndex((URITarget) target);
        checkLocale((URITarget) target);
        checkObliterate((URITarget) target, false);
        
        target = checkTarget("/" + servletMapping + "/widgets/acme/123.en.xml/3?start-index=" + startIndex +
                             "&locale=" + localeString  + "&obliterate=" + true,
                             TargetType.TYPE_ENTRY);
        checkStartIndex((URITarget) target);
        checkLocale((URITarget) target);
        checkObliterate((URITarget) target, true);
    }

    private void checkUpdatedMin(URITarget target) {
        Date updatedMinParam = target.getUpdatedMinParam();
        log.debug("updatedMinParam = " + updatedMinParam + " updatedMinDate = " + updatedMinDate);
        assertNotNull(updatedMinParam);
        assertEquals(updatedMinDate, updatedMinParam);
    }

    private void checkUpdatedMax(URITarget target) {
        Date updatedMaxParam = target.getUpdatedMaxParam();
        log.debug("updatedMaxParam = " + updatedMaxParam + " updatedMaxDate = " + updatedMaxDate);
        assertNotNull(updatedMaxParam);
        assertEquals(updatedMaxDate, updatedMaxParam);
    }

    private void checkStartIndex(URITarget target) {
        int startIndexParam = target.getStartIndexParam();
        log.debug("startIndexParam = " + startIndexParam );
        assertEquals(startIndex, startIndexParam);
    }

    private void checkMaxResults(URITarget target) {
        int maxResultsParam = target.getMaxResultsParam();
        log.debug("maxResultsParam = " + maxResultsParam );
        assertEquals(maxResults, maxResultsParam);
    }

    private void checkEndIndex(URITarget target) {
        int endIndexParam = target.getEndIndexParam();
        log.debug("endIndexParam = " + endIndexParam );
        assertEquals(endIndex, endIndexParam);
    }

    private void checkLocale(URITarget target) {
        Locale localeParam = target.getLocaleParam();
        log.debug("localeParam = " + localeParam);
        assertNotNull(localeParam);
        assertEquals(locale, localeParam);
    }
    
    private void checkEntryType(URITarget target) {
        EntryType entryTypeParam = target.getEntryTypeParam();
        log.debug("entryTypeParam = " + entryTypeParam);
        assertNotNull(entryTypeParam);
        assertEquals(entryType, entryTypeParam);
    }

    private void checkNoLatency(URITarget target, Boolean expected) {
        Boolean noLatencyParam = target.getNoLatency();
        log.debug("noLatencyParam = " + noLatencyParam);
        assertNotNull(noLatencyParam);
        assertEquals(expected, noLatencyParam);
    }
    private void checkObliterate(URITarget target, Boolean expected) {
        Boolean obliterateParam = target.getObliterate();
        log.debug("obliterateParam = " + obliterateParam);
        assertNotNull(obliterateParam);
        assertEquals(expected, obliterateParam);
    }
}
