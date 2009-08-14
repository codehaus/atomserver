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

package org.atomserver.utils.alive;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

/** An "alive page" used by the load balancer
 * Requirements:<br>
 * 1. Should be /alive.txt for all apps, unprotected<br>
 * 2. Should be text/plain content type<br>
 * 3. should return a 200 if all is well, with an "OK" pure text response<br>
 * 4. in any other situation, then the content of the body is the error message which should be a short one line message
 *    and the status code is 503 <br>
 * 5. if set to DOWN externally (via JMX), should respond as DOWN. This allows it to be removed from the load balancer,
 *    and its Request queue drained.<br>
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class AliveServlet extends HttpServlet  {

    static private Log log = LogFactory.getLog( AliveServlet.class );
    
    private ApplicationContext appCtx = null;
    private IsAliveHandler isAliveHandler = null;

    /**
     * {@inheritDoc}
     */    
    public void init(ServletConfig config)
            throws ServletException {
        super.init(config);
        loadSpringContext();
    }

    protected void loadSpringContext() {
        if (appCtx == null) {
            ServletConfig config = getServletConfig();

            ServletContext context = config.getServletContext();
            if ( context != null ) {
                 if (log.isDebugEnabled())
                     log.debug("LOADING: WebApplicationContextUtils.getRequiredWebApplicationContext(context))");
                appCtx = WebApplicationContextUtils.getRequiredWebApplicationContext(context);
            } else {
                log.error("COULD NOT LOAD ApplicationContext");
            }
            
            if (log.isTraceEnabled()) 
                log.trace("Application context set:: appCtx= " + appCtx);
            
            isAliveHandler = (IsAliveHandler)( appCtx.getBean("org.atomserver-isAliveHandler"));
        }
    }

    /**
     * {@inheritDoc}
     */    
    public void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        //  Pass all GET request to the the doPost method
        doPost(req,res);  			
    }
       
    /**
     * {@inheritDoc}
     */    
    public void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException  {

        AliveStatus aliveStatus = isAliveHandler.isAlive();
        if (log.isTraceEnabled()) 
            log.trace("aliveStatus= " + aliveStatus);

        // prepare the Response
        res.setContentType("text/plain");

        int statusCode = ( aliveStatus.isOkay() ) ?  200 : 503 ;
        res.setStatus( statusCode );
        
        PrintWriter out = res.getWriter();

        if ( aliveStatus.isError() ) 
            out.println( aliveStatus.getState() + " :: " + aliveStatus.getErrorMessage() );
        else 
            out.println( aliveStatus.getState() );

        out.close();       
    }
}
