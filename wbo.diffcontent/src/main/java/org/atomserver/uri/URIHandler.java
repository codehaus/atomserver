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

package org.atomserver.uri;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.protocol.Request;
import org.apache.abdera.protocol.Resolver;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.Target;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.AtomCategory;
import org.atomserver.AtomService;
import org.atomserver.EntryDescriptor;
import org.atomserver.FeedDescriptor;
import org.atomserver.core.WorkspaceOptions;
import org.atomserver.core.dbstore.utils.SizeLimit;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.exceptions.BadRequestException;
import org.atomserver.utils.collections.ArraySegmentIterator;
import org.atomserver.utils.locale.LocaleUtils;
import org.atomserver.utils.logic.BooleanExpression;
import org.atomserver.utils.logic.Conjunction;
import org.atomserver.utils.logic.Disjunction;
import org.atomserver.utils.logic.TermDictionary;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * URIHandler - The class which decodes the URL for AtomServer. This class extends Abdera's Target Resolver.
 * The class creates the appropriate AtomServer URITarget; EntryTarget, FeedTarget, or ServiceTarget from the
 * URL.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class URIHandler
        implements Resolver<Target> {

    private static final Log log = LogFactory.getLog(URIHandler.class);

    public static final int REVISION_OVERRIDE = -1;

    private AtomService atomService = null;

    private String rootPath = null;
    private String contextPath = null;
    public static final Pattern JOIN_WORKSPACE_PATTERN =
            Pattern.compile("\\$join(?:\\((\\w+(?:,\\s*\\w+)*)\\))?");
    private SizeLimit sizeLimit = null;     // injected by spring if database storage.

    private class ParsedTarget {
        private final URITarget target;
        private final RuntimeException exception;

        private ParsedTarget(URITarget target, RuntimeException exception) {
            this.target = target;
            this.exception = exception;
        }
    }

    public void setAtomService(AtomService atomService) {
        this.atomService = atomService;
    }

    public String getServletContext() {
        return rootPath;
    }

    public String getServletMapping() {
        return contextPath;
    }

    /**
     * Set the root path (the Servlet Context) for the URL
     * This is meant to be set from the IOC container (e.g. Spring)
     * @param rootPath
     */
    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    /**
     * Set the Context path from the URL
     * This is meant to be set from the IOC container (e.g. Spring)
     * @param contextPath
     */
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    /**
     * Get the size limit settings
     * @return
     */
    public SizeLimit getSizeLimit() {
        return sizeLimit;
    }

    /**
     * Set the size limit settings
     * @param sizeLimit
     */
    public void setSizeLimit(SizeLimit sizeLimit) {
        this.sizeLimit = sizeLimit;
    }


    /**
     * Construct a URI string that matches the supplied parameters
     * The URI looks like this; </p>
     * /[root]/[context]/[workspace]/[collection]/[entryId].[locale].xml/[revision] </p>
     * (e.g. /foodata/v1/foo/bars/1234.en_GB.xml/3 )
     * @param workspace  Required
     * @param collection Required
     * @param entryId Required
     * @param locale May be null
     * @param revision If equal to REVISION_OVERRIDE (a static in this class) then "/*" is used
     * @return The constructed URI String
     */
    public String constructURIString(String workspace,
                                     String collection,
                                     String entryId,
                                     Locale locale,
                                     int revision) {
        String uri = constructURIString(workspace, collection, entryId, locale);
        uri += ((revision == REVISION_OVERRIDE) ? "/*" : ("/" + revision));
        return uri;
    }

    /**
     * Construct a URI string that matches the supplied parameters
     * The URI looks like this; </p>
     * /[root]/[context]/[workspace]/[collection]/[entryId].[locale].xml </p>
     * (e.g. /foodata/v1/foo/bars/1234.en_GB.xml )
     * @param workspace  Required
     * @param collection Required
     * @param entryId Required
     * @param locale May be null
     * @return The constructed URI String
     */
    public String constructURIString(String workspace,
                                     String collection,
                                     String entryId,
                                     Locale locale) {
        String fileName = constructFileName(entryId, locale);
        return "/" + getServiceBaseUri() + "/" + workspace +
               "/" + collection + "/" + fileName;
    }

    /**
     * Construct the filename portion of the URI
     * The URI filename looks like this; </p>
     * [entryId].[locale].xml  (e.g. 1234.en_GB.xml)</p>
     * where locale is optional.
     * @param entryId Required
     * @param locale May be null
     * @return The filename portion of the URI
     */
    public String constructFileName(String entryId,
                                    Locale locale) {
        return locale != null ?
               (entryId + "." + locale.toString() + ".xml") :
               (entryId + ".xml");
    }

    /**
     * Return the base URI, which is /[root]/[context] where either of these may be null.
     * @return
     */
    public String getServiceBaseUri() {
        return StringUtils.isEmpty(rootPath) ? "" :
               StringUtils.isEmpty(contextPath) ? rootPath :
               rootPath + "/" + contextPath;
    }

    /**
     * Parse the Request to get an EntryTarget. Note that the Target may be pulled from an internal cache.
     * @param request
     * @param checkIfCollectionExists If true, verify that the Collection exists during parsing
     * @return The EntryTarget
     */
    public EntryTarget getEntryTarget(Request request, boolean checkIfCollectionExists) {
        URIHandler.ParsedTarget parsedTarget = lazyParseTarget(request);
        if (parsedTarget.exception != null) {
            throw parsedTarget.exception;
        }
        EntryTarget entryTarget = (EntryTarget) parsedTarget.target;

        verifyURIMatchesStorage(entryTarget,
                                ((RequestContext) request).getResolvedUri(),
                                checkIfCollectionExists);

        WorkspaceOptions options = atomService.getAtomWorkspace(entryTarget.getWorkspace()).getOptions();
        if (options.getDefaultLocalized() &&
            !entryTarget.getEntryId().startsWith("$") &&
            entryTarget.getLocale() == null) {
            throw new BadRequestException(
                    MessageFormat.format("unable to decode a locale from {0}",
                                         ((RequestContext) request).getUri()));
        }
        return entryTarget;
    }

    /**
     * Parse the Request to get a FeedTarget. Note that the Target may be pulled from an internal cache.
     * @param request
     * @return The FeedTarget
     */
    public FeedTarget getFeedTarget(Request request) {
        URIHandler.ParsedTarget parsedTarget = lazyParseTarget(request);
        if (parsedTarget.exception != null) {
            throw parsedTarget.exception;
        }
        FeedTarget feedTarget = (FeedTarget) parsedTarget.target;
        verifyURIMatchesStorage(feedTarget,
                                ((RequestContext) request).getResolvedUri(),
                                true);
        return feedTarget;
    }

    /**
     * Parse the Request to get a ServiceTarget. Note that the Target may be pulled from an internal cache.
     * @param request
     * @return The ServiceTarget
     */
    public ServiceTarget getServiceTarget(Request request) {
        URIHandler.ParsedTarget parsedTarget = lazyParseTarget(request);
        if (parsedTarget.exception != null) {
            throw parsedTarget.exception;
        }
        return (ServiceTarget) parsedTarget.target;
    }

    /**
     * {@inheritDoc}
     */
    public Target resolve(Request request) {
        URIHandler.ParsedTarget parsedTarget = lazyParseTarget(request);
        return parsedTarget == null ? null : parsedTarget.target;
    }

    private ParsedTarget lazyParseTarget(Request request) {
        // every request that comes in is going to be an instance of RequestContext (we know this
        // because this cast is the very first thing that the default Abdera resolver does)
        RequestContext requestContext = (RequestContext) request;

        // we need to operate on the path relative to the base URI of the service, so that the
        // service can be installed at any context path
        if (log.isDebugEnabled()) {
            log.debug("base uri:            [" + requestContext.getBaseUri() + "]");
            log.debug("resolved uri:        [" + requestContext.getResolvedUri() + "]");
        }

        IRI iri = requestContext.getBaseUri().relativize(requestContext.getResolvedUri());
        if (log.isDebugEnabled()) {
            log.debug("decoded RELATIVE URI [" + iri + "]");
        }

        return parseTargetFromIRI(requestContext, iri);
    }

    /**
     * Parse an AtomServer URITarget from the IRI
     * @param requestContext
     * @param iri
     * @return The URITarget
     */
    public URITarget parseIRI(RequestContext requestContext, IRI iri) {
        if (log.isDebugEnabled()) {
            log.debug("parsing IRI [" + iri + "]");
        }

        // We cannot allow "fragments" or "anchors" (e.g. /foo/bar/#baz )
        //   because we cannot tell this from a legitimate EntryId, etc.
        if (iri.getFragment() != null) {
            String msg = "Could no parse the URI. It contains a fragment (i.e. an anchor - e.g. /foo/bar/#baz)";
            log.error( msg );
            throw new AtomServerException( msg );
        }

        URIHandler.ParsedTarget parsedTarget = parseTargetFromIRI(requestContext, iri);
        if (parsedTarget == null) {
            return null;
        } else if (parsedTarget.exception != null) {
            throw parsedTarget.exception;
        }
        return parsedTarget.target;
    }

    private ParsedTarget parseTargetFromIRI(RequestContext requestContext, IRI iri) {
        RuntimeException exception = null;
        // we can split the relative path into the "path" and the "categories" around the /-/
        Set<BooleanExpression<AtomCategory>> categoriesQuery = null;
        String fullPathString = iri.getPath();
        if (log.isDebugEnabled()) {
            log.debug("parseTargetFromIRI:: fullPathString " + fullPathString);
        }

        String[] pathAndCategories = fullPathString.split("\\/\\-\\/", 2);
        if (pathAndCategories.length > 1) {
            String categoriesString = pathAndCategories[1];
            if (log.isDebugEnabled()) {
                log.debug("decoded CATEGORIES PATH [" + categoriesString + "]");
            }

            categoriesQuery = decodeCategoryFields(categoriesString);
        }

        // the "path" is now just the identifier for the service/collection/entry
        String pathString = pathAndCategories[0];

        // strip off the context path
        if (!StringUtils.isEmpty(contextPath)) {
            pathString = pathString.replaceAll("^\\/?" + contextPath + "\\/?", "");
        }
        String[] path = pathString
                // strip off any leading or trailing slashes
                .replaceAll("^\\/|\\/$", "")
                        // and then split by slashes
                .split("\\/");

        // the path should contain between 1 and 4 slash-separated components.  with more or less
        // components, we log a warning and return null - otherwise we decompose the path into its
        // parts.
        String workspace = null;
        String collection = null;
        FileInfo fileInfo = null;
        Integer revision = null;
        Locale locale = null;
        switch (path.length) {
        case 4:
            try {
                revision = "*".equals(path[3]) ? REVISION_OVERRIDE : Integer.parseInt(path[3]);
            } catch (NumberFormatException e) {
                log.error("failed to parse " + path[3] + " as a revision number");
                return null;
            }
            if (log.isDebugEnabled()) {
                log.debug("decoded REVISION    [" + revision + "]");
            }
        case 3:
            fileInfo = decodeFileName(path[2]);
            if (log.isDebugEnabled()) {
                log.debug("decoded ENTRY ID    [" + fileInfo.getEntryId() + "]");
            }
        case 2:
            collection = path[1];
            if (log.isDebugEnabled()) {
                log.debug("decoded COLLECTION  [" + collection + "]");
            }
        case 1:
            workspace = path[0];
            if (log.isDebugEnabled()) {
                log.debug("decoded WORKSPACE   [" + workspace + "]");
            }
            break;
        default:
            log.warn("invalid uri - path contains " + path.length + " components, " +
                     "should have 1-4");
            return null;
        }

        try {
            locale = fileInfo != null && fileInfo.getLocale() != null ?
                     fileInfo.getLocale() :
                     (Locale) QueryParam.locale.parse(requestContext);
        } catch (RuntimeException e) {
            log.error("parsed an invalid locale " +
                      requestContext.getParameter(QueryParam.locale.getParamName()));
            exception = e;
        }

        // we reserve all workspace, collection, and entry ids that start with "$" - we need
        // to check that if there are any such tokens, they are in the set that we support.
        
        // workspaces that start with $ are special - currently we only support $join
        if (workspace != null && workspace.startsWith("$")) {
            if (!JOIN_WORKSPACE_PATTERN.matcher(workspace).matches()) {
                return null;
            }
        }
        // collections that start with $ are special - currently we don't support any
        if (collection != null && collection.startsWith("$")) {
            return null;
        }
        // entry ids that start with $ are special - currently we only support $batch
        if (fileInfo != null && fileInfo.getEntryId().startsWith("$")) {
            if (!"$batch".equals(fileInfo.getEntryId())) {
                return null;
            }
        }

        log.debug("locale= " + locale);

        URITarget target =
                fileInfo != null ? new EntryTarget(requestContext, workspace, collection,
                                                   fileInfo.getEntryId(), revision, locale)
                : collection != null ?
                  "POST".equals(requestContext.getMethod()) ?
                      new EntryTarget(requestContext, workspace, collection,
                                      revision, locale)
                    : new FeedTarget(requestContext, workspace, collection, categoriesQuery)
                  : new ServiceTarget(requestContext, workspace);


        return new ParsedTarget(target, exception);
    }

    static private final String SCHEME_START_CHAR = "(";
    static private final String SCHEME_END_CHAR = ")";

    private Set<BooleanExpression<AtomCategory>> decodeCategoryFields(String categoriesString) {
        Set<BooleanExpression<AtomCategory>> categoriesQuery = new HashSet<BooleanExpression<AtomCategory>>();
        TermDictionary<AtomCategory> dictionary = new TermDictionary<AtomCategory>();
        Iterator<String> iterator = new ArraySegmentIterator<String>(categoriesString.split("\\/"), 0);
        while (iterator.hasNext()) {
            categoriesQuery.add(decodeCategoryExpression(categoriesString, iterator, dictionary));
        }
        return categoriesQuery;
    }


    private BooleanExpression<AtomCategory> decodeCategoryExpression( String categoriesString,
                                                                      Iterator<String> iterator,
                                                                      TermDictionary<AtomCategory> dictionary) {
        if (!iterator.hasNext()) {
            String msg = MessageFormat.format(
                    "The Category Query [{0}] was malformed", categoriesString);
            log.error(msg);
            throw new BadRequestException(msg);
        }
        String category = iterator.next();
        if ("AND".equalsIgnoreCase(category)) {
            return new Conjunction<AtomCategory>(
                    decodeCategoryExpression(categoriesString, iterator, dictionary),
                    decodeCategoryExpression(categoriesString, iterator, dictionary));
        } else if ("OR".equalsIgnoreCase(category)) {
            return new Disjunction<AtomCategory>(
                    decodeCategoryExpression(categoriesString, iterator, dictionary),
                    decodeCategoryExpression(categoriesString, iterator, dictionary));
        } else {

            String scheme = null;
            String term = null;
            String[] catParams = category.split("\\" + SCHEME_END_CHAR);
            if (log.isTraceEnabled()) {
                log.trace("categoryField= " + category);
                log.trace("catParams.length= " + catParams.length);
                for (int ii = 0; ii < catParams.length; ii++) {
                    log.trace("catParams[" + ii + "]= " + catParams[ii]);
                }
            }
            switch (catParams.length) {
            case 1:
                term = catParams[0];
                break;
            case 2:
                if (!catParams[0].startsWith(SCHEME_START_CHAR)) {
                    String msg = MessageFormat.format(
                            "The Category [{0}] was not properly formatted:: i.e. (scheme)term", category);
                    log.error(msg);
                    throw new BadRequestException(msg);
                }
                scheme = catParams[0].substring(1);
                term = catParams[1];
                break;
            default:
                String msg = MessageFormat.format(
                        "The Category [{0}] was not properly formatted:: i.e. (scheme)term", category);
                log.error(msg);
                throw new BadRequestException(msg);
            }
            if (log.isDebugEnabled()) {
                log.debug("adding new query Category:: [" + scheme + ", " + term + "]");
            }
            return dictionary.termFor(new AtomCategory(scheme, term));
        }
    }

    private interface FileInfo {
        String getEntryId();

        Locale getLocale();
    }

    private FileInfo decodeFileName(String fileName) {
        final String[] fileNameParts = fileName.split("\\.");
        if ("".equals(fileNameParts[0])) {
            throw new BadRequestException("Empty entry ID is not allowed in [" + fileName + "]");
        }
        switch (fileNameParts.length) {
        case 1:
        case 2:
            // if the filename is of the form "1234" or "1234.xml"
            return new FileInfo() {
                public String getEntryId() {
                    return fileNameParts[0];
                }

                public Locale getLocale() {
                    return null;
                }
            };
        case 3:
            // if the filename is of the form "1234.en_GB.xml"
            try {
                final Locale locale = LocaleUtils.toLocale(fileNameParts[1]);
                return new FileInfo() {
                    public String getEntryId() {
                        return fileNameParts[0];
                    }

                    public Locale getLocale() {
                        return locale;
                    }
                };
            } catch (Exception e) {
                return new FileInfo() {
                    public String getEntryId() {
                        return fileNameParts[0];
                    }

                    public Locale getLocale() {
                        throw new BadRequestException(
                                MessageFormat.format(
                                        "Unable to parse locale from [{0}]", fileNameParts[1]));
                    }
                };
            }
        default:
            throw new BadRequestException(
                    MessageFormat.format("Invalid file name [{0}] in URI.", fileName));
        }
    }

    private void verifyURIMatchesStorage(FeedDescriptor feedDescriptor,
                                         IRI iri,
                                         boolean checkIfCollectionExists)
            throws BadRequestException {
        verifyURIMatchesStorage(feedDescriptor.getWorkspace(),
                                feedDescriptor.getCollection(),
                                iri,
                                checkIfCollectionExists);
    }

    private void verifyURIMatchesStorage(EntryDescriptor entryDescriptor,
                                         IRI iri,
                                         boolean checkIfCollectionExists)
            throws BadRequestException {
        verifyURIMatchesStorage(entryDescriptor.getWorkspace(),
                                entryDescriptor.getCollection(),
                                iri,
                                checkIfCollectionExists);
        verifySizeLimits(entryDescriptor);
    }

    private void verifyURIMatchesStorage(String workspace,
                                         String collection,
                                         IRI iri,
                                         boolean checkIfCollectionExists)
            throws BadRequestException {
        if ((JOIN_WORKSPACE_PATTERN.matcher(workspace).matches() || "$aggregate".equals(workspace))) {
            if (collection == null) {
                throw new BadRequestException("you must specify a Category Scheme to use as the " +
                                              "collection for an aggregate feed or entry.");
            }
        } else {
            atomService.verifyURIMatchesStorage(workspace, collection, iri, checkIfCollectionExists);
        }
    }

    private void verifySizeLimits(EntryDescriptor entryDescriptor) {
        if(entryDescriptor != null && sizeLimit != null) {
            String entryId = entryDescriptor.getEntryId();
            if (!sizeLimit.isValidEntryId(entryId)) {
                String msg = "An EntryId must NOT be longer than " + sizeLimit.getEntryIdSize() +
                             " characters. The EntryId [" + entryId + "] was not properly formatted";
                log.error(msg);
                throw new BadRequestException(msg);
            }
        }
    }

}
