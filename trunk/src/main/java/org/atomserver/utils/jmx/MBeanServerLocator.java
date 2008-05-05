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


package org.atomserver.utils.jmx;


import javax.management.MBeanServer;

import java.lang.management.ManagementFactory;

import org.springframework.jndi.JndiObjectLocator; 

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**    
<pre>            
    <bean id="resinMBeanServer"
          class="org.springframework.jndi.JndiObjectFactoryBean">
        <property name="jndiName" value="java:comp/env/jmx/MBeanServer"/>
    </bean>
</pre>
*/

public class MBeanServerLocator extends JndiObjectLocator {

    static private Log log = LogFactory.getLog( MBeanServerLocator.class );

    static private final String RESIN_JNDI_NAME= "java:comp/env/jmx/MBeanServer"; 

    private String containerType = null; 

    public MBeanServerLocator() {
        setJndiName( RESIN_JNDI_NAME );
    }

    public void setContainerType( String containerType ) {
        this.containerType = containerType;
    }

    public MBeanServer locateMBeanServer() {
        MBeanServer mbeanServer = null; 
        if ( containerType != null && containerType.equals( "Resin" ) ) {
            try { 
                mbeanServer = ( MBeanServer )lookup(); 
            } catch ( Exception ee ) { 
                log.error( "Exception (" + ee.getMessage() +") while attempting to lookup" + 
                           " the Resin JNDI MBeanServer (" + RESIN_JNDI_NAME + ")", ee );
            }
        }
        // Just fallback to the Java 5 Platform MBeanServer, which is always present
        if ( mbeanServer == null ) { 
            mbeanServer = ManagementFactory.getPlatformMBeanServer();
        }       
        return mbeanServer;
    }
    
 	
}