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
package org.atomserver.utils.hsql;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.utils.conf.ConfigurationAwareClassLoader;

import java.io.File;
import java.sql.DriverManager;
import java.sql.Connection;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class HsqlBootstrapper
        implements Runnable {
    static private Log log = LogFactory.getLog( HsqlBootstrapper.class);

    private static final String HSQLDB_FILE_PREFIX = "jdbc:hsqldb:file:";
    private static final String HSQL_DDL = "org/atomserver/sql/hsql/hsql_ddl.sql";

    private static boolean hasBootstrapped = false;

    public void run() {
        try {
            // make sure we only bootstrap once per VM
            if (!hasBootstrapped) {
                // get the DB URL, and if it is a file-based DB, create the directory
                String dbUrl = ConfigurationAwareClassLoader.getENV().getProperty("db.url");
                log.debug( "db.url = " + dbUrl );
                
                if (dbUrl.startsWith(HSQLDB_FILE_PREFIX)) {
                    new File(dbUrl.replaceFirst(HSQLDB_FILE_PREFIX, "")).getParentFile().mkdirs();
                }

                // make sure the HSQL driver is loaded
                Class.forName("org.hsqldb.jdbcDriver");

                // open a connection as the root DB user
                Connection c = DriverManager.getConnection(dbUrl, "sa", "");

                // open the DDL file, and execute all the statements (delimited by semicolons)
                String ddlString = FileUtils.readFileToString(
                        new File(getClass().getClassLoader().getResource(HSQL_DDL).toURI()));
                String[] statements = ddlString.replaceAll("\\s+", " ").split(";");
                for (String statement : statements) {
                    c.createStatement().execute(statement);
                }
                c.commit();

                // set the hasBootstrapped flag so we don't do this again.
                hasBootstrapped = true;
            }
        } catch (Exception e) {
            // if anything goes wrong, we want startup to fail
            throw new RuntimeException(e);
        }
    }
}
