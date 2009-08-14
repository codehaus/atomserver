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
import org.atomserver.AtomCategory;
import org.atomserver.FeedDescriptor;
import org.atomserver.utils.logic.BooleanExpression;

import java.util.Set;

/**
 * A URITarget that specifically represents a Feed request
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class FeedTarget extends URITarget implements FeedDescriptor {
    private final Set<BooleanExpression<AtomCategory>> categoriesQuery;
    private final String workspace;
    private final String collection;
    private final Boolean scrollOnUnmodified;

    public FeedTarget(RequestContext requestContext,
                      final String workspace,
                      final String collection,
                      Set<BooleanExpression<AtomCategory>> categoriesQuery) {
        super(TargetType.TYPE_COLLECTION, requestContext);
        this.categoriesQuery = categoriesQuery;
        this.workspace = workspace;
        this.collection = collection;
        this.scrollOnUnmodified = requestContext.getParameter("scroll-on-unmodified") != null;
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getCollection() {
        return collection;
    }

    public Boolean getScrollOnUnmodified() {
        return scrollOnUnmodified;
    }

    public Set<BooleanExpression<AtomCategory>> getCategoriesQuery() {
        return categoriesQuery;
    }
}