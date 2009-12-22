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

package org.atomserver.core;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.atomserver.testutils.client.JettyWebAppTestCase;
import org.atomserver.testutils.conf.TestConfUtil;
import org.atomserver.utils.thread.ManagedThreadPoolTaskExecutor;

public class ExtendedContextOpsDirTest extends JettyWebAppTestCase {

    boolean runTest = false;

    protected void setUp() throws Exception {

        // when tests are run for TeamCity they use ops conf, so skip this test
        String opsConf = System.getProperty("atomserver.ops.conf.dir");
        if ( StringUtils.isEmpty(opsConf)) {

            // TODO : FIXME
            // runTest = true;
            // TestConfUtil.preSetup(null, "opsconf" );
        }
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        if ( runTest ) {
            TestConfUtil.postTearDown();
        }
    }

    public void testExtendedContext() throws Exception {

        if ( !runTest ) {
            return;
        }

        // evrify that AtomServer comes up, and functions
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod("http://localhost:" + getPort() + "/atomserver/v1");
        client.executeMethod(get);

        assertEquals( 200, get.getStatusCode() );

        // check that our overrides worked
        //  Note: we used the default asdev-postgres env
        ManagedThreadPoolTaskExecutor bean = 
                    (ManagedThreadPoolTaskExecutor)(getSpringFactory().getBean("org.atomserver-taskExecutor"));
        assertEquals( 7, bean.getCorePoolSize() );
        assertEquals( 77, bean.getKeepAliveSeconds() );
        assertEquals( 777, bean.getMaxPoolSize() );
    }
}
