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

package org.atomserver.core;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;

import java.util.Locale;
import java.util.Date;

/**
 * Describes the fundamental things that identify an entry State in an AtomServer - the
 * (workspace, collection, entryId) tuple.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class EntryState {
    private Long entryStoreId = null;
    //private Long entryStateId = null;
    private int entryStateId = -1;

    private String workspace = null;
    private String collection = null;
    private String entryId = null;
    private String language = "**";
    private String country = "**";

    private Date createDate = null;
    private String serverIp = null;
    private String serviceName = null;
    private String state = null;
    private String message = null;

    public EntryState() {}

    public Long getEntryStoreId() { return entryStoreId; }
    public void setEntryStoreId(Long entryStoreId) { this.entryStoreId = entryStoreId; }

    //public Long getEntryStateId() { return entryStateId; }
    //public void setEntryStateId(Long entryStateId) { this.entryStateId = entryStateId; }
    public int getEntryStateId() { return entryStateId; }
    public void setEntryStateId(int entryStateId) { this.entryStateId = entryStateId; }

    public String getWorkspace() { return workspace; }
    public void setWorkspace(String workspace) { this.workspace = workspace; }

    public String getCollection() { return collection; }
    public void setCollection(String collection) { this.collection = collection; }

    public void setLocale(Locale locale) {
        setLanguage(locale == null ? null : locale.getLanguage());
        setCountry(locale == null ? null : locale.getCountry());
    }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = StringUtils.isEmpty(language) ? "**" : language; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = StringUtils.isEmpty(country) ? "**" : country; }

    public String getEntryId() { return entryId; }
    public void setEntryId(String entryId) { this.entryId = entryId; }

    //------------
    public Date getCreateDate() { return createDate; }
    public void setCreateDate(Date createDate) { this.createDate = createDate; }

    public String getServerIp() { return serverIp; }
    public void setServerIp(String serverIp) { this.serverIp = serverIp; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append("[ ").append(entryStoreId)
            .append(", ").append(workspace)
            .append(", ").append(collection)
            .append(", ").append(entryId)
            .append(", ").append(language)
            .append(", ").append(country)
            .append(", ").append(createDate)
            .append(", ").append(serverIp)
            .append(", ").append(serviceName)
            .append(", ").append(state)
            .append(", ").append(message).append("]");
        return buff.toString();
    }

    public int hashCode() {
        return (entryStoreId == null) ?
               new HashCodeBuilder(17, 8675309)
                       .append(workspace).append(collection).append(entryId)
                       .append(language).append(country)
                       .append(state).append(createDate)
                       .toHashCode() :
                                     new HashCodeBuilder(17, 8675309)
                                             .append(entryStoreId)
                                             .append(state).append(createDate)
                                             .toHashCode();
    }

    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        EntryState other = (EntryState) obj;
        return (entryStoreId == null || other.entryStoreId == null) ?
               new EqualsBuilder()
                       .append(workspace, other.workspace)
                       .append(collection, other.collection)
                       .append(entryId, other.entryId)
                       .append(language, other.language)
                       .append(country, other.country)
                       .append(state, other.state)
                       .append(createDate, other.createDate)
                       .isEquals() :
                                   new EqualsBuilder()
                                           .append(entryStoreId, other.entryStoreId)
                                           .append(state, other.state)
                                           .append(createDate, other.createDate)
                                           .isEquals();
    }
}
