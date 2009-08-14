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

package org.atomserver.utils.perf;

import org.atomserver.utils.IOCLog;

/**
 * PerformanceLog - logger that uses the StopWatch interface to log performance numbers.
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class PerformanceLog extends IOCLog {
    protected java.util.Set<String> omitSvcTags = null; 

    /**
     * Setter for property 'omitSvcTags'.
     *
     * @param omitSvcTags Value to set for property 'omitSvcTags'.
     */
    public void setOmitSvcTags(java.util.Set<String> omitSvcTags) {
        this.omitSvcTags = omitSvcTags;
    }

    protected boolean isTagDisabled( String svcTag1 )  {
        if ( svcTag1 != null && omitSvcTags != null ) {
            for (String omitTag : omitSvcTags) {
                if ( svcTag1.startsWith( omitTag ) )
                    return true;
            }
        }
        return false;
    }
    
    public void log( String svcTag1, String svcTag2, StopWatch stopWatch ) {       
        if ( ioclog != null && ioclog.isInfoEnabled () ) {
            if ( ! isTagDisabled( svcTag1 ) )  {
                long startMs = stopWatch.getStartTimeAsLong();
                long elapsedMs = stopWatch.getElapsedInMillis();
                ioclog.info( svcTag1 + "," + svcTag2 + "," + startMs + "," + elapsedMs );
            }
        }
    }

}