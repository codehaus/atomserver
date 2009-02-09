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
import org.atomserver.testutils.conf.TestConfUtil;


public class DBContentFeedDBSTest extends FeedDBSTest {
    public static Test suite()
    { return new TestSuite( DBContentFeedDBSTest.class ); }

    public void setUp() throws Exception {
        TestConfUtil.preSetup("dbcontent");
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        TestConfUtil.postTearDown();
    }
}
