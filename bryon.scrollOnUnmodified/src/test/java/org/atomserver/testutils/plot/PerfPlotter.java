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

import java.awt.Color;
import java.text.SimpleDateFormat;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.RefineryUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A time series chart.  For the most part, default settings are 
 * used, except that the renderer is modified to show filled shapes (as well as 
 * lines) at each data point.
 */
public class PerfPlotter {

    static private Log log = LogFactory.getLog(PerfPlotter.class );

    private int width = 0; 
    private int hieght = 0; 

    private String title = null;
    private String xAxisLabel = null;
    private String yAxisLabel = null;
    
    private String pngFileName = null; 

    private boolean useDataPointMarkers = true;

    private JFreeChart plot = null ;

    public PerfPlotter( XYDataset dataset, 
                        int width, int hieght,
                        String title, String xAxisLabel, String yAxisLabel,
                        String pngFileName ) {
        this( dataset, width, hieght, title, xAxisLabel, yAxisLabel, pngFileName, true);
    }

    public PerfPlotter( XYDataset dataset, 
                        int width, int hieght,
                        String title, String xAxisLabel, String yAxisLabel,
                        String pngFileName, boolean useDataPointMarkers ) {
        this.width = width; 
        this.hieght = hieght;         
        this.title = title;
        this.xAxisLabel = xAxisLabel;
        this.yAxisLabel = yAxisLabel;        
        this.pngFileName = pngFileName; 

        this.useDataPointMarkers = useDataPointMarkers;

        this.plot = createPlot( dataset );
    }

    public void displayInFrame( int displayTime )  { 
        ApplicationFrame appFrame = new ApplicationFrame( title );
        appFrame.setContentPane( getChartPanel() );
        
        appFrame.pack();
        RefineryUtilities.centerFrameOnScreen( appFrame );
        appFrame.setVisible(true);

        try { Thread.sleep( displayTime ); }
        catch( java.lang.InterruptedException ee ) {}
    }

    private ChartPanel getChartPanel()  { 
        ChartPanel chartPanel = new ChartPanel( this.plot );
        if ( log.isTraceEnabled() ) 
            log.trace( "chartPanel= " + chartPanel );

        chartPanel.setPreferredSize( new java.awt.Dimension( this.width,  this.hieght ) );
        chartPanel.setMouseZoomable( true, false );

        return chartPanel; 
    } 

    /**
     */
    private JFreeChart createChart( XYDataset dataset ) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            title,              // title
            xAxisLabel,         // x-axis label
            yAxisLabel,         // y-axis label
            dataset,            // data
            true,               // create legend?
            true,               // generate tooltips?
            false               // generate URLs?
        );
        chart.setBackgroundPaint(Color.white);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        
        if ( useDataPointMarkers ) { 
            XYItemRenderer r = plot.getRenderer();
            if (r instanceof XYLineAndShapeRenderer) {
                XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
                renderer.setBaseShapesVisible(true);
                renderer.setBaseShapesFilled(true);
            }      
        }

        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride( new SimpleDateFormat("HH:mm:ss") );
        
        return chart;
    }
    

    /**
     */
    public JFreeChart createPlot( XYDataset dataSet ) {

        JFreeChart chart = createChart( dataSet );
        if ( log.isTraceEnabled() ) 
            log.trace( "chart= " + chart );

        try { 
            java.awt.image.BufferedImage buffImage = chart.createBufferedImage( width, hieght );
            if ( log.isTraceEnabled() ) 
                log.trace( "buffImage= " + buffImage );

            org.jfree.chart.encoders.SunPNGEncoderAdapter pngEncoder = new org.jfree.chart.encoders.SunPNGEncoderAdapter();

            java.io.FileOutputStream fileOS = new java.io.FileOutputStream( pngFileName );
            if ( log.isTraceEnabled() ) 
                log.trace( "fileOS= " + fileOS );

            pngEncoder.encode( buffImage, fileOS );
            fileOS.close();
            if ( log.isTraceEnabled() ) 
                log.trace( "PNG file created sucessfully!!!!" );
        } catch ( Exception ee ) {
            log.error( ee );
            return null;
        }
        return chart; 
    }
    
}
