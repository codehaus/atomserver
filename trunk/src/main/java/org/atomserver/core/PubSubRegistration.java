/* Copyright (c) 2010 HomeAway, Inc.
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

package org.atomserver.core;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Describes the fundamental things that identify a pubsub entry - the
 * (registationId, feedURL, callbackURL, timestamp) tuple.
 * @author Austin Buckner (abuckner at homeaway.com)
 */
public class PubSubRegistration {
    protected Long registrationId = null;
    protected String feedURL = null;
    protected String callbackURL = null;
    protected Long timestamp = null;

    public PubSubRegistration() {}

    public Long getRegistrationId() { return registrationId; }

    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }

    public String getFeedURL() { return feedURL; }

    public void setFeedURL(String feedURL) { this.feedURL = feedURL; }

    public String getCallbackURL() { return callbackURL; }

    public void setCallbackURL(String callbackURL) { this.callbackURL = callbackURL; }
    
    public Long getTimestamp() { return timestamp; }

    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append("[ ").append(registrationId)
            .append(", ").append(feedURL)
            .append(", ").append(callbackURL)
            .append(", ").append(timestamp).append("]");
        return buff.toString();
    }

    public int hashCode() {
        return (registrationId == null) ?
               new HashCodeBuilder(17, 8675309)
                       .append(feedURL).append(callbackURL).append(timestamp)
                       .toHashCode() :
                                     new HashCodeBuilder(17, 8675309)
                                             .append(registrationId)
                                             .append(feedURL).append(callbackURL).append(timestamp)
                                             .toHashCode();
    }

    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        PubSubRegistration other = (PubSubRegistration) obj;
        return (registrationId == null || other.registrationId == null) ?
               new EqualsBuilder()
                       .append(feedURL, other.feedURL)
                       .append(callbackURL, other.callbackURL)
                       .append(timestamp, other.timestamp)
                       .isEquals() :
                                   new EqualsBuilder()
                                           .append(registrationId, other.registrationId)
                                           .append(feedURL, other.feedURL)
                                           .append(callbackURL, other.callbackURL)
                                           .append(timestamp, other.timestamp)
                                           .isEquals();
    }
}
