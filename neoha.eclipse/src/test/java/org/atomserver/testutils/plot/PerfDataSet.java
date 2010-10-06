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

import java.util.HashMap; 
import java.util.Map; 
import java.util.List; 
import java.util.ArrayList;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.MovingAverage; 
import org.jfree.data.xy.XYDataset;

public class PerfDataSet {
    static private Log log = LogFactory.getLog( PerfDataSet.class );

    static private final int NUM_POINTS_IN_AVG_CALC = 5; 

    private Map< String, List<PerfData> > dataSet = new HashMap< String, List<PerfData> >();

    private Date minDate = null;
    private Date maxDate = null;

    static public XYDataset combineDataSets( List<XYDataset> xyDatasetList ) { 
        TimeSeriesCollection newTSC = new TimeSeriesCollection();
        for ( XYDataset xyDataset : xyDatasetList ) { 
            TimeSeriesCollection subTSC = (TimeSeriesCollection)xyDataset;
            List seriesList = subTSC.getSeries();
            for( Object seriesObj : seriesList ) {
                TimeSeries series = (TimeSeries)seriesObj;
                newTSC.addSeries( series );
            }
        }
        return newTSC;
    }

    public void addPerfData( PerfData perfData ) {
        String method = perfData.getMethod();
        List<PerfData> dataList = dataSet.get( method );
        if ( dataSet.get( method ) == null ) {
            dataList = new ArrayList<PerfData>(); 
            dataSet.put( method, dataList );
        }
        dataList.add( perfData ); 
        
        Date datetime = perfData.getDateTime();
        if ( minDate == null || datetime.before( minDate ) ) {
            minDate = datetime;
        }
        if ( maxDate == null || datetime.after( maxDate ) ) {
            maxDate = datetime;
        }
    }

    public XYDataset getXYDataSet() {
        return getXYDataSet( false, null, null, false );
    }

    public XYDataset getXYDataSet( boolean normalizeTimes, List methodNamesToPlot, 
                                   String methodNameSuffix, boolean computeMovingAverage ) {
        return getXYDataSet( normalizeTimes, methodNamesToPlot, methodNameSuffix, computeMovingAverage, NUM_POINTS_IN_AVG_CALC, false  );
    }

    public XYDataset getXYDataSet( boolean normalizeTimes, List methodNamesToPlot, String methodNameSuffix, 
                                   boolean computeMovingAverage, int numPointsInAvg ) {
        return getXYDataSet( normalizeTimes, methodNamesToPlot, methodNameSuffix, computeMovingAverage, numPointsInAvg, false );
    }

    public XYDataset getXYDataSet( boolean normalizeTimes, List methodNamesToPlot, String methodNameSuffix, 
                                   boolean computeMovingAverage, int numPointsInAvg, boolean plotTPM ) {
        long lMinDate = 0L; 
        if ( normalizeTimes ) {
            lMinDate = minDate.getTime(); 
        }
        TimeSeriesCollection tsc = new TimeSeriesCollection();
        
        for (  Map.Entry< String, List<PerfData> > entry : dataSet.entrySet() ) {
            String methodName = entry.getKey();

            if ( methodNamesToPlot != null ) {
                if (  ! methodNamesToPlot.contains( methodName ) )
                    continue;
            }
    
            if ( methodNameSuffix != null ) {
                methodName += " " + methodNameSuffix;
            }
            
            TimeSeries series = new TimeSeries( methodName, Second.class );

            List<PerfData> perfDataList = entry.getValue();
            for ( PerfData perfData : perfDataList ) { 
                Date datetime = perfData.getDateTime();
                Double value = 0.0;
                if ( plotTPM ) 
                    value = perfData.getTPM();
                else 
                    value = perfData.getAvgResp(); 

                if ( normalizeTimes ) {
                    long lDatetime = datetime.getTime();
                    long lNormalizedTime = lDatetime - lMinDate; 
                    datetime = new Date( lNormalizedTime );
                    if ( log.isTraceEnabled() ) 
                        log.trace( "[ " + lDatetime + ", " + lMinDate + ", " + lNormalizedTime
                                   + "] normalized time = " + datetime );
                }
                
                series.add( new Second( datetime ), value );
            }

            if ( computeMovingAverage ) { 
                TimeSeries movingAvg = MovingAverage.createPointMovingAverage( series,  methodName + " moving average", 
                                                                               numPointsInAvg );
                tsc.addSeries( movingAvg );
            } else { 
                tsc.addSeries( series );
            }
            

        }       
        return tsc;
    }

    static public class PerfData {
        private String method = null;
        private Date datetime = null; 
        private int count = 0;
        private double avgResp = 0.0;
        private double maxResp = 0.0;
        private double minResp = 0.0;
        private double stdDev = 0.0;
        private double tpm = 0;

        public String getMethod() { return method; } 
        public Date getDateTime() { return datetime; } 
        public double getAvgResp() { return avgResp; } 
        public double getTPM() { return tpm; } 

        public PerfData( Date datetime, String method, int count,
                         double avgResp, double minResp, double maxResp, double stdDev, double tpm) {
            this.method = method;
            this.datetime = datetime;
            this.avgResp = avgResp;
            this.maxResp = maxResp;
            this.minResp = minResp;
            this.stdDev = stdDev;
            this.count = count;
            this.tpm = tpm;
        }

        public String toString() {
            StringBuffer buff= new StringBuffer();
            buff.append( "[ " + datetime );
            buff.append( ", " + method );
            buff.append( ", " + count );
            buff.append( ", " + avgResp );
            buff.append( ", " + minResp );
            buff.append( ", " + maxResp );
            buff.append( ", " + stdDev );
            buff.append( ", " + tpm + " ]" );         
            return buff.toString();
        }
    }
    



}
