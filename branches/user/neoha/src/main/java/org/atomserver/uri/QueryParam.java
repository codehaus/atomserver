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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.EntryType;
import org.atomserver.exceptions.BadRequestException;
import org.atomserver.utils.AtomDate;
import org.atomserver.utils.locale.LocaleUtils;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QueryParam - An enumeration which describes the AtomServer URL Query parameters.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public enum QueryParam {
    startIndex("start-index", 0) {
        Object parseValue(String stringValue) {
            return Integer.parseInt(stringValue);
        }},
    endIndex("end-index", -1) {
        Object parseValue(String stringValue) {
            return Integer.parseInt(stringValue);
        }},
    maxResults("max-results", 0) {
        Object parseValue(String stringValue) {
            return Integer.parseInt(stringValue);
        }},
    updatedMin("updated-min", null) {
        Object parseValue(String stringValue) {
            return AtomDate.parse(stringValue);
        }},
    updatedMax("updated-max", null) {
        Object parseValue(String stringValue) {
            return AtomDate.parse(stringValue);
        }},
    locale("locale", null) {
        Object parseValue(String stringValue) {
            return LocaleUtils.toLocale(stringValue);
        }},
    entryType("entry-type", null) {
        Object parseValue(String stringValue) {
            return EntryType.valueOf(stringValue);
        }},
    noLatency("no-latency", false) {
        Object parseValue(String stringValue) {
            return Boolean.valueOf(stringValue);
        }},
    obliterate("obliterate", false) {
        Object parseValue(String stringValue) {
            return Boolean.valueOf(stringValue);
        }};

    private static final Log log = LogFactory.getLog(QueryParam.class);

    private final String paramName;
    private final Object defaultValue;
    private final Pattern pattern;
    public String getParamName() { return paramName; }

    QueryParam(String paramName, Object defaultValue) {
        this.paramName = paramName;
        this.defaultValue = defaultValue;
        // this regex pulls the query param from an IRI string.  it is a partial match (use
        // matcher.find()) and there is one group, which will be the parameter value.
        this.pattern = Pattern.compile("[?&]" + this.paramName + "=([^&]*)(?:[&]|$)");
    }

    abstract Object parseValue(String stringValue);
    public Object parse(RequestContext requestContext) throws BadRequestException {
        log.debug("parsing " + this + " (" + this.paramName + ")");
        Object value = this.defaultValue;
        // there is an Abdera bug regarding its request parameter handling, so we handle it with a
        // regex - ideally, we would do this instead:
        //
        //   String stringValue = requestContext.getParameter(this.paramName);
        //
        Matcher matcher = this.pattern.matcher(requestContext.getResolvedUri().toString());
        String stringValue = matcher.find() ? matcher.group(1) : null;
        log.debug("value is " + stringValue);
        if (stringValue != null) {
            try {
                value = parseValue(stringValue);
            } catch (RuntimeException e) {
                String msg = MessageFormat.format(
                        "Invalid value {1} for parameter {0}", this.paramName, stringValue);
                log.error(msg);
                throw new BadRequestException(msg, e);
            }
        }
        if (log.isTraceEnabled()) {
            log.trace(
                    MessageFormat.format(
                            "parsed request parameter {0} as value {1}",
                            this.paramName,
                            value));
        }
        return value;
    }
}
