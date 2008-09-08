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


package org.atomserver.core.dbstore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.ContentStorage;
import org.atomserver.core.dbstore.dao.AtomServerDAO;
import org.atomserver.utils.alive.AliveStatus;
import org.atomserver.utils.alive.IsAliveHandler;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.util.Date;

/**
 * AtomServerIsAliveHandler - The isAliveHandler specific to AtomServer.
 * This class is accessed by the AliveSerlet to determine if the AtomServer
 * is in one of three states;
 * <ol>
 * <li>OK - the AtomServer is alive and well</li>
 * <li>ERROR - the AtomServer is throwing errors</li>
 * <li>DOWN - this means that the AtomServer has been explicitly set as DOWN,
 * so that it can drain it's Request queue, but not be seen by the Load balancer</li>
 * </ol>
 * This class actually delegates to the wired-in DAO and ContentStorage to determine
 * if the Errors are being thrown.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
@ManagedResource(description = "Is Alive Handler")
public class AtomServerIsAliveHandler implements IsAliveHandler {
    
    static private Log log = LogFactory.getLog( AtomServerIsAliveHandler.class );
    
    private AtomServerDAO atomServerDao = null;
    private ContentStorage contentStorage = null;
    
    private AliveStatus aliveStatus = AliveStatus.OK_STATUS;
    
    public void setAtomServerDAO( AtomServerDAO atomServerDao) {
        this.atomServerDao = atomServerDao;
    }
    
    public void setContentStorage( ContentStorage contentStorage ) {
        this.contentStorage = contentStorage;
    }

    /**
     * Set the initial Alive state of the server
     * @param state must be one of "OK" or "DOWN"
     */
    public synchronized void setInitialAliveState( String state ) {
        AliveStatus.State status = AliveStatus.State.valueOf(state);
        if ( status == AliveStatus.State.DOWN ) {
            aliveStatus = AliveStatus.DOWN_STATUS;
        } else if ( status == AliveStatus.State.OK ) {
            aliveStatus = AliveStatus.OK_STATUS;
        } else {
           throw new IllegalArgumentException( "Unknown initial state for IsAliveHandler ("
                                               + state + "). Accepted values are OK or DOWN");
        }
    }

    /**
     * {@inheritDoc}
     */
    @ManagedAttribute
    public String getAliveState() {
        if ( aliveStatus == null ) 
            return AliveStatus.OK_STATUS.getState();
        return aliveStatus.getState();
    }

    /**
     * {@inheritDoc}
     */    
    @ManagedOperation(description = "set alive status to OK")
    public synchronized void activate() {
        aliveStatus = AliveStatus.OK_STATUS;
    } 

    /**
     * {@inheritDoc}
     */    
    @ManagedOperation(description = "set alive status to DOWN")
    public synchronized void deactivate() {
        aliveStatus = AliveStatus.DOWN_STATUS;
    }   

    /**
     * {@inheritDoc}
     */    
    public synchronized AliveStatus isAlive() {
        // the state stays DOWN until it is set from the outside (via JMX)...
        if ( ! aliveStatus.isDown() ) { 
            String errorMsg = null; 
            
            // Test the database
            try {
                atomServerDao.testAvailability();
            } catch ( Exception ee ) {
                errorMsg = "Database Problem. Cause:: " + ee.getMessage() ;
            }
            
            // Test NFS
            try {
                contentStorage.testAvailability();
            } catch ( Exception ee ) {
                errorMsg = "NFS Problem. Cause:: " + ee.getMessage() ;
            }

            if ( errorMsg == null ) 
                aliveStatus = AliveStatus.OK_STATUS;
            else 
                aliveStatus = new AliveStatus( AliveStatus.State.ERROR, errorMsg );
        }
        return aliveStatus; 
    }
}
