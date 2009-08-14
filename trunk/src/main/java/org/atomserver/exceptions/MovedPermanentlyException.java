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

package org.atomserver.exceptions;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class MovedPermanentlyException extends AtomServerException {

    private String alternateURI = null;
    
    public String getAlternateURI() {
        return alternateURI;
    }

    public MovedPermanentlyException() {
    }

    public MovedPermanentlyException( String message, String alternateURI ) {
        super(message);
        this.alternateURI = alternateURI; 
    }

    public MovedPermanentlyException(String message) {
        super(message);
    }

    public MovedPermanentlyException(Throwable cause) {
        super(cause);
    }

    public MovedPermanentlyException(String message, Throwable cause) {
        super(message, cause);
    }
}
