package org.atomserver;

import javax.xml.namespace.QName;
import static java.lang.String.format;

public interface AtomServerConstants {

    public static final int MAJOR_VERSION = 3;

    public static final String NAMESPACE = format("http://atomserver.org/%d", MAJOR_VERSION);

    public static final String PREFIX = "as";

    public static final QName END_INDEX = new QName(NAMESPACE, "endIndex", PREFIX);
    public static final QName ENTRY_ID = new QName(NAMESPACE, "entryId", PREFIX);
    public static final QName SERVER = new QName(NAMESPACE, "server", PREFIX);
    public static final QName SERVICE = new QName(NAMESPACE, "service", PREFIX);
    public static final QName WORKSPACE = new QName(NAMESPACE, "workspace", PREFIX);
    public static final QName COLLECTION = new QName(NAMESPACE, "collection", PREFIX);
    public static final QName LOCALE = new QName(NAMESPACE, "locale", PREFIX);

    public static final QName NAME = new QName(NAMESPACE, "name", PREFIX);
    public static final QName CATEGORY_QUERY = new QName(NAMESPACE, "category-query", PREFIX);
    public static final QName AGGREGATE = new QName(NAMESPACE, "aggregate", PREFIX);
    public static final QName AGGREGATE_CONTENT = new QName(NAMESPACE, "aggregate-content", PREFIX);
    public static final QName FILTER = new QName(NAMESPACE, "filter", PREFIX);
    public static final QName STATUS = new QName(NAMESPACE, "status", PREFIX);
    public static final QName TIMESTAMP = new QName(NAMESPACE, "timestamp", PREFIX);
    public static final QName ETAG = new QName(NAMESPACE, "etag", PREFIX);

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
}
