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


package org.atomserver.core.validators;

import org.atomserver.exceptions.BadContentException;
import org.atomserver.ContentValidator;
import java.util.Collection;

/**
 * MultiValidator - implementation of the ContentValidator interface that checks content against
 * a collection of ContentValidators, all of which must pass the content.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class MultiValidator implements ContentValidator {

    private Collection<ContentValidator> validators = null;

    public void setValidators(Collection<ContentValidator> validators) {
        this.validators = validators;
    }

    public void validate(String contentXML) throws BadContentException {
        try {
            for (ContentValidator validator : validators) {
                validator.validate(contentXML);
            }
        } catch (Exception e) {
            throw new BadContentException("document invalid - " + e.getMessage(), e);
        }
    }
}
