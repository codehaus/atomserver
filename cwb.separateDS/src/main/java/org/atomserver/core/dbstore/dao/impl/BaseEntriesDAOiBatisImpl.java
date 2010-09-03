/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.impl;

import org.atomserver.FeedDescriptor;
import org.atomserver.core.dbstore.dao.ContentDAO;
import org.atomserver.core.dbstore.dao.WriteReadEntryCategoriesDAO;
import org.atomserver.core.dbstore.dao.WriteReadEntryCategoryLogEventDAO;
import org.atomserver.utils.conf.ConfigurationAwareClassLoader;
import org.atomserver.utils.locale.LocaleUtils;
import org.springframework.jmx.export.annotation.ManagedAttribute;

import java.util.Date;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class BaseEntriesDAOiBatisImpl
        extends AbstractDAOiBatisImpl {

    public static final int UNDEFINED = -1;
    public static final Date ZERO_DATE = new Date(0L);

    private ContentDAO contentDAO;
    private WriteReadEntryCategoriesDAO entryCategoriesDAO;
    private WriteReadEntryCategoryLogEventDAO entryCategoryLogEventDAO;
    private int latencySeconds = UNDEFINED;


    public void setContentDAO(ContentDAO contentDAO) {
        this.contentDAO = contentDAO;
    }

    public void setEntryCategoriesDAO(WriteReadEntryCategoriesDAO entryCategoriesDAO) {
        this.entryCategoriesDAO = entryCategoriesDAO;
    }

    public void setEntryCategoryLogEventDAO(WriteReadEntryCategoryLogEventDAO entryCategoryLogEventDAO) {
        this.entryCategoryLogEventDAO = entryCategoryLogEventDAO;
    }

    public ContentDAO getContentDAO() {
        return contentDAO;
    }

    public WriteReadEntryCategoriesDAO getEntryCategoriesDAO() {
        return entryCategoriesDAO;
    }

    public WriteReadEntryCategoryLogEventDAO getEntryCategoryLogEventDAO() {
        return entryCategoryLogEventDAO;
    }

    @ManagedAttribute
    public int getLatencySeconds() {
        return latencySeconds;
    }

    @ManagedAttribute
    public void setLatencySeconds(int latencySeconds) {
        // this will be true if we are setting this for the second time through JMX
        if (this.latencySeconds != UNDEFINED) {
            // protect against a wacky value coming in through JMX
            int txnTimeout = UNDEFINED;
            String txnTimeoutStr = ConfigurationAwareClassLoader.getENV().getProperty("db.timeout.txn.put");
            if (txnTimeoutStr != null) {
                try {
                    txnTimeout = Integer.parseInt(txnTimeoutStr);
                } catch (NumberFormatException ee) {
                    log.error("setLatencySeconds; NumberFormatException:: ", ee);
                }
                txnTimeout = txnTimeout / 1000;
            } else {
                log.error("db.timeout.txn.put is NULL ");
            }

            if (!(latencySeconds < 0 || ((txnTimeout != UNDEFINED) && (latencySeconds < txnTimeout)))) {
                this.latencySeconds = latencySeconds;
            } else {
                log.error("The latency provided (" + latencySeconds + ") is less than txnTimeout (" +
                          txnTimeout + ")");
            }
        }
        this.latencySeconds = latencySeconds;
    }

    AbstractDAOiBatisImpl.ParamMap prepareParamMapForSelectEntries(Date updatedMin, Date updatedMax,
                                                                   int startIndex, int endIndex,
                                                                   int pageSize, String locale, FeedDescriptor feed) {

        if (updatedMin != null && updatedMin.equals(ZERO_DATE)) {
            updatedMin = null;
        }

        AbstractDAOiBatisImpl.ParamMap paramMap = paramMap()
                .param("workspace", feed.getWorkspace())
                .param("updatedMin", updatedMin)
                .param("updatedMax", updatedMax)
                .param("startIndex", (long) startIndex)
                .param("endIndex", (long) endIndex)
                .param("pageSize", pageSize)
                .param("collection", feed.getCollection());

        if (locale != null) {
            paramMap.param("undefinedCountry", "**").addLocaleInfo(LocaleUtils.toLocale(locale));
        }

        if (log.isDebugEnabled()) {
            log.debug("EntriesDAOiBatisImpl prepareParamMapForSelectEntries:: paramMap= " + paramMap);
        }
        return paramMap;
    }
}
