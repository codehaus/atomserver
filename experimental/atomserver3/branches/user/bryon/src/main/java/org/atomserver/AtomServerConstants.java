package org.atomserver;

import javax.xml.namespace.QName;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static java.lang.String.format;

public interface AtomServerConstants {

    public static final int MAJOR_VERSION = 3;

    public static final String DEFAULT_PAGE_SIZE_PARAMETER = "100";
    public static final int DEFAULT_PAGE_SIZE = Integer.valueOf(DEFAULT_PAGE_SIZE_PARAMETER);

    public static final String NAMESPACE = format("http://atomserver.org/%d", MAJOR_VERSION);

    public static final String PREFIX = "as";

    public static final QName END_INDEX = new QName(NAMESPACE, "endIndex", PREFIX);
    public static final QName ENTRY_ID = new QName(NAMESPACE, "entryId", PREFIX);

    public static final QName NAME = new QName(NAMESPACE, "name", PREFIX);
    public static final QName UPDATED = new QName(NAMESPACE, "updated", PREFIX);
    public static final QName CATEGORY_QUERY = new QName(NAMESPACE, "category-query", PREFIX);
    public static final QName FILTER = new QName(NAMESPACE, "filter", PREFIX);
    public static final QName STATUS = new QName(NAMESPACE, "status", PREFIX);
    public static final QName TIMESTAMP = new QName(NAMESPACE, "timestamp", PREFIX);
    public static final QName ETAG = new QName(NAMESPACE, "etag", PREFIX);
    public static final QName DELETED = new QName(NAMESPACE, "deleted", PREFIX);

    public interface Batch {

        public static final String NAMESPACE = format("%s/%s",
                AtomServerConstants.NAMESPACE,
                "batch");

        public static final String PREFIX = "asbatch";

        public static final QName OPERATION = new QName(NAMESPACE, "operation", PREFIX);
        public static final QName STATUS = new QName(NAMESPACE, "status", PREFIX);
        public static final QName RESULTS = new QName(NAMESPACE, "results", PREFIX);
    }

    public interface XPath {

        public static final String NAMESPACE = format("%s/%s",
                AtomServerConstants.NAMESPACE,
                "xpath");

        public static final String PREFIX = "asxpath";

        public static final QName XPATH_NAMESPACE = new QName(NAMESPACE, "namespace", PREFIX);
        public static final QName DELETE_ALL = new QName(NAMESPACE, "delete-all", PREFIX);
        public static final QName DELETE_SCHEME = new QName(NAMESPACE, "delete-scheme", PREFIX);
        public static final QName MATCH = new QName(NAMESPACE, "match", PREFIX);

        public static final QName SCRIPT = new QName(NAMESPACE, "script", PREFIX);
    }

    public static final String APPLICATION_APP_XML = "application/atomsvc+xml";

    public static final String OPTIMISTIC_CONCURRENCY_OVERRIDE = "*";

    public static final String HOSTNAME = Util.computeHostname();

    static class Util {
        static String computeHostname() {
            String hostname;
            try {
                hostname = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                hostname = "localhost";
            }
            return System.getProperty("org.atomserver.HOSTNAME", hostname);
        }
    }

}
