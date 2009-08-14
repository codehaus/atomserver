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


package org.atomserver.utils.acegi;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.acegisecurity.util.FilterChainProxy;

//>>>>
import java.util.Map;
import org.springframework.util.Assert;
import org.acegisecurity.intercept.web.FilterInvocationDefinitionSource;

/** 
 * Extends the standard Acegi FilterChainProxy with the ability to 
 * turn on/off the Acegi Servlet Filters.
 * <p/>
 * This is useful for both JUnits and for Staging Servers, where we often
 * want to disable security.
 * <p/>
 * Of course, the default is "security on".
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class ConfigurableFilterChainProxy extends FilterChainProxy {
       
    static private Log log = LogFactory.getLog(ConfigurableFilterChainProxy.class);
    static private boolean wasWarned = false ;

    private boolean useFilters = true; 


    //>>>>>>>>>
    private String filterChainInUse = null; 
    private Map<String, FilterInvocationDefinitionSource> filtersMap = null;

    public void setFilterInvocationDefinitionSourceMap( Map<String, FilterInvocationDefinitionSource> filtersMap ) {
        this.filtersMap = filtersMap;
    }

    public void setFilterChainInUse( String filterChainInUse ) { 
        this.filterChainInUse = filterChainInUse;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull( filterChainInUse, "filterChainInUse must be specified");
        Assert.notNull( filtersMap, "filterInvocationDefinitionSourceMap must be specified");

        log.info( "Setting Filter Chain= " + filterChainInUse );
        FilterInvocationDefinitionSource filterDefSource = filtersMap.get( filterChainInUse );
        Assert.notNull( filterDefSource, "the filterChainInUse must be specified in the filterInvocationDefinitionSourceMap");
        setFilterInvocationDefinitionSource( filterDefSource );

        super.afterPropertiesSet();
    }
    //<<<<<<


    public void setUseFilters( boolean tOrF ) {
        log.warn( "WARNING: Acegi ServletFilters: useFilters= " + tOrF );
        useFilters = tOrF; 
    }

    public boolean getUseFilters() {
        return useFilters; 
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        if ( useFilters ) {
            super.doFilter( request, response, chain);
        } else {
            if ( ! wasWarned ) { 
                log.warn( "WARNING: NOT USING Acegi ServletFilters" );
                wasWarned = true; 
            }

            chain.doFilter(request, response);
        }
    }
}
