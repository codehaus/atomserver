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

package org.atomserver.core.etc;

import java.util.Locale;

import org.atomserver.utils.stats.StatsTrackingPerformanceLog;
import org.atomserver.EntryDescriptor;
import org.atomserver.core.EntryCategory;

/**
 * Adds some simple utility methods specific to AtomServer to the StatsTrackingPerformanceLog
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class AtomServerPerformanceLog extends StatsTrackingPerformanceLog {
    
    //~~~~~~~~~~~~~~~~~~~~~~
    public String getPerfLogFeedString( Locale locale, String workspace, String collection ) { 
        String localeStr = ( locale == null ) ? null : locale.toString();
        return getPerfLogFeedString( localeStr, workspace, collection );
    }

    //~~~~~~~~~~~~~~~~~~~~~~
    public String getPerfLogFeedString( String locale, String workspace, String collection ) { 
        StringBuffer buff = new StringBuffer();
        buff.append( "[" );
        buff.append( workspace );
        buff.append( "." );
        buff.append( collection );
        buff.append( "." );
        buff.append( ((locale == null) ? "**" : locale ) );
        buff.append( "]" );
        return buff.toString();
    }

    //~~~~~~~~~~~~~~~~~~~~~~
    public String getPerfLogEntryString( EntryDescriptor entryQuery ) { 
        if ( entryQuery == null ) 
            return "";
            
        StringBuffer buff = new StringBuffer();
        buff.append( "[" );
        buff.append( entryQuery.getWorkspace() );
        buff.append( "." );
        buff.append( entryQuery.getCollection() );
        buff.append( "." );
        Locale locale = entryQuery.getLocale();
        buff.append( ((locale == null) ? "**" : locale.getLanguage()) );
        buff.append( "." );
        buff.append( ((locale == null || locale.getCountry().equals("")) ? "**" : locale.getCountry()) );
        buff.append( "." );
        buff.append( entryQuery.getEntryId() );
        buff.append( "]" );
        return buff.toString();
    }

    //~~~~~~~~~~~~~~~~~~~~~~
    public String getPerfLogEntryCategoryString( EntryCategory entryQuery ) {
        if ( entryQuery == null ) 
            return "";

        StringBuffer buff = new StringBuffer();
        buff.append( "[" );
        buff.append( entryQuery.getWorkspace() );
        buff.append( "." );
        buff.append( entryQuery.getCollection() );
        buff.append( "." );
        buff.append( entryQuery.getEntryId() );
        buff.append( "." );
        buff.append( entryQuery.getScheme() );
        buff.append( "." );
        buff.append( entryQuery.getTerm() );
        buff.append( "]" );
        return buff.toString();
    }

}