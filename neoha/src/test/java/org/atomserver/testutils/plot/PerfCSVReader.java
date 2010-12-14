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


package org.atomserver.testutils.plot;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PerfCSVReader {
    static private Log log = LogFactory.getLog( PerfCSVReader.class );
     
    static private final int DATE_FIELD = 0; 
    static private final int TIME_FIELD = 1; 
    static private final int METHOD_FIELD = 2; 
    static private final int COUNT_FIELD = 3; 
    static private final int AVGRESP_FIELD = 4; 
    static private final int MINRESP_FIELD = 5;
    static private final int MAXRESP_FIELD = 6;
    static private final int STDDEV_FIELD = 7;
    static private final int TPM_FIELD = 8;

    // 10/19/2007,11:53:00
    static private final SimpleDateFormat dateFormat = new SimpleDateFormat( "MM/dd/yyyy HH:mm:ss" );

    public PerfDataSet readCSVFile( File csvFile ) {
        if ( log.isTraceEnabled() ) 
            log.trace( "csvFile= " + csvFile );

        PerfDataSet dataSet = new PerfDataSet(); 
        
        BufferedReader br = null;
        try {
            br = new BufferedReader( new FileReader( csvFile ) );
            String line = null; 
            while ( ( line = br.readLine() ) != null ){
                if ( ! line.equals( "" ) ) { 
                    dataSet.addPerfData( processCSVLine( line ) );
                }
            }
        }
        catch ( Exception ex ) {
            log.error( ex );
            return null;
        }
        finally {
            try { 
                if ( br != null ) br.close();
            }
            catch ( Exception ex ) {} 
        }
        return dataSet;
    }  
     
    private PerfDataSet.PerfData processCSVLine( String line ) throws Exception { 
        String csvInfo[] = line.split( "," );
        
        Date datetime = convertToDate( csvInfo[DATE_FIELD], csvInfo[TIME_FIELD] );
        String method = csvInfo[METHOD_FIELD];
        int count = convertToInt( csvInfo[COUNT_FIELD] );
        double avgResp = convertToDouble( csvInfo[AVGRESP_FIELD] );
        double maxResp = convertToDouble( csvInfo[MINRESP_FIELD] );
        double minResp = convertToDouble( csvInfo[MAXRESP_FIELD] );
        double stdDev = convertToDouble( csvInfo[STDDEV_FIELD] );
        double tpm = convertToDouble( csvInfo[TPM_FIELD] );

        PerfDataSet.PerfData perfData = new PerfDataSet.PerfData( datetime, method, count, avgResp, minResp, maxResp, stdDev, tpm );
        if ( log.isTraceEnabled() ) 
            log.trace( "perfData= " + perfData );
        return perfData;
    }

    private Date convertToDate( String date, String time ) throws Exception {
        return dateFormat.parse( date + " " + time );
    }

    private int convertToInt( String sval ) throws Exception {
        return Integer.valueOf( sval );
    }

    private double convertToDouble( String sval ) throws Exception {
        return Double.valueOf( sval );
    }
}
