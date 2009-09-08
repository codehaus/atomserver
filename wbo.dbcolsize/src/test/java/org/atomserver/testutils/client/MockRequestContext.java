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

package org.atomserver.testutils.client;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.abdera.model.Entry;
import org.apache.abdera.protocol.server.ServiceContext;
import org.apache.abdera.protocol.server.impl.HttpServletRequestContext;
import org.apache.abdera.i18n.iri.IRI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;

//import org.osaf.cosmo.atom.AtomConstants;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

/**
 * Mock implementation of {@link org.apache.abdera.protocol.server.RequestContext}.
 */
public class MockRequestContext extends HttpServletRequestContext {

    private static final Log log = LogFactory.getLog( MockRequestContext.class );

    private static IRI baseURI;

    /**
     * Create a Mock ServiceRequest. <br/>
     * NOTE: we ASSUME that the first field of teh path is the Servlet Context,
     * and use this  field to construct the BaseURI.
     * @param context  The Abdera ServiceContext
     * @param method   The HTTP Method
     * @param uri      The full URI
     */
    public MockRequestContext( ServiceContext context,
                               String method,
                               String uri ) {
        super(context, createRequest(method, uri));
    }

    private static MockHttpServletRequest createRequest(String method,
                                                        String uri) {
        MockServletContext ctx = new MockServletContext();
        createBaseURI( uri );
        return new MockHttpServletRequest(ctx, method, uri);
    }

    private static void createBaseURI( String uri ) {
        IRI iri = new IRI(uri);

        String sbaseURI = "http://" + iri.getHost();
        sbaseURI += ( iri.getPort() != -1 ) ? iri.getPort() : "" ;

        String[] pathFields = StringUtils.split( iri.getRawPath(), "/" );
        String servletContext = pathFields[0];
        sbaseURI += "/" + servletContext;
        log.debug( "SETTING MockRequestContext baseURI= " + sbaseURI );

        baseURI = new IRI( sbaseURI );
    }

    public MockHttpServletRequest getMockRequest() {
        return (MockHttpServletRequest) getRequest();
    }

    public IRI getBaseUri() {
        return baseURI;
    }

    public void setContent(Entry entry)
        throws IOException {
        String xml = context.getAbdera().getWriter().write(entry).toString();
        getMockRequest().setContent(xml.getBytes());

        // FIXME
        //getMockRequest().setContentType(ATOM_MEDIA_TYPE);
        //getMockRequest().addHeader("Content-Type", ATOM_MEDIA_TYPE);

        getMockRequest().addHeader("Content-Length", xml.getBytes().length);
    }

    public void setPropertiesAsEntry(Properties props)
        throws IOException {
        Entry entry = context.getAbdera().getFactory().newEntry();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        props.store(out, null);
        entry.setContent(out.toString());
        setContent(entry);
    }

    public void setPropertiesAsText(Properties props)
        throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        props.store(out, null);
        setContentAsText(out.toString());
    }

    public void setContentAsEntry(String content)
        throws IOException {
        Entry entry = context.getAbdera().getFactory().newEntry();
        entry.setContent(content);
        setContent(entry);
    }

    public void setXhtmlContentAsEntry(String content)
        throws IOException {
        Entry entry = context.getAbdera().getFactory().newEntry();
        entry.setContentAsXhtml(content);
        setContent(entry);
    }

    public void setContentAsText(String content)
        throws IOException {
        getMockRequest().setContent(content.getBytes());
        getMockRequest().setContentType("text/plain");
        getMockRequest().addHeader("Content-Type", "text/plain");
        getMockRequest().addHeader("Content-Length",
                                   content.getBytes().length);
    }

    public void setContentAsXhtml(String content)
        throws IOException {
        if (content != null)
            getMockRequest().setContent(content.getBytes());
        getMockRequest().setContentType("application/xhtml+xml");
        getMockRequest().addHeader("Content-Type", "application/xhtml+xml");
        getMockRequest().addHeader("Content-Length",
                                   content.getBytes().length);
    }
}
