/* Copyright (c) 2007 HomeAway, Inc.
 *  All rights reserved.  http://www.atomserver.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.atomserver.core.dbstore;


import junit.framework.Test;
import junit.framework.TestSuite;

/**
 */
public class FeedDBSTest extends FeedDBSTestCase {

    public static Test suite() { return new TestSuite(FeedDBSTest.class); }

    public void setUp() throws Exception {
        super.setUp();
        
        // the tests below assume an even count at the start of the DB
        assertTrue((startCount % 2) == 0);
    }

    public void tearDown() throws Exception { super.tearDown(); }

    protected boolean requiresDBSeeding() { return true; }

    // --------------------
    //       tests
    //---------------------

    public void testSeveralScenarios() throws Exception {
        readEntireFeedAtOnce("widgets", "acme");

        // test using TotalResultsElement
        createRunAllTestsDelete("dummy", "floop", 76543, "it");

        // grab a page with max-results, but without start-index
        readFeedWithMaxNoStart("widgets", "acme");

        readFeedMaxPerPage("widgets", "acme");

        // grab pages without start-index and max-results
        //  loop thru all pages, with partial page at the end
        loopThruPagesUsingEndIndex("widgets", (startCount / 3 + 1), "acme");

        // grab pages without start-index and max-results
        //  loop thru all pages, without partial page at the end
        loopThruPagesUsingEndIndex("widgets", (startCount / 2), "acme");

        // grab single page with start-index and max-results
        //  where max-results > total count
        maxGreaterThanTotal("widgets", "acme");

        // grab pages without start-index and max-results
        //  loop thru all pages, with partial page at the end
        loopThruPagesUsingNextLink("widgets", (startCount / 3 + 1), "acme");

        // grab pages without start-index and max-results
        //  loop thru all pages, without partial page at the end
        loopThruPagesUsingNextLink("widgets", (startCount / 2), "acme");

        nextAndSelf("widgets", "acme");
    }
}
