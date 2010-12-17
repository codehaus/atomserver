/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.utils.perf;

import org.atomserver.EntryDescriptor;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.PubSubRegistration;

import java.util.Locale;

/**
 * Simple utility methods specific to AtomServer to format tags for perf4j logging.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class AtomServerPerfLogTagFormatter {

    public static String getPerfLogFeedString( Locale locale, String workspace, String collection ) {
        String localeStr = ( locale == null ) ? null : locale.toString();
        return getPerfLogFeedString( localeStr, workspace, collection );
    }

    //~~~~~~~~~~~~~~~~~~~~~~
    public static String getPerfLogFeedString( String locale, String workspace, String collection ) {
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
    public static String getPerfLogEntryString( EntryDescriptor entryQuery ) {
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
    public static String getPerfLogEntryCategoryString( EntryCategory entryQuery ) {
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
    
  //~~~~~~~~~~~~~~~~~~~~~~
    public static String getPerfLogEntryCategoryString( PubSubRegistration entryQuery ) {
        if ( entryQuery == null )
            return "";

        StringBuffer buff = new StringBuffer();
        buff.append( "[" );
        buff.append( entryQuery.getFeedURL() );
        buff.append( "." );
        buff.append( entryQuery.getCallbackURL() );
        buff.append( "." );
        buff.append( entryQuery.getTimestamp() );
        buff.append( "]" );
        return buff.toString();
    }
}
