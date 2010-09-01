/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
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

package org.atomserver.server.servlet;

/**
 * Wrapper class for thread specific information on authenticated user.
 */
public class AtomServerUserInfo {
    // Authenticated user name 
    private static ThreadLocal<String> userInfo = new ThreadLocal<String>();

    public static void setUser(String user){
         userInfo.set(user);
    }

    public static String getUser() {
        String user = userInfo.get();
        return  (user == null) ? "undef" : user ;
    }
}
