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


package org.atomserver.blogs;

import org.apache.abdera.Abdera;
import org.apache.abdera.util.Constants;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.i18n.iri.IRISyntaxException;
import org.apache.abdera.model.*;
import org.apache.abdera.parser.ParseException;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.*;
import org.atomserver.core.CollectionOptions;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.exceptions.EntryNotFoundException;

import java.io.*;
import java.util.Date;

/**
 * This code was inspired from original work by Ugo Cei
 */
public class FileBasedBlogCollection implements AtomCollection {
    private Log log = LogFactory.getLog(FileBasedBlogCollection.class);

    private AtomWorkspace parentWorkspace;
    private String name = null;
    private CollectionOptions options = new CollectionOptions();

    public FileBasedBlogCollection( AtomWorkspace parent, String name ) {
        this.name = name ;
        this.parentWorkspace = parent;
    }

    public AtomWorkspace getParentAtomWorkspace() {
        return parentWorkspace;
    }

    public File getRootDir() {
       return ((FileBasedBlogWorkspace)parentWorkspace).getRootDir();
    }

    public String getName() {
        return name;
    }

    public java.util.Collection<Category> listCategories( RequestContext request, String workspace, String collection ) {
        return null;
    }

    private Entry createEntry( RequestContext request ) throws AtomServerException {
        IRI iri = request.getUri();
        String slug = request.getSlug();

        Entry entry = null;
        try {
            Document<Entry> document = request.getDocument();
            entry = document.getRoot();
        } catch (IOException e) {
            throw new AtomServerException(e);
        }

        try {
            entry.setId("tag:blog.example.com,2006:" + slug);
        } catch (IRISyntaxException e) {
            throw new AtomServerException(e);
        }
        entry.setUpdated(new Date());
        String pathInfo[] = iri.toString().split("/");
        String dir = pathInfo[pathInfo.length - 1];
        File file = new File(new File(getRootDir(), dir), slug);

        entry.addLink(request.getResolvedUri() + slug, "edit" );

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            entry.writeTo(out);
        } catch (FileNotFoundException e) {
            throw new AtomServerException("File not found: " + file, e);
        } catch (IOException e) {
            throw new AtomServerException(e);
        } finally {
            if (out != null) try {
                out.close();
            } catch (IOException ignored) {}
        }
        return entry;
    }

    public UpdateCreateOrDeleteEntry.CreateOrUpdateEntry updateEntry(RequestContext request) throws AtomServerException {
        IRI iri = request.getUri();

        Entry entry = null;
        boolean isCreated = false;
        if ("POST".equals(request.getMethod())) {
            entry = createEntry(request);
            isCreated = true;
        } else {

            try {
                Document<Entry> document = request.getDocument();
                entry = document.getRoot();
            } catch (IOException e) {
                throw new AtomServerException(e);
            }

            entry.setUpdated(new Date());

            String pathInfo[] = iri.toString().split("/");
            String dir = pathInfo[pathInfo.length - 2];
            String slug = pathInfo[pathInfo.length - 1];

            entry.addLink(request.getResolvedUri().toString(), "edit" );

            File file = new File(new File(getRootDir(), dir), slug);
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(file);
                entry.writeTo(out);
            } catch (FileNotFoundException e) {
                throw new AtomServerException("File not found: " + file, e);
            } catch (IOException e) {
                throw new AtomServerException(e);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ignored) {}
                }
            }
        }

        log.debug( "isCreated = " + isCreated );
        UpdateCreateOrDeleteEntry.CreateOrUpdateEntry uEntry =
                new UpdateCreateOrDeleteEntry.CreateOrUpdateEntry(entry, isCreated);
        return uEntry;
    }

    public Entry getEntry( RequestContext request ) throws AtomServerException {
        Abdera abdera = request.getServiceContext().getAbdera();
        IRI iri = request.getUri();

        String pathInfo[] = iri.toString().split("/");
        String dir = pathInfo[pathInfo.length - 2];
        String slug = pathInfo[pathInfo.length - 1];
        File file = new File(new File(getRootDir(), dir), slug);
        try {
            Document<Entry> doc =  abdera.getParser().parse(new FileInputStream(file));
            Entry entry = doc.getRoot();
            if (entry.getEditLink() == null) {
                entry.addLink(iri.toString(), "edit");
            }
            return entry;
        } catch (ParseException e) {
            throw new AtomServerException("Can't parse " + file);
        } catch (FileNotFoundException e) {
            throw new EntryNotFoundException("File not found: " + file);
        } catch (IRISyntaxException e) {
            throw new AtomServerException(e);
        }
    }

    public Entry deleteEntry( RequestContext request ) throws AtomServerException {
        IRI iri = request.getUri();

        String pathInfo[] = iri.toString().split("/");
        String dir = pathInfo[pathInfo.length - 2];
        String slug = pathInfo[pathInfo.length - 1];
        File file = new File(new File(getRootDir(), dir), slug);
        file.delete();

        return null;
    }


    public Feed getEntries( RequestContext request) {
        Abdera abdera = request.getServiceContext().getAbdera();
        IRI iri = request.getUri();

        String pathInfo[] = iri.toString().split("/");
        String dir = pathInfo[pathInfo.length - 1];
        File[] files = new File(getRootDir(), dir).listFiles();
        Feed feed = abdera.getFactory().newFeed();
        long lastModified = 0;
        for (int i = 0 ; i < files.length ; ++i) {
            if (files[i].isFile() && ! files[i].isHidden()) {
                try {
                    Document<Entry> doc = abdera.getParser().parse(new FileInputStream(files[i]));
                    Entry entry = doc.getRoot();
                    if (entry.getEditLink() == null) {
                        entry.addLink(iri.toString() + '/' + files[i].getName(), "edit");
                    }
                    feed.addEntry(entry);
                    if (files[i].lastModified() > lastModified) {
                        lastModified = files[i].lastModified();
                    }
                } catch (ParseException e) {
                    throw new AtomServerException("Can't parse " + files[i]);
                } catch (FileNotFoundException e) {
                    throw new AtomServerException("File not found: " + files[i], e);
                } catch (IRISyntaxException e) {
                    throw new AtomServerException(e);
                }
            }
        }
        try {
            feed.setTitle(dir);
            feed.setUpdated(new Date(lastModified));
            feed.setId("tag:blog.example.com,2006:" + dir);
        } catch (IRISyntaxException e) {
            throw new AtomServerException(e);
        }
        return feed;
    }

    public java.util.Collection<UpdateCreateOrDeleteEntry> updateEntries(RequestContext request) throws AtomServerException {
         return null;
     }

     public ContentStorage getContentStorage() {
         return null;
     }

     public ContentValidator getContentValidator() {
         return null;
     }

     public CategoriesHandler getCategoriesHandler() {
         return null;
     }

     public EntryAutoTagger getAutoTagger() {
         return null;
     }

     public CollectionOptions getOptions() {
         return options;
     }

     public void setOptions(CollectionOptions options) {
     }

     public java.util.Collection<Category> listCategories(RequestContext request) throws AtomServerException {
         return null;
     }

    public void ensureCollectionExists(String collection) {
    }
}
