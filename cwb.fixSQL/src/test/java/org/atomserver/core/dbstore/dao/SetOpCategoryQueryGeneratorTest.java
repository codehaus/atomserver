/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao;

import junit.framework.TestCase;
import org.apache.abdera.Abdera;
import org.apache.abdera.protocol.server.ServiceContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.AtomCategory;
import org.atomserver.core.AbstractAtomService;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.uri.FeedTarget;
import org.atomserver.uri.URIHandler;
import org.atomserver.utils.conf.ConfigurationAwareClassLoader;
import org.atomserver.utils.logic.BooleanExpression;
import org.atomserver.utils.logic.BooleanTerm;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class SetOpCategoryQueryGeneratorTest extends TestCase {

    private static final Log log = LogFactory.getLog(SetOpCategoryQueryGeneratorTest.class);

    protected URIHandler handler = null;
    protected ServiceContext serviceContext = null;
    static private final String CONTEXT_NAME = "org.apache.abdera.protocol.server.ServiceContext";

    protected String baseURI;

    protected void setUp() throws Exception {
        String[] configs = {"/org/atomserver/spring/propertyConfigurerBeans.xml",
                            "/org/atomserver/spring/databaseBeans.xml",
                            "/org/atomserver/spring/storageBeans.xml",
                            "/org/atomserver/spring/logBeans.xml",
                            "/org/atomserver/spring/abderaBeans.xml"};

        ClassPathXmlApplicationContext springFactory = new ClassPathXmlApplicationContext(configs, false);
        springFactory.setClassLoader( new ConfigurationAwareClassLoader(springFactory.getClassLoader()));
        springFactory.refresh();

        handler = ((AbstractAtomService) springFactory.getBean("org.atomserver-atomService")).getURIHandler();
        serviceContext = (ServiceContext) springFactory.getBean(CONTEXT_NAME);
        if (serviceContext.getAbdera() == null) {
            serviceContext.init(new Abdera(), null );
        }
        baseURI = handler.getServiceBaseUri();
    }

    public void testFourTerms() throws Exception {
        String iri ="http://foobar:7890/" + baseURI +
                    "/widgets/acme/-/AND/OR/(urn:inquiry.state)PROXY_REQUIRED/(urn:inquiry.state)BUILT/OR/(urn:inquiry.stripes)0/(urn:inquiry.stripes)1?max-results=50&entry-type=full&start-index=114733223";
        Set<BooleanExpression<AtomCategory>>  catQuery = checkCatQuery(iri);
        String sql = SetOpCategoryQueryGenerator.generateCategorySearch( catQuery );
    }

    public void XXXtestThreeAnds() throws Exception {
        String iri ="http://foobar:7890/" + baseURI +
             "/widgets/acme/-/(urn:inquiry.state)PROXY_REQUIRED/(urn:inquiry.state)BUILT/(urn:inquiry.state)SOME_OTHER?max-results=50&entry-type=full&start-index=114733223";
        Set<BooleanExpression<AtomCategory>>  catQuery = checkCatQuery(iri);
    }

    public void testThreeTerms() throws Exception {
        String iri ="http://foobar:7890/" + baseURI +
             "/widgets/acme/-/AND/(urn:inquiry.state)TRAVELER_SENT/OR/(urn:inquiry.stripes)2/(urn:inquiry.stripes)3?max-results=50&entry-type=full&start-index=121673351";
        Set<BooleanExpression<AtomCategory>>  catQuery = checkCatQuery(iri);
        String sql = SetOpCategoryQueryGenerator.generateCategorySearch( catQuery );
    }

    public void testTwoTerms() throws Exception {
        String iri ="http://foobar:7890/" + baseURI +
        "$join(listings,calendars,reviewmetadata,propmedia)/urn:ha.listings.trips/-/OR/(urn:ha.companies)HR/(urn:ha.companies)VV?max-results=100&entry-type=full&locale=de_DE&start-index=209384962";
        Set<BooleanExpression<AtomCategory>>  catQuery = checkCatQuery(iri);
        String sql = SetOpCategoryQueryGenerator.generateCategorySearch( catQuery );
    }

    public void testOneTerm() throws Exception {
        String iri ="http://foobar:7890/" + baseURI +
        "/widgets/acme/-/(urn:ha.companies)vv?start-index=209372224&max-results=25&entry-type=link";
        Set<BooleanExpression<AtomCategory>>  catQuery = checkCatQuery(iri);
        String sql = SetOpCategoryQueryGenerator.generateCategorySearch( catQuery );
    }

    private Set<BooleanExpression<AtomCategory>> checkCatQuery(String iri) {
        log.debug("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        log.debug("Looking at " + iri );
        FeedTarget ft = handler.getFeedTarget(new MockRequestContext(serviceContext, "GET", iri));
        assertNotNull(ft);

        Set<BooleanExpression<AtomCategory>> catQuery = ft.getCategoriesQuery();
        assertNotNull(catQuery);

        // TODO -assert
        Set<BooleanTerm<? extends AtomCategory>> terms = new HashSet<BooleanTerm<? extends AtomCategory>>();
        for (BooleanExpression<AtomCategory> expr : catQuery) {
            log.debug("*** type= " + expr.getType() );
            log.debug("***" + expr );
            expr.buildTermSet(terms);
        }
        log.debug("---------------------");
        for (BooleanTerm<? extends AtomCategory> term : terms) {
            log.debug("TERM= (scheme= " + term.getValue().getScheme() + " ,term= " + term.getValue().getTerm() + ")");
        }

        return catQuery;
    }
}