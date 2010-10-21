/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.impl;

import com.ibatis.sqlmap.client.SqlMapClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.core.dbstore.dao.AtomServerDAO;
import org.atomserver.core.dbstore.dao.impl.rwimpl.AbstractDAOiBatisImpl;

import javax.sql.DataSource;
import java.util.Date;

/**
 * A Delegator for the original DAOs.
 * We had to leave the single DAOs around, as well as the single Impls.
 * Both of which now delegate to the new forms.
 * This way we did not have to rewrite tests, and more important,
 * the change is, essentially, transparent to the end users.
 * They only need to add a couple of properties, and change
 * the location of the Impl in their Spring config.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
abstract public class AbstractDAOiBatisImplDelegator {
    static protected final Log log = LogFactory.getLog(AbstractDAOiBatisImpl.class);
    static public final String DEFAULT_DB_TYPE = "sqlserver";

    /**
     * valid values are "hsql", "mysql" and "sqlserver"
     */
    protected String dbType = DEFAULT_DB_TYPE;
    protected DataSource dataSource;

    protected SqlMapClient sqlMapClient;

    public SqlMapClient getSqlMapClient() {
        return sqlMapClient;
    }

    public void setSqlMapClient(SqlMapClient sqlMapClient) {
        this.sqlMapClient = sqlMapClient;
    }

    public final void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public final DataSource getDataSource() {
        return dataSource;
    }

    public String getDatabaseType() {
        return dbType;
    }

    public void setDatabaseType(String dbType) {
        if (AtomServerDAO.DatabaseType.isValidType(dbType)) {
            log.info("Database Type = " + dbType);
            this.dbType = dbType;
        } else {
            throw new IllegalArgumentException(dbType + " is not a valid DatabaseType value");
        }
    }

    public Date selectSysDate() {
        return getReadDAO().selectSysDate();
    }

    public void testAvailability() {
        getReadDAO().testAvailability();
    }

    abstract public AbstractDAOiBatisImpl getReadDAO();
}
