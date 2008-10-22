/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.protocol.server.Target;
import org.apache.abdera.protocol.server.TargetType;
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

    private static final String localeString = "en";
    private static final Locale locale = new Locale(localeString);

    public void testCollection() throws Exception {
        Target target = checkTarget("/" + servletMapping + "/widgets/acme?updated-min=" + updatedMinDateString + "&locale=" + localeString,
                                    TargetType.TYPE_COLLECTION);
        checkUpdatedMin((URITarget) target);
        checkLocale((URITarget) target);

        target = checkTarget("/" + servletMapping + "/widgets/acme/-/cat1?updated-min=" + updatedMinDateString + "&locale=" + localeString,
                             TargetType.TYPE_COLLECTION);
        checkUpdatedMin((URITarget) target);
        checkLocale((URITarget) target);

        target = checkTarget("/" + servletMapping + "/widgets/acme/-/cat1/cat2/cat3?updated-min=" + updatedMinDateString + "&locale=" + localeString,
                             TargetType.TYPE_COLLECTION);
        checkUpdatedMin((URITarget) target);
        checkLocale((URITarget) target);

    }

    public void testEntry() throws Exception {
        Target target = checkTarget("/" + servletMapping + "/widgets/acme/123?updated-min=" + updatedMinDateString + "&locale=" + localeString,
                                    TargetType.TYPE_ENTRY);
        checkUpdatedMin((URITarget) target);
        checkLocale((URITarget) target);

        target = checkTarget("/" + servletMapping + "/widgets/acme/123/3?updated-min=" + updatedMinDateString + "&locale=" + localeString,
                             TargetType.TYPE_ENTRY);
        checkUpdatedMin((URITarget) target);
        checkLocale((URITarget) target);

        target = checkTarget("/" + servletMapping + "/widgets/acme/123.en.xml/3?updated-min=" + updatedMinDateString + "&locale=" + localeString,
                             TargetType.TYPE_ENTRY);
        checkUpdatedMin((URITarget) target);
        checkLocale((URITarget) target);

    }

    private void checkUpdatedMin(URITarget target) {
        Date updatedMinParam = target.getUpdatedMinParam();
        log.debug("%%%%%%%%%%%%%%%%%%%% updatedMin = " + updatedMinParam + " updatedMinDate = " + updatedMinDate);
        assertNotNull(updatedMinParam);
        assertEquals(updatedMinDate, updatedMinParam);
    }

    private void checkLocale(URITarget target) {
        Locale localeParam = target.getLocaleParam();
        log.debug("%%%%%%%%%%%%%%%%%%%% locale = " + locale);
        assertNotNull(localeParam);
        assertEquals(localeParam, locale);
    }
}
