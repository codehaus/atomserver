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
import javax.management.ObjectName;

import org.springframework.jmx.export.MBeanExporterListener;

/** The class basically exists so that we can start the MX4J JMX HttpAdaptor
 * which is wired up in the Spring applicationContext file.
 * 
 * Note that we are using the singleton MBeanServer in Resin, such that <b>every</b> 
 * installed webapp is associated with this MBeanServer and thus, registers
 * all of its MBeans there. "Server:name=HttpAdaptor" is the name for our 
 * HttpAdaptor, and is used in all the applicationContext files. 
 * And because of this, we register the "Server:name=HttpAdaptor"
 * in every webapp, since they may be deployed either separately, or all together. Thus, in 
 * some circumstances we will see a messages like these; 
 * 
 * <pre>
 * INFO [2007-05-24 10:41:02][main                    ] : Registering beans for JMX exposure on startup
 * DEBUG[2007-05-24 10:41:02][main                    ] : Located MBean under key [Server:name=HttpAdaptor]: registering with JMX server
 * DEBUG[2007-05-24 10:41:02][main                    ] : Registering MBean [Server:name=HttpAdaptor]
 * DEBUG[2007-05-24 10:41:02][main                    ] : Replacing existing MBean at [Server:name=HttpAdaptor]
 * .......
 *
 * INFO [2007-05-24 11:11:07][resin-destroy           ] : Unregistering JMX-exposed beans on shutdown
 * WARN [2007-05-24 11:11:07][resin-destroy           ] : Could not unregister MBean [Server:name=HttpAdaptor] as said MBean is not registered 
 *                                                        (perhaps already unregistered by an external process)
 * </pre>
 * These messages are harmless.
 * 
 * Note that this class does protect us from actually starting or stopping the HttpAdaptor more than once
 *
 * The JMX HttpAdaptor is always started as; HOSTNAME:PORT (e.g. com.foobar:50505). Where the port number (default=50505) is 
 * set in ctrl.sh. HOSTNAME and PORT are passed in as a System Property in ctrl.sh 
 * 
 * The basics of this code was taken from here ::
 *  http://blogger.xs4all.nl/kuip/archive/2006/01/20/75092.aspx
 */
public class HttpAdaptorMgr implements MBeanExporterListener {

    /** Because we want the same HttpAdaptor for all webapps 
     *   loaded into a given Resin, then we should only start this up one time
     */
    private static boolean isStarted = false; 
    private static final Object lock = new Object();

    private void start( ObjectName objectName ) {
        synchronized ( lock ) {
            if ( ! isStarted ) {
                try {
                    mbeanServer.invoke(objectName, "start", null, null);
                } catch (Exception e) {
                    System.err.println("Can't start HttpAdaptor: " + e);
                }
                isStarted = true;
            }
        }
    }
    
    private void stop( ObjectName objectName ) {
        synchronized ( lock ) {
            if ( isStarted ) {
                try {
                    mbeanServer.invoke(objectName, "stop", null, null);
                } catch (Exception e) {
                    System.err.println("Can't stop HttpAdaptor: " + e);
                }
                isStarted = false;
            }
        }
    }
    
    private MBeanServer mbeanServer;
    
    /** Note: this name <b>must</b> match that specified in applicationContext.xml
     */
    private String adaptorName = "Server:name=HttpAdaptor";
    
    /** When the specific MBean named "adapterName"
     *   is registered, it triggers a "start" for our MX4J HttpAdapter
     */
    public void mbeanRegistered(ObjectName objectName) {
        if ( adaptorName.equals( objectName.getCanonicalName() ) ) {
            start( objectName );
        }
    }
    
    public void mbeanUnregistered(ObjectName objectName) {
        if (adaptorName.equals(objectName.getCanonicalName())) {
            stop( objectName );
        }
    }
    
    public void setMbeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }
    
    public void setAdaptorName(String adaptorName) {
        this.adaptorName = adaptorName;
    }
    
}