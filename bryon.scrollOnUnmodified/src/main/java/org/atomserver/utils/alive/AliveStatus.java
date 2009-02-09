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


/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class AliveStatus {

    static private Log log = LogFactory.getLog( AliveServlet.class );

    static public final AliveStatus OK_STATUS = new AliveStatus( State.OK, null );
    static public final AliveStatus DOWN_STATUS = new AliveStatus( State.DOWN, null ); 

    private State state = State.OK;
    private String errmsg = null;

    public enum State { 
        OK, DOWN, ERROR; 
    }

    public boolean isDown() {
        return ( state == State.DOWN );
    }

    public boolean isOkay() {
        return ( state == State.OK );
    }

    public boolean isError() {
        return ( state == State.ERROR );
    }

    public AliveStatus( State state, String errmsg ) {
        if ( errmsg == null && state == State.ERROR ) 
            throw new IllegalStateException( "An ERROR state requires an Error Message" );
        this.state = state;
        this.errmsg = errmsg;
    }
    
    public String getState() {
        return state.name();
    }

    public String getErrorMessage() { 
        return errmsg;
    } 

    public String toString() {
        return ( state.name() + " " + errmsg );
    }
}
