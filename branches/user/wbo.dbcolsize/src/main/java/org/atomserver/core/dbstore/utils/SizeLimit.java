/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.utils;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;


/**
 * SizeLimit - The class which maintains the maximum size settings for specific attributes.
 * The attributes for which the maximum size is tracked are:
 * <ul>
 * <li>EntryId</li>
 * <li>Category Scheme</li>
 * <li>Category Term</li>
 * <li>Category Label</li>
 * </ul>
 * The default values are overwritten by the database column sizes.
 */
public class SizeLimit {

    private final Log log = LogFactory.getLog(SizeLimit.class);

    // One time initialization flag
    private static boolean initialized = false;

    // A map to track sizes.
    private static final Map<String, Integer> attributeSize = new HashMap<String, Integer>();

    // defaults sizes
    private static final int ENTRYID_DEFAULT_SIZE = 256;
    private static final int SCHEME_DEFAULT_SIZE = 128;
    private static final int TERM_DEFAULT_SIZE = 32;
    private static final int LABEL_DEFAULT_SIZE = 128;

    static {
        attributeSize.put(getColumnKey("entrystore", "entryid"), ENTRYID_DEFAULT_SIZE);
        attributeSize.put(getColumnKey("entrycategory", "scheme"), SCHEME_DEFAULT_SIZE);
        attributeSize.put(getColumnKey("entrycategory", "term"), TERM_DEFAULT_SIZE);
        attributeSize.put(getColumnKey("entrycategory", "label"), LABEL_DEFAULT_SIZE);
    }

    private BasicDataSource dataSource = null;

    /**
     * validate EntryId size
     * @param entryId entry id to validate
     * @return true if the entry id size is valid
     */
    public boolean isValidEntryId(String entryId) {
        return isValidSize(entryId, getEntryIdSize());
    }

    /**
     * Maximum EntryId size
     * @return maximum entry id size
     */
    public int getEntryIdSize() {
        return getColumnSize("entrystore", "entryid");
    }

    /**
     * validate Scheme size
     * @param scheme scheme value to validate
     * @return true if the scheme size is valid
     */
    public boolean isValidScheme(String scheme) {
        return isValidSize(scheme, getSchemeSize());
    }

    /**
     * Maximum Scheme size
     * @return maximum size of Scheme
     */
    public int getSchemeSize() {
        return getColumnSize("entrycategory", "scheme");
    }

    /**
     * validate Term size
     * @param term term value to validate
     * @return true if the term size is valid
     */
    public boolean isValidTerm(String term) {
        return isValidSize(term, getTermSize());
    }

    /**
     * Maximum Term size
     * @return the maximum term size
     */
    public int getTermSize() {
        return getColumnSize("entrycategory", "term");
    }

    /**
     * validate Label size
     * @param label label value to validate
     * @return true if the label size is valid
     */
    public boolean isValidLabel(String label) {
        return isValidSize(label, getLabelSize());
    }

    /**
     * Maximum Label size
     * @return the maximum label size
     */
    public int getLabelSize() {
        return getColumnSize("entrycategory", "label");
    }

    /**
     * Get the data source
     * @return  the data source
     */
    public BasicDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Set the data source
     * @param dataSource Data Source to use in determining the size limits.
     */
    public void setDataSource(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    // basic size check
    private boolean isValidSize(String value, Integer maxSize) {
        return (value == null) || ("".equals(value)) ||
               ((maxSize != null) && (maxSize >= value.length()));
    }

    // Initialize from data source to override the settings.
    private void init() {
        if(dataSource == null) {
            return;
        }

        // biuld a set to filter table by name
        Set<String> tables = new HashSet<String>();
        Set<String> keys = attributeSize.keySet();
        for (String key : keys) {
            String tableName = key.substring(0, key.indexOf("."));
            tables.add(tableName);
        }

        try {
            // get metadata
            Connection conn = dataSource.getConnection();
            DatabaseMetaData md = conn.getMetaData();
            ResultSet dbTables = md.getTables(null, null, "%", new String[]{"TABLE"});
            // update max size from column size
            while (dbTables.next()) {

                String tableName = dbTables.getString(3);
                if (!tables.contains(tableName.toLowerCase())) {
                    continue;   // skip tables not interested.
                }

                ResultSet columns = md.getColumns(null, null, tableName, "%");
                while (columns.next()) {

                    String columnName = columns.getString(4).toLowerCase();
                    String key = getColumnKey(tableName.toLowerCase(), columnName);

                    if (keys.contains(key)) {
                        String size = columns.getString(7);
                        if (size != null) {
                            log.debug("Size Limit:" + key + " max-size:" + size);
                            attributeSize.put(key, Integer.valueOf(size));
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            log.error("Error in SizeLimit settings", e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        } finally {
            initialized = true; // done with init.
        }
    }

    private Integer getColumnSize(String tableName, String columnName) {
        if (!initialized) {
            init();
        }
        return attributeSize.get(getColumnKey(tableName, columnName));
    }

    private static String getColumnKey(String tname, String cname) {
        return tname + "." + cname;
    }
}
