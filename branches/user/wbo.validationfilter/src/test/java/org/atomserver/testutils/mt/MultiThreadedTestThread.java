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


package org.atomserver.testutils.mt;

abstract public class MultiThreadedTestThread extends Thread {
    boolean finished = false;
    boolean hasFailed = false;
    
    public void setFinished() {
        this.finished = true;
    }
    
    public boolean hasFailed() {
        return hasFailed;
    }

    abstract public void runTest() throws Exception;
    
    public void run() {
        while ( !finished ) {
            try {
                runTest();
            }
            catch ( Throwable ee ) {
                ee.printStackTrace();
                hasFailed = true;
                finished = true;
            }
        }		
    }
}
