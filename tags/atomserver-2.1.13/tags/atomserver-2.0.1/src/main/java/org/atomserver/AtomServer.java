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

package org.atomserver;

import org.atomserver.ext.batch.Operation;
import org.atomserver.ext.batch.Results;
import org.atomserver.ext.batch.Status;
import org.atomserver.core.etc.AtomServerConstants;
import org.atomserver.uri.FeedTarget;
import org.atomserver.uri.EntryTarget;
import org.atomserver.exceptions.*;
import org.atomserver.utils.perf.AutomaticStopWatch;
import org.atomserver.utils.perf.PerformanceLog;
import org.atomserver.utils.perf.StopWatch;
import org.atomserver.utils.IOCLog;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.i18n.iri.IRISyntaxException;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Collection;
import org.apache.abdera.model.Categories;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.abdera.model.Workspace;
import org.apache.abdera.protocol.error.Error;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.impl.AbstractProvider;
import org.apache.abdera.protocol.server.impl.AbstractResponseContext;
import org.apache.abdera.protocol.server.impl.BaseResponseContext;
import org.apache.abdera.protocol.server.impl.EmptyResponseContext;
import org.apache.abdera.protocol.server.impl.HttpServletRequestContext; 
import org.apache.abdera.util.EntityTag;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AtomServer - This class is the true entry point for an AtomServer web application - the AbderaServlet
 * ultimately delegates to this class. AtomServer is an Abdera Provider which delegates to an underlying AtomService.
 * It provides a basic wrapper to the standard Atom "control flow", which is explained briefly below;
 * <p/>
 * Central to the Atom Publishing Protocol is the concept of collections of editable resources that are represented
 * by Atom Feed and Entry documents. A collection has a unique URI. Issuing an HTTP GET request
 * to that URI returns an Atom Feed Document. To create new entries in that feed, clients either send HTTP POST
 * requests to the collection's URI, or alternately send HTTP PUT requests to the entries URI.
 * When POST-ed those newly created entries will be assigned their own unique entry Id (edit URI).
 * And when PUT, the newly created entries will be assigned the unique entry Id as determined from the URI.
 * To modify those entries, the client simply retrieves the resource from the collection, makes its modifications,
 * then puts it back. Removing the entry from the feed is a simple matter of issuing an HTTP DELETE request to the
 * appropriate edit URI. All operations are performed using simple HTTP requests and can usually be performed
 * with nothing more than a simple text editor and a command prompt.
 * <p/>
 * 
 * <pre>
 * HTTP STATUS CODES
 * Code                                     Explanation
 * 200 OK 	                    No error.
 * 201 CREATED 	                Creation of a resource was successful.
 * 304 NOT MODIFIED           	The resource hasn't changed since the time specified in the request's If-Modified-Since header.
 * 400 BAD REQUEST 	            Invalid request URI or header, or unsupported nonstandard parameter.
 * 401 UNAUTHORIZED 	        Authorization required.
 * 403 FORBIDDEN                Unsupported standard parameter, or authentication or authorization failed.
 * 404 NOT FOUND                Resource (such as a feed or entry) not found.
 * 409 CONFLICT                 Specified version number doesn't match resource's latest version number.
 * 422 BAD CONTENT              The Entry <content> is not valid
 * 500 INTERNAL SERVER ERROR 	Internal error. This is the default code that is used for all unrecognized errors.
 * </pre>
 * <p/>
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class AtomServer extends AbstractProvider {

    // ------------
    //   statics 
    //--------------
    static protected Log logger = LogFactory.getLog(AtomServer.class);
    private static final Pattern BATCH_ENTRY_PATTERN = Pattern.compile("/([^/#?]+)/([^/#?]+)/\\$batch");
    private static int DEFAULT_PAGE_SIZE = 100;

    static public Factory getFactory(Abdera abdera) {
        return new org.apache.abdera.parser.stax.FOMFactory(abdera);
    }

    // ------------
    //   instance
    //--------------
    private EntityTag service_etag = new EntityTag("service");
    private AtomService atomService;
    private PerformanceLog perflog;
    private IOCLog errlog;


    /**
     * Set the REQUIRED AtomService for use by this AtomServer. This method is meant to
     * inject an AtomService which has been configured externally in an IOC container like Spring.
     * @param atomService  Value to set
     */
    public void setService(AtomService atomService) {
        if (logger.isDebugEnabled()) {
            logger.debug("AtomServer.setService: service= " + atomService);
        }
        this.atomService = atomService;
    }


    /**
     * Set the optional Performance log. If present a logger will log performance statistics
     * (i.e. start time, and elapsed time in milliseconds, along with some identifier) at
     * several pertinent points within the AtomServer, including all methods in this class,
     * as well as wrapping database access methods, etc. This method is meant to
     * inject an PerformanceLog which has been configured externally in an IOC container like Spring.
     * It is in this external configuration that you specify such details as the logger name, which, in turn,
     * will define the actual name of the performance log file.
     * @param perflog   Value to set
     */
    public void setPerformanceLog( PerformanceLog perflog ) {
        if (logger.isDebugEnabled()) {
            logger.debug("AtomServer.setPerformanceLog: perflog= " + perflog);
        }
        this.perflog = perflog;
    }

    /**
     * Set the optional "500 Error" log. If present a logger will log all Server Error (i.e. 500 Errors)
     * to a special log, which makes them much easier to find than trolling through the log of
     * standard output, access logs, etc. In addition, the errors are logged with much more detail that they are
     * when logged to standard out.  This method is meant to
     * inject an IOCLog which has been configured externally in an IOC container like Spring.
     * It is in this external configuration that you specify such details as the logger name, which, in turn,
     * will define the actual name of the error log file.
     * @param errlog   Value to set
     */
    public void setErrorLog( IOCLog errlog ) {
        if (logger.isDebugEnabled()) {
            logger.debug("AtomServer.setErrorLog: errlog= " + errlog);
        }
        this.errlog = errlog;
    }

    /**
     * Called for a HTTP GET to an SERVICE URL.
     * <p/>
     * The first step to using any APP-enabled service is to determine what collections are available
     * and what types of resources those collections can contain.
     * The Atom protocol specification defines an XML format known as a service document that
     * a client can use to introspect an endpoint. This method returns that document
     * @param request The Abdera RequestContext
     * @return response The Abdera ResponseContext, which will contain the appropriate HTTP status code,
     * as detailed above.
     */
    public ResponseContext getService(RequestContext request) {
        StopWatch stopWatch = new AutomaticStopWatch();
        if (logger.isInfoEnabled()) {
            logger.info("GET Service:: [ " + request.getUri() + " ]");
        }
        Abdera abdera = request.getServiceContext().getAbdera();
        try {
            Factory factory = AtomServer.getFactory(abdera);

            org.apache.abdera.model.Service service = factory.newService();

            java.util.Collection<String> workspaces = this.atomService.listWorkspaces(request);
            if (logger.isTraceEnabled()) {
                logger.trace("AtomServer.getService:: workspaces= " + workspaces);
            }
            if ( workspaces == null ) {
               throw new BadRequestException( "The workspace indicated in the Service Request: " + request.getUri()
                                              + " does NOT exist" );
            }

            for (String wsName : workspaces) {
                Workspace workspace = service.addWorkspace(wsName);

                AtomWorkspace atomWorkspace = this.atomService.getAtomWorkspace(wsName);
                java.util.Collection<Collection> collections = atomWorkspace.listCollections(request);
                if (logger.isTraceEnabled()) {
                    logger.trace("AtomServer.getService:: [" + wsName + "] collections= " + collections);
                }

                if (collections == null) {
                    return servererror(abdera, request, "The collections was null", null);
                } else {
                    for (Collection coll : collections) {
                        try {
                            String collName = coll.getTitle();
                            org.apache.abdera.model.Collection collection = 
                                workspace.addCollection(collName, wsName + '/' + coll.getTitle() + '/');
                            collection.setAccept("entry");

                            java.util.Collection<Category> categoryList =
                                    atomWorkspace.getAtomCollection(collName).listCategories(request);
                            if (logger.isTraceEnabled()) {
                                logger.trace("AtomServer.getService:: [" + wsName + ", " + collName + "] categoryList= "
                                             + categoryList);
                            }

                            if (categoryList != null) {
                                Categories categories = collection.addCategories();
                                for (Category category : categoryList) {
                                    categories.addCategory(category);
                                }
                            } else {
                                collection.addCategories().setFixed(false);
                            }

                        } catch (IRISyntaxException e) {
                            throw new BadRequestException(e);
                        }
                    }
                }
            }
            Document<org.apache.abdera.model.Service> doc = service.getDocument();
            BaseResponseContext rc = new BaseResponseContext<Document<org.apache.abdera.model.Service>>(doc);
            rc.setEntityTag(service_etag);
            return rc;
        } catch (Throwable e) {
            return handleTopLevelException(e, abdera, request);
        }
        finally {
            if (perflog != null) {
                perflog.log("GET.service", request.getUri().getPath(), stopWatch);
            }
        }
    }

    /**
     * Called for a HTTP GET to an FEED URL.
     * <p/>
     * The Atom protocol specification defines an XML format known as a feed document.
     * This method returns that document, responding to a HTTP GET. The URL defines precisely
     * which Atom workspace and collection to return.
     * @param request The Abdera RequestContext
     * @return response The Abdera ResponseContext, which will contain the appropriate HTTP status code,
     * as detailed above.
     */
    public ResponseContext getFeed(RequestContext request) {
        StopWatch stopWatch = new AutomaticStopWatch();
        if (logger.isInfoEnabled()) {
            logger.info("GET Feed:: [ " + request.getUri() + " ]");
        }
        Abdera abdera = request.getServiceContext().getAbdera();
        try {
            FeedTarget feedTarget = atomService.getURIHandler().getFeedTarget(request);
                Feed feed = atomService.getAtomWorkspace(feedTarget.getWorkspace())
                                .getAtomCollection(feedTarget.getCollection()).getEntries(request);

            if (feed == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("AtomServer.getFeed() THE FEED IS NOT MODIFIED -- RETURNING 304");
                }
                return notmodified(abdera, request, "No Entries were found modified since " + request.getIfModifiedSince());
            } else {
                Document<Feed> doc = feed.getDocument();
                BaseResponseContext rc = new BaseResponseContext<Document<Feed>>(doc);
                try {
                    rc.setEntityTag(new EntityTag(feed.getId().toString()));
                } catch (IRISyntaxException e) {
                    throw new BadRequestException(e);
                }
                return rc;
            }
        } catch (Throwable e) {
            return handleTopLevelException(e, abdera, request);
        }
        finally {
            if (perflog != null) {
                perflog.log("GET.feed", request.getUri().getPath(), stopWatch);
            }
        }
    }

    /**
     * Called for a HTTP GET to an ENTRY URL.
     * <p/>
     * The Atom protocol specification defines an XML format known as a entry document.
     * This method returns that document, responding to an HTTP GET.  The URL defines precisely
     * which Atom workspace and collection to return.
     * @param request The Abdera RequestContext
     * @return response The Abdera ResponseContext, which will contain the appropriate HTTP status code,
     * as detailed above
     */
    public ResponseContext getEntry(RequestContext request) {
        StopWatch stopWatch = new AutomaticStopWatch();
        if (logger.isInfoEnabled()) {
            logger.info("GET Entry:: [ " + request.getUri() + " ]");
        }
        Abdera abdera = request.getServiceContext().getAbdera();
        try {
            EntryTarget entryTarget = atomService.getURIHandler().getEntryTarget(request, false);
            Entry entry = atomService.getAtomWorkspace(entryTarget.getWorkspace())
                             .getAtomCollection(entryTarget.getCollection()).getEntry(request);

            if (entry == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("AtomServer.getEntry() THE ENTRY IS NOT MODIFIED -- RETURNING 304");
                }
                return notmodified(abdera, request, "No Entry was found modified since " + request.getIfModifiedSince());
            } else {
                Document<Entry> doc = entry.getDocument();
                BaseResponseContext rc = new BaseResponseContext<Document<Entry>>(doc);
                try {
                    rc.setEntityTag(new EntityTag(entry.getId().toString()));
                } catch (IRISyntaxException e) {
                    throw new BadRequestException(e);
                }
                return rc;
            }
        } catch (Throwable e) {
            return handleTopLevelException(e, abdera, request);
        }
        finally {
            if (perflog != null) {
                perflog.log("GET.entry", request.getUri().getPath(), stopWatch);
            }
        }
    }

    /**
     * Called for a HTTP POST to a FEED URL
     * <p/>
     * The Atom protocol specification defines an XML format known as a entry document.
     * This method accepts AND returns that document, responding to an HTTP POST.  The URL defines precisely
     * which Atom entry to create (i.e which workspace, collection, and entry).
     * @param request The Abdera RequestContext
     * @return response The Abdera ResponseContext, which will contain the appropriate HTTP status code,
     * as detailed above. (201 indicates a successful creation)
     */
    public ResponseContext createEntry(RequestContext request) {
        StopWatch stopWatch = new AutomaticStopWatch();
        if (logger.isInfoEnabled()) {
            logger.info("POST Entry:: [ " + request.getUri() + " ]");
        }
        Abdera abdera = request.getServiceContext().getAbdera();
        try {
            return handleSingleEntry(request, abdera);
        } finally {
            if (perflog != null) {
                perflog.log("POST.entry", request.getUri().getPath(), stopWatch);
            }
        }
    }

    /**
     * Called for a HTTP DELETE to an ENTRY URL.
     * <p/>
     * The URL defines precisely which Atom entry to delete. The semantics of what actually happens when
     * you delete an entry are, of course, undefined. In the default version of AtomServer (i.e. using
     * the DBBasedAtomService), we do not destroy the actual entry (i.e. remove the corresponding row from
     * the database). Instead we simply mark it as deleted, so that Feed consumers can be alerted to the entries 
     * new state.
     * @param request The Abdera RequestContext
     * @return response The Abdera ResponseContext, which will contain the appropriate HTTP status code,
     * as detailed above.
     */
    public ResponseContext deleteEntry(RequestContext request) {
        StopWatch stopWatch = new AutomaticStopWatch();
        if (logger.isInfoEnabled()) {
            logger.info("DELETE Entry:: [ " + request.getUri() + " ]");
        }
        Abdera abdera = request.getServiceContext().getAbdera();
        try {

            EntryTarget entryTarget = atomService.getURIHandler().getEntryTarget(request, false);
            Entry entry = atomService.getAtomWorkspace(entryTarget.getWorkspace())
                             .getAtomCollection(entryTarget.getCollection()).deleteEntry(request);

            AbstractResponseContext rc = null;
            if (entry == null) {
                rc = new EmptyResponseContext(200);
            } else {
                Document<Entry> doc = entry.getDocument();
                rc = new BaseResponseContext<Document<Entry>>(doc);
                rc.setEntityTag(new EntityTag(entry.getId().toString()));
            }
            return rc;

        } catch (Throwable e) {
            return handleTopLevelException(e, abdera, request);
        }
        finally {
            if (perflog != null) {
                perflog.log("DELETE.entry", request.getUri().getPath(), stopWatch);
            }
        }
    }

    /**
     * Called for a HTTP PUT to an ENTRY URL.
     * <p/>
     * The Atom protocol specification defines an XML format known as a entry document.
     * This method accepts AND returns that document, responding to an HTTP PUT.  The URL defines precisely
     * which Atom workspace and collection to create. This method is used for EITHER document creation or update.
     * <p/>
     * The essential difference between using this method (PUT) and using the POST is whether the document is
     * truly owned by the AtomServer or not. Often a document is actually created by some other system and,
     * in turn, labeled (i.e. given an Id) by that system. So the default version of AtomServer (i.e. using
     * the DBBasedAtomService) allows you to PUT that entry as you would PUT any entry, and then determines under
     * the covers whether this is a create or modify operation.
     * @param request The Abdera RequestContext
     * @return response The Abdera ResponseContext, which will contain the appropriate HTTP status code,
     * as detailed above. Will return a status code of 201 if the document was successfully created,
     * and otherwise, 200 for success.
     */
    public ResponseContext updateEntry(RequestContext request) {
        StopWatch stopWatch = new AutomaticStopWatch();
        if (logger.isInfoEnabled()) {
            logger.info("PUT Entry:: [ " + request.getUri() + " ]");
        }
        String relativeUri =
                atomService.getServiceBaseUri() == null ? request.getUri().toString() :
                request.getUri().toString().replace(atomService.getServiceBaseUri(), "").replaceAll("\\/+", "/");

        Abdera abdera = request.getServiceContext().getAbdera();

        Matcher matcher = BATCH_ENTRY_PATTERN.matcher(relativeUri);
        try {
            if (matcher.matches()) {
                return handleBatch(request, abdera);
            } else {
                return handleSingleEntry(request, abdera);
            }
        } finally {
            if (perflog != null) {
                perflog.log("PUT.entry", request.getUri().getPath(), stopWatch);
            }
        }
    }

    /**
     * Returns the default page size
     * @return  the default page size
     */
    public int getDefaultPageSize() {
        return DEFAULT_PAGE_SIZE;
    }

    //====================================
    //  Methods Not Currently Implemented
    //====================================
    /**
     * Called for a HTTP POST to an ENTRY URL.
     * NOT CURRENTLY IMPLEMENTED
     * <p/>
     * @param request The Abdera RequestContext
     * @return response The Abdera ResponseContext, which will contain the appropriate HTTP status code,
     * as detailed above.
     */
    public ResponseContext entryPost(RequestContext request) {
        return notImplemented(request, "POST to Entry");
    }

    /**
     * Called for a HTTP GET for a Categories document.
     * NOT CURRENTLY IMPLEMENTED
      * <p/>
     * @param request The Abdera RequestContext
     * @return response The Abdera ResponseContext, which will contain the appropriate HTTP status code,
     * as detailed above.
     */
    public ResponseContext getCategories(RequestContext request) {
        return notImplemented(request, "GET categories");
    }

    /**
     * Called for a HTTP PUT to an ENTRY URL, where the Content type is some media.
     * NOT CURRENTLY IMPLEMENTED
      * <p/>
     * @param request The Abdera RequestContext
     * @return response The Abdera ResponseContext, which will contain the appropriate HTTP status code,
     * as detailed above.
     */
    public ResponseContext updateMedia(RequestContext request) {
        return notImplemented(request, "PUT media");
    }

    /**
     * Called for a HTTP DELETE to an ENTRY URL, where the Content type is some media.
     * NOT CURRENTLY IMPLEMENTED
      * <p/>
     * @param request The Abdera RequestContext
     * @return response The Abdera ResponseContext, which will contain the appropriate HTTP status code,
     * as detailed above.
     */
    public ResponseContext deleteMedia(RequestContext request) {
        return notImplemented(request, "DELETE media");
    }


    /**
     * Called for a HTTP GET to an ENTRY URL, where the Content type is some media.
     * NOT CURRENTLY IMPLEMENTED
      * <p/>
     * @param request The Abdera RequestContext
     * @return response The Abdera ResponseContext, which will contain the appropriate HTTP status code,
     * as detailed above.
     */
    public ResponseContext getMedia(RequestContext request) {
        return notImplemented(request, "GET media");
    }

    /**
     * Called for a HTTP POST to an ENTRY URL,, where the Content type is some media.
     * NOT CURRENTLY IMPLEMENTED
      * <p/>
     * @param request The Abdera RequestContext
     * @return response The Abdera ResponseContext, which will contain the appropriate HTTP status code,
     * as detailed above.
     */
    public ResponseContext mediaPost(RequestContext request) {
        return notImplemented(request, "POST media");
    }

    private ResponseContext handleSingleEntry(RequestContext request, Abdera abdera) {
        try {
            EntryTarget entryTarget = atomService.getURIHandler().getEntryTarget(request, false);
            UpdateCreateOrDeleteEntry.CreateOrUpdateEntry uEntry =
                    atomService.getAtomWorkspace(entryTarget.getWorkspace())
                        .getAtomCollection(entryTarget.getCollection()).updateEntry(request);
            Entry entry = uEntry.getEntry();

            Document<Entry> doc = entry.getDocument();
            AbstractResponseContext rc = new BaseResponseContext<Document<Entry>>(doc);
            rc.setEntityTag(new EntityTag(entry.getId().toString()));

            if (uEntry.isNewlyCreated()) {
                rc.setStatus(201);
            }

            rc.setLocation( entry.getEditLinkResolvedHref().toString() );
            
            return rc;
        } catch (Throwable e) {
            return handleTopLevelException(e, abdera, request);
        }
    }

    private ResponseContext handleBatch(RequestContext request, Abdera abdera) {

        final Factory factory = getFactory(abdera);      

        try {
            EntryTarget entryTarget = atomService.getURIHandler().getEntryTarget(request, false);
            java.util.Collection<UpdateCreateOrDeleteEntry> updateEntries =
                    atomService.getAtomWorkspace(entryTarget.getWorkspace())
                        .getAtomCollection(entryTarget.getCollection()).updateEntries(request);

            Document<Feed> doc = factory.newFeed().getDocument();

            int inserts = 0, updates = 0, deletes = 0, errors = 0;
            for (UpdateCreateOrDeleteEntry updateOrDeleteEntry : updateEntries) {
                Entry entry = updateOrDeleteEntry.getEntry();
           
                Operation operation = factory.newExtensionElement(AtomServerConstants.OPERATION);
                entry.addExtension(operation);

                operation.setType(updateOrDeleteEntry.isDeleted() ? "delete" :
                                  updateOrDeleteEntry.isNewlyCreated() ? "insert" : "update");
                if (updateOrDeleteEntry.getException() == null) {

                    Status status = factory.newExtensionElement(AtomServerConstants.STATUS);
                    entry.addExtension(status);

                    status.setCode(updateOrDeleteEntry.isNewlyCreated() ? "201" : "200");
                    status.setReason(updateOrDeleteEntry.isNewlyCreated() ? "CREATED" : "OK");

                    if (updateOrDeleteEntry.isDeleted()) {
                        deletes++;
                    } else if (updateOrDeleteEntry.isNewlyCreated()) {
                        inserts++;
                    } else {
                        updates++;
                    }
                } else {
                    handleBatchItemException(updateOrDeleteEntry.getException(), abdera, request, entry);
                    errors++;
                }
                doc.getRoot().addEntry(entry);
            }

            Results results = factory.newExtensionElement(AtomServerConstants.RESULTS);
            doc.getRoot().addExtension(results);
            results.setInserts(inserts);
            results.setUpdates(updates);
            results.setDeletes(deletes);
            results.setErrors(errors);

            BaseResponseContext<Document<Feed>> responseContext = new BaseResponseContext<Document<Feed>>(doc);
            responseContext.setStatus(200);
            responseContext.setStatusText("BATCH OK");
            return responseContext;
        } catch (Throwable e) {
            return handleTopLevelException(e, abdera, request);
        }
    }


    //====================================
    //  Special HTTP Error Codes/Returns 
    //====================================
    /** Return a 403 FORBIDDEN error when a method has not yet been implemented
     * <p/>
     * @param request The Abdera RequestContext
     * @param details Details concerning the error
     * @return response The Abdera ResponseContext, which will contain the 403 HTTP status code.
     */
    private ResponseContext notImplemented(RequestContext request, String details) {
        Abdera abdera = request.getServiceContext().getAbdera();
        String msg = "NOT IMPLEMENTED :: (" + details + ") :: " + request.getUri();
        logger.error(msg);
        return forbidden(abdera, request, msg);
    }

    /** Return a 422 BAD CONTENT error
     * <p/>
     * @param abdera The Abdera Instance
     * @param reason Details concerning the error
     * @return response The Abdera ResponseContext, which will contain the 422 HTTP status code.
     */
    protected ResponseContext badcontent(Abdera abdera, String reason) {
        return returnBase(createErrorDocument(abdera, 422, reason, null), 422, null);
    }

    /** Return a 409 CONFLICT error when an optimistic concurrency error occurs
     * <p/>
     * @param abdera The Abdera Instance
     * @param reason Details concerning the error
     * @param editURI The edit URI that user should use to access the Entry.
     * @return response The Abdera ResponseContext, which will contain the 409 HTTP status code.
     */
    protected ResponseContext optimisticConcurrencyError(Abdera abdera, String reason, String editURI) {
        return returnBase(createOptimisticConcurrencyErrorDocument(abdera, reason, editURI), 409, null);
    }


    /**  Creates the a 409 Conflict error, when an optimistic concurrency error occurs,
     * which looks something like this;  
     * <pre>
     *  <error xmlns="http://incubator.apache.org/abdera">
     *    <code>409</code>
     *    <message>Optimisitic Concurrency Error:: /atomserver/v1/widgets/acme/12345.en.xml/2</message>
     *    <link xmlns="http://www.w3.org/2005/Atom" href="/atomserver/v1/widgets/acme/12345.en.xml/1" rel="edit" />
     *  </error>
     * </pre>
     * <p/>
     * @param abdera The Abdera Instance
     * @param message Details concerning the error
     * @param editURI The edit URI that user should use to access the Entry.
     * @return error The Abdera Error, as shown above.
     */
    protected Error createOptimisticConcurrencyErrorDocument(Abdera abdera, String message, String editURI) {

        Error error = Error.create( abdera, 409, ((message != null) ? message : "") );

        // create an Atom edit link, note that we must add it as an Extension
        //  so that we can access it later (in JUnits, etc)
        Factory factory = AtomServer.getFactory(abdera);
        Link link = factory.newLink();
        link.setHref(editURI);
        link.setRel("edit");

        if (logger.isDebugEnabled()) {
            logger.debug("link = " + link);
        }

        error.addExtension(link);
        return error;
    }

    /** Return a 301 MOVED PERMANANTLY error
     * <p/>
     * @param abdera The Abdera Instance
     * @param reason Details concerning the error
     * @param altURI The edit URI that user should use to access the Entry.
     * @return response The Abdera ResponseContext, which will contain the 301 HTTP status code.
     */
    protected ResponseContext movedPermanently(Abdera abdera, String reason, String altURI) {
        return returnBase(createMovedPermanentlyErrorDocument(abdera, reason, altURI), 301, null);
    }


    /**  Creates the a 301 Moved Permanently error, which looks something like this;  
     * <pre>
     *  <error xmlns="http://incubator.apache.org/abdera">
     *    <code>301</code>
     *    <message>Moved Permanently Error:: /foobar/v1/tags:widgets/acme</message>
     *    <link xmlns="http://www.w3.org/2005/Atom" href="/foobar/v1/widgets/acme" rel="alternate" />
     *  </error>
     * </pre>
     * @param abdera The Abdera Instance
     * @param message Details concerning the error
     * @param altURI The URI that user should use to access the Entry.
     * @return error The Abdera Error, as shown above.
     */
    protected Error createMovedPermanentlyErrorDocument(Abdera abdera, String message, String altURI) {
        Error error = Error.create( abdera, 301, ((message != null) ? message : "") );

        // create an Atom alternative link, note that we must add it as an Extension
        //  so that we can access it later (in JUnits, etc)
        Factory factory = AtomServer.getFactory(abdera);
        
        Link link = factory.newLink();
        link.setHref(altURI);
        link.setRel("alternate");

        if (logger.isDebugEnabled()) {
            logger.debug("link = " + link);
        }

        error.addExtension(link);
        return error;
    }

    //====================================
    // Handle top-level exceptions
    //====================================

    private interface TopLevelExceptionHandler<T extends Throwable> {
        ResponseContext handle(T exception, Abdera abdera, RequestContext request);
    }

    private final Map<Class<? extends Throwable>, TopLevelExceptionHandler> exceptionHandlers =
            new HashMap<Class<? extends Throwable>, TopLevelExceptionHandler>();

    {
        TopLevelExceptionHandler<EntryNotFoundException> entryNotFoundHandler =
                new TopLevelExceptionHandler<EntryNotFoundException>() {
                    public ResponseContext handle(EntryNotFoundException exception, Abdera abdera, RequestContext request) {
                        String message = MessageFormat.format( "Unknown Entry:: {0}\nReason:: {1}", request.getUri(), exception.getMessage());
                        ResponseContext response = unknown(abdera, request, message);
                        if (response instanceof BaseResponseContext) {
                            ((BaseResponseContext) response).setStatusText(message);
                        }
                        return response;
                    }
                };
        exceptionHandlers.put(EntryNotFoundException.class, entryNotFoundHandler);

        TopLevelExceptionHandler<BadRequestException> badRequestHandler =
                new TopLevelExceptionHandler<BadRequestException>() {
                    public ResponseContext handle(BadRequestException exception, Abdera abdera, RequestContext request) {
                        String message = MessageFormat.format( "Bad Request:: {0}\nReason:: {1}", request.getUri(), exception.getMessage());
                        ResponseContext response = badrequest(abdera, request, message);
                        if (response instanceof BaseResponseContext) {
                            ((BaseResponseContext) response).setStatusText(message);
                        }
                        return response;
                    }
                };
        exceptionHandlers.put(BadRequestException.class, badRequestHandler);
        exceptionHandlers.put(MalformedURLException.class, badRequestHandler);
        exceptionHandlers.put(URISyntaxException.class, badRequestHandler);

        TopLevelExceptionHandler<BadContentException> badContentHandler =
                new TopLevelExceptionHandler<BadContentException>() {
                    public ResponseContext handle(BadContentException exception, Abdera abdera, RequestContext request) {
                        String message = MessageFormat.format( "Bad Content:: {0}\nReason:: {1}", request.getUri(), exception.getMessage());
                        ResponseContext response = badcontent(abdera, message);
                        if (response instanceof BaseResponseContext) {
                            ((BaseResponseContext) response).setStatusText(message);
                        }
                        return response;
                    }
                };
        exceptionHandlers.put(BadContentException.class, badContentHandler);

        TopLevelExceptionHandler<MovedPermanentlyException> movedPermanentlyHandler =
                new TopLevelExceptionHandler<MovedPermanentlyException>() {
                    public ResponseContext handle(MovedPermanentlyException exception, Abdera abdera, RequestContext request) {
                        String message = MessageFormat.format( "Moved Permanently:: {0}\nReason:: {1}", request.getUri(), exception.getMessage());
                        ResponseContext response = movedPermanently(abdera, message, exception.getAlternateURI() );
                        if (response instanceof BaseResponseContext) {
                            ((BaseResponseContext) response).setStatusText( message );
                            ((BaseResponseContext) response).setLocation( exception.getAlternateURI() );
                        }
                        return response;
                    }
                };
        exceptionHandlers.put(MovedPermanentlyException.class, movedPermanentlyHandler);

        TopLevelExceptionHandler<OptimisticConcurrencyException> optimisticConcurrencyHandler =
                new TopLevelExceptionHandler<OptimisticConcurrencyException>() {
                    public ResponseContext handle(OptimisticConcurrencyException exception, Abdera abdera, RequestContext request) {
                        String message = MessageFormat.format("Optimisitic Concurrency Error:: {0}", request.getUri());
                        ResponseContext response = optimisticConcurrencyError(abdera, message, exception.getEditURI());
                        if (response instanceof BaseResponseContext) {
                            ((BaseResponseContext) response).setStatusText(message);
                        }
                        return response;
                    }
                };
        exceptionHandlers.put(OptimisticConcurrencyException.class, optimisticConcurrencyHandler);
    }

    /** Handle a top-level Exception when using batched operations. This method delegates to handleTopLevelException.
     * @param e  The Throwable that got you here.
     * @param abdera The Abdera Instance
     * @param request The Abdera RequestContext
     * @param entry The Entry that threw the Exception
    */
    protected void handleBatchItemException(Throwable e, Abdera abdera, RequestContext request, Entry entry) {
        final ResponseContext responseContext = handleTopLevelException(e, abdera, request);
        Status status = getFactory(abdera).newExtensionElement(AtomServerConstants.STATUS);
        entry.addExtension(status);
        status.setCode(String.valueOf(responseContext.getStatus()));
        status.setReason(responseContext.getStatusText());
    }

    /** Handle a top-level Exception. This is the funnel point for Exception handling in the AtomServer.
     * It allows a single entry point for the AtomServer methods, which provides maximal code reuse,
     * and insures that, for example all 500 errors get logged specially.
     * @param e  The Throwable that got you here.
     * @param abdera The Abdera Instance
     * @param request The Abdera RequestContext
    */
    protected ResponseContext handleTopLevelException(Throwable e, Abdera abdera, RequestContext request) {
        TopLevelExceptionHandler exceptionHandler = exceptionHandlers.get(e.getClass());
        String message = MessageFormat.format( "Unknown Error:: {0}\nReason:: {1}", request.getUri(), e.getMessage());

        ResponseContext response = null;
        if (exceptionHandler == null) {
            response = servererror(abdera, request, message, e);
            if (response instanceof BaseResponseContext) {
                ((BaseResponseContext) response).setStatusText(message);
            }

            log500Error( e, abdera, request ); 

            // These Exceptions have probably been thrown all the way to here
            //  Thus, it has most likely NOT been logged to the "standard log" below
            logger.error( message, e );

        } else {
            if ( e instanceof EntryNotFoundException ) 
                logger.warn( ("Error for [" + request.getUri() + "] Cause: " + e.getMessage()) );
            else 
                logger.error( ("Error for [" + request.getUri() + "] Cause: " + e.getMessage()), e );
            response = exceptionHandler.handle(e, abdera, request);
        }
        return response;
    }

    static private final int MAX_REQ_BODY_DUMP = 500;

    private void log500Error( Throwable e, Abdera abdera, RequestContext request ) {
        if ( errlog != null ) { 
            Log http500log = errlog.getLog();
            if ( http500log.isInfoEnabled() ) { 
                try { 
                    http500log.info( "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" );
                    http500log.info( "==> 500 ERROR occurred for {" + request.getUri() +
                                     "} Type:: " + e.getClass().getName() + " Reason:: " +  e.getMessage() );
                    http500log.error( "500 Error:: ", e );
                    http500log.info( "METHOD:: " + request.getMethod() );
                    if ( request.getPrincipal() != null ) 
                        http500log.info( "PRINCIPAL:: " + request.getPrincipal().getName() );
                    http500log.info( "HEADERS:: " );
                    String[] headerNames = request.getHeaderNames();
                    if ( headerNames != null ) {
                        for( int ii = 0; ii < headerNames.length; ii++ ) { 
                            http500log.info( "Header(" + headerNames[ii] + ")= " + request.getHeader( headerNames[ii] ));
                        }
                    }
                    http500log.info( "PARAMETERS:: " );
                    String[] paramNames = request.getParameterNames();
                    if ( paramNames != null ) {
                        for( int ii = 0; ii < paramNames.length; ii++ ) { 
                            http500log.info( "Parameter(" + paramNames[ii] + ")= " + request.getParameter( paramNames[ii] ));
                        }
                    }
                    if ( request instanceof HttpServletRequestContext ) { 
                        HttpServletRequestContext httpRequest = (HttpServletRequestContext)request; 
                        javax.servlet.http.HttpServletRequest servletRequest = httpRequest.getRequest();
                        if ( servletRequest != null ) { 
                            http500log.info("QUERY STRING::" + servletRequest.getQueryString() );
                            http500log.info("AUTH TYPE:: " + servletRequest.getAuthType() );
                            http500log.info("REMOTE USER:: "  + servletRequest.getRemoteUser() );
                            http500log.info("REMOTE ADDR:: " + servletRequest.getRemoteAddr() );
                            http500log.info("REMOTE HOST:: " + servletRequest.getRemoteHost() );
                        }
                    }
                    http500log.info( "BODY:: " );
                    if ( request.getDocument() != null ) { 
                        java.io.StringWriter stringWriter = new java.io.StringWriter();
                        request.getDocument().writeTo( abdera.getWriterFactory().getWriter("PrettyXML"), stringWriter );

                        //http500log.info( "\n" + stringWriter.toString() );
                        String requestString = stringWriter.toString();
                        if ( requestString != null && requestString.length() > MAX_REQ_BODY_DUMP ) {
                            requestString = requestString.substring(0, (MAX_REQ_BODY_DUMP-1) );
                        }

                        http500log.info( "\n" + requestString );
                    }
                } catch ( Exception ee ) {}
            }
        }
    }
}
