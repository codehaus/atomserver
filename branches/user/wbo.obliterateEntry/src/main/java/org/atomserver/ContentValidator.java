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

package org.atomserver;

import org.atomserver.exceptions.BadContentException;

/**
 * ContentValidator - API for validating the content of entries as they are written to the store.
 * Implementations of this interface are wired to an AtomCollection.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface ContentValidator {

    /**
     * Validate the content provided. Subclasses may be simple XML validation, or validation for a particular
     * schema, or whatever is appropriate.
     * The validate method is expected to throw a BadContentException when
     * it encounters a problem. Thus, it returns nothing.
     * In other words, a simple "boolean validate()" wouldn't be very informative.
     *
     * @param content the content to validate
     * @throws org.atomserver.exceptions.BadContentException
     *          if the content is invalid
     */
    void validate(String content) throws BadContentException;
}
