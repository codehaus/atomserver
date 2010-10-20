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

package org.atomserver.uri;

import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.TargetType;
import org.apache.abdera.protocol.server.impl.AbstractTarget;
import org.atomserver.EntryType;
import org.atomserver.exceptions.BadRequestException;

import java.util.Date;
import java.util.Locale;

/**
 * URITarget - The API for accessing AtomServer URI information, extending the Abdera AbstractTarget.
 * Mostly this provides access to the AtomServer Query parameters.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public abstract class URITarget extends AbstractTarget {
    private final RequestContext requestContext;

    protected URITarget(TargetType targetType, RequestContext requestContext) {
        super(targetType, requestContext);
        this.requestContext = requestContext;
    }

    public int getStartIndexParam() throws BadRequestException {
        return (Integer) QueryParam.startIndex.parse(this.requestContext);
    }

    public int getEndIndexParam() throws BadRequestException {
        return (Integer) QueryParam.endIndex.parse(this.requestContext);
    }

    public int getMaxResultsParam() throws BadRequestException {
        return (Integer) QueryParam.maxResults.parse(this.requestContext);
    }

    public Date getUpdatedMinParam() throws BadRequestException {
        return (Date) QueryParam.updatedMin.parse(this.requestContext);
    }

    public Date getUpdatedMaxParam() throws BadRequestException {
        return (Date) QueryParam.updatedMax.parse(this.requestContext);
    }

    public Locale getLocaleParam() throws BadRequestException {
        return (Locale) QueryParam.locale.parse(this.requestContext);
    }

    public EntryType getEntryTypeParam() throws BadRequestException {
        return (EntryType) QueryParam.entryType.parse(this.requestContext);
    }

    public Boolean getNoLatency() throws BadRequestException {
        return (Boolean) QueryParam.noLatency.parse(this.requestContext);
    }

    public RequestContext getRequestContext() {
        return requestContext;
    }
}
