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

package org.atomserver.core.dbstore.dao;

import java.util.Date;

/**
 * The base DAO API for AtomServer
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface AtomServerDAO {
    /**
     * enum to represent the allowed types - ultimately the getDatabaseType and setDatabaseType methods should deal
     * in this enum, but to make spring config as easy as possible we keep it as a String for now, and provide the
     * method here to check for validity
     */
    public enum DatabaseType {
        hsql, mysql, sqlserver, postgres;

        public static boolean isValidType(String type) {
            try {
                // return true iff there is a non-null type that matches.
                return valueOf(DatabaseType.class, type) != null;
            } catch (Exception e) {
                // if we get an exception, return false.
                return false;
            }
        }
    }

    /**
     * valid values are "hsql", "mysql" and "sqlserver"
     * Set from Spring using the ${db.type} variable
     */
    String getDatabaseType();

    void setDatabaseType(String databaseType);

    Date selectSysDate();

    /**
     * Test the availabilty of the DAOs. This method is used by the IsAliveHandler
     * to determine if the Database is up and running. Mostly this is meant to check the
     * the DB Connection in some lightweight way
     * Note that the method does not bother returning a boolean. Instead, it is assumed
     * that a RuntimeException will be thrown
     */
    void testAvailability();
}
