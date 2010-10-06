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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jfree.data.xy.XYDataset;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * mvn clean test -Dtest=PlotTest
 *
 * NOTE: if -DplotsVisible=true, will only display one plot. So "X out" the other tests...
 */
public class PlotTest  extends TestCase {

    // toggle this back to FALSE before you check in !!!
    static private final boolean MAKE_PLOTS_LOCALLY = false;
    //static private final boolean MAKE_PLOTS_LOCALLY = true;

    static private boolean MAKE_VISIBLE = getBooleanProperty( "plotsVisible" );
    static {
        if ( MAKE_PLOTS_LOCALLY )
            MAKE_VISIBLE = true ;
    }

    // -------------------------------------------------------
    public static Test suite()
    { return new TestSuite( PlotTest.class ); }

    // -------------------------------------------------------
    protected void setUp() throws Exception
    { super.setUp(); }

    // -------------------------------------------------------
    protected void tearDown() throws Exception
    { super.tearDown(); }

    //----------------------------
    //          Tests 
    //----------------------------
    static private boolean getBooleanProperty( String propertyName ) {
        String propval = System.getProperty( propertyName );
        boolean bval = false;
        if ( propval != null && propval.length() > 0 ) {
            if ( propval.trim().toLowerCase().equals( "true" ) )
                bval = true;
        }
        return bval;
    }

    static private final String USER_DIR = System.getProperty( "user.dir" );
    static private final int DONT_USE_NUM_POINTS = -1;

    private XYDataset readDataSet( String filename, boolean lookupAsResource,
                                   ArrayList<String> methodNamesToPlot, String methodNameSuffix,
                                   boolean computeMovingAverage )
        throws Exception {
        return  readDataSet( filename, lookupAsResource, methodNamesToPlot, methodNameSuffix,
                             computeMovingAverage, DONT_USE_NUM_POINTS, false );
    }

    private XYDataset readDataSet( String filename, boolean lookupAsResource,
                                   ArrayList<String> methodNamesToPlot, String methodNameSuffix,
                                   boolean computeMovingAverage, int numPointsInAvg )
        throws Exception {
        return  readDataSet( filename, lookupAsResource, methodNamesToPlot, methodNameSuffix,
                             computeMovingAverage, numPointsInAvg, false  );
    }

    private XYDataset readDataSet( String filename, boolean lookupAsResource,
                                   List<String> methodNamesToPlot, String methodNameSuffix,
                                   boolean computeMovingAverage, int numPointsInAvg, boolean plotTPM )
        throws Exception {

        PerfCSVReader csvReader = new PerfCSVReader();
        File csvFile = null;
        if ( lookupAsResource ) {
            URL url = this.getClass().getResource( filename );
            csvFile = new File( url.toURI() );
        } else {
            csvFile = new File( filename );
        }
        assertNotNull( csvFile );

        PerfDataSet perfDataSet = csvReader.readCSVFile( csvFile );
        assertNotNull( perfDataSet );

        XYDataset xyDataSet = null;
        if ( numPointsInAvg == DONT_USE_NUM_POINTS )
            xyDataSet = perfDataSet.getXYDataSet( true, methodNamesToPlot, methodNameSuffix, computeMovingAverage );
        else
            xyDataSet = perfDataSet.getXYDataSet( true, methodNamesToPlot, methodNameSuffix, computeMovingAverage, numPointsInAvg, plotTPM );
        return xyDataSet;
    }

    public void testPlotCombined() throws Exception {
        if ( MAKE_PLOTS_LOCALLY )
            return;

        ArrayList<String> methodNamesToPlot = new ArrayList<String>();
        methodNamesToPlot.add( "GET.feed" );

        ArrayList<XYDataset> xyDatasetList = new ArrayList<XYDataset>();
        boolean computeMovingAverage = true;

        XYDataset xyDataSet = readDataSet( "/without-new-SQL.perf.csv", true,
                                           methodNamesToPlot, "(with N+1 SQL)", computeMovingAverage );
        xyDatasetList.add( xyDataSet );

        xyDataSet = readDataSet( "/with-new-SQL.perf.csv", true,
                                 methodNamesToPlot, "(with SQL fix)", computeMovingAverage);
        xyDatasetList.add( xyDataSet );

        XYDataset combinedDataset = PerfDataSet.combineDataSets( xyDatasetList );

        //------------
        String pngFile = "./target/compareSQL.png";
        PerfPlotter plotter = new PerfPlotter( combinedDataset, 700, 500,
                                               "Comparison of \"N+1 SQL\" vs. \"improved form\" (10/19)",
                                               "Time (sec) normalized",
                                               "Average Response (sec)",
                                               pngFile ) ;

        File ff = new File( pngFile );
        assertNotNull( ff );
        assertTrue( ff.exists() );

        if ( MAKE_VISIBLE ) {
            plotter.displayInFrame( 30000 );
        }
    }

    public void testPlotAll() throws Exception {
        if ( MAKE_PLOTS_LOCALLY )
            return;

        boolean computeMovingAverage = false;
        XYDataset xyDataset = readDataSet( "/RC-SPREAD-10u.csv", true, null, null, computeMovingAverage );

        String pngFile = "./target/testutils.png";
        PerfPlotter plotter = new PerfPlotter( xyDataset, 700, 500,
                                               "All Methods (10/19)",
                                               "Time (sec) normalized",
                                               "Average Response (sec)",
                                               pngFile ) ;
        File ff = new File( pngFile );
        assertNotNull( ff );
        assertTrue( ff.exists() );

        if ( MAKE_VISIBLE ) {
            plotter.displayInFrame( 30000 );
        }
    }

    // +++++++++++++++++++++++++++++++++++
    // The following generate plots for specific CSVs
    // DO NOT REMOVE THEM -- I reuse logic from then as I go....
    // DO NOT FORGET TO COMMENT THEM OUT BEFORE CHECKING IN (i.e. testF00 ==> XXXtestFoo)
    // +++++++++++++++++++++++++++++++++++
    public void XXXtestLongevityPlot() throws Exception {
         boolean computeMovingAverage = true;

         ArrayList<String> methodNamesToPlot = new ArrayList<String>();

         methodNamesToPlot.add( "GET.feed" );
         methodNamesToPlot.add( "GET.entry" );
         methodNamesToPlot.add( "PUT.entry" );
         methodNamesToPlot.add( "DELETE.entry" );


         XYDataset xyDataset = readDataSet(  USER_DIR + "/longevity.perf.csv", false,
                                             methodNamesToPlot, null, computeMovingAverage, 40 );

         String pngFile = "./target/1.3.Atom.stability-MovAvg.png";
         PerfPlotter plotter = new PerfPlotter( xyDataset, 1000, 700,
                                                "21 hr Stability Test - Atom Methods -Moving Averages (2/12/08)",
                                                "Time (sec) normalized",
                                                "Average Response (sec)",
                                                pngFile, false ) ;
         File ff = new File( pngFile );
         assertNotNull( ff );
         assertTrue( ff.exists() );

         if ( MAKE_VISIBLE ) {
             plotter.displayInFrame( 30000 );
         }
     }


    public void XXXtestToGeneratePlots() throws Exception {
        List<String> methodNamesToPlot = Arrays.asList(
                "GET.entry", "PUT.entry", "DELETE.entry", "GET.feed");

        int width = 700;
        int height = 700;
        String csvFile = System.getProperty("csvfile");
        assertNotNull("you must provide a CSV file ( -Dcsvfile=FILENAME )", csvFile);
        String title = System.getProperty("title");
        assertNotNull("you must provide a title ( -Dtitle=TITLE )", title);

        createPlot(methodNamesToPlot, width, height, csvFile, title,
                   false, "avgresp.png", " - Average Response Times", "Average Response (sec)");
        createPlot(methodNamesToPlot, width, height, csvFile, title,
                   true, "tpm.png", " - Throughput", "Throughput (TPM)");
    }

    private void createPlot(List<String> methodNamesToPlot, int width, int height, String csvFile, String title,
                            boolean plotTPM, String filename, String titleSuffix, String yAxis)
            throws Exception {
        XYDataset dataset = readDataSet(csvFile, false, methodNamesToPlot, null,
                                        true, 8, plotTPM);
        File pngFile = new File(new File(csvFile).getParentFile(), filename);

        //boolean useDataPointMarkers = true;
        boolean useDataPointMarkers = false;

        new PerfPlotter(dataset, width, height,
                        title + titleSuffix,
                        "Time (sec) normalized",
                        yAxis,
                        pngFile.getAbsolutePath(),
                        useDataPointMarkers );
    }


    public void XXXtestPlot() throws Exception {

        boolean computeMovingAverage = true;
        boolean useDataPointMarkers = false;

        boolean plotTPM = false;
        //boolean plotTPM = true;

        ArrayList<String> methodNamesToPlot = new ArrayList<String>();

        String methodName = "Atom.methods";
        methodNamesToPlot.add( "GET.entry" );
        methodNamesToPlot.add( "PUT.entry" );
        methodNamesToPlot.add( "DELETE.entry" );
        methodNamesToPlot.add( "GET.feed" );

        /*
        String methodName = "GET.feed";
        methodNamesToPlot.add( methodName );
        */

       /*
       methodNamesToPlot.add( "DB.selectEntry" );
       methodNamesToPlot.add( "DB.updateEntry" );
       methodNamesToPlot.add( "DB.insertEntry" );
       methodNamesToPlot.add( "DB.insertEntryCategoryBATCH" );
       methodNamesToPlot.add( "DB.selectEntryCategoriesInScheme" );
       */
        /*
       methodNamesToPlot.add( "DB.selectEntriesByPage" );
       methodNamesToPlot.add( "DB.selectEntriesByPageAndLocale" );
       methodNamesToPlot.add( "DB.selectEntriesByPageAndLocalePerCategory_disjunction" );
       methodNamesToPlot.add( "DB.selectEntriesByPagePerCategory_conjunction" );
       methodNamesToPlot.add( "DB.selectEntriesByPagePerCategory_disjunction" );
       */

        //int width = 700;
        //int hieght = 500;
        //int width = 700;
        //int hieght = 700;
        int width = 1200;
        int hieght = 1000;

        String csvFile =  USER_DIR + "/hcdata.perf.100full.3hr.ALL2.20users.csv";
        String csvFile2 =  USER_DIR + "/hcdata.perf.50full.3hr.ALL2.20users.csv";
        String csvFile3 =  USER_DIR + "/hcdata.perf.25full.3hr.ALL2.20users.csv";
        int numPointsInAvg = 8;

        String plotName = "AvgResp";
        String yAxis = "Average Response (sec)";
        if ( plotTPM )  {
            plotName = "TPM" ;
            yAxis = "Transactions per Minute";
        }

        ArrayList<XYDataset> xyDatasetList = new ArrayList<XYDataset>();
        XYDataset xyDataSet = readDataSet( csvFile, false, methodNamesToPlot,
                                           "100 entries/full",
                                           computeMovingAverage, numPointsInAvg, plotTPM );
        xyDatasetList.add( xyDataSet );

        xyDataSet = readDataSet( csvFile2, false, methodNamesToPlot,
                                 "50 entries/full",
                                 computeMovingAverage, numPointsInAvg, plotTPM );
        xyDatasetList.add( xyDataSet );

        xyDataSet = readDataSet( csvFile3, false, methodNamesToPlot,
                                 "25 entries/full",
                                 computeMovingAverage, numPointsInAvg, plotTPM );
        xyDatasetList.add( xyDataSet );

        XYDataset combinedDataset = PerfDataSet.combineDataSets( xyDatasetList );

        //------------
        String movingAvgFileAdd = ( computeMovingAverage ) ? "-MoveAvg" : "" ;
        String pngFile = "./target/" + "1127-25-100full-214-20users-ALL2-" +
                         methodName + "-" + plotName + movingAvgFileAdd + ".png";
        String movingAvgTitleAdd = ( computeMovingAverage ) ? "- Moving Average " : "" ;

        String title = methodName + " - 25 to 100 Entries per Full Feed - " + yAxis  + movingAvgTitleAdd +
                       "- (11/29/08)";
        PerfPlotter plotter = new PerfPlotter( combinedDataset, width, hieght, title,
                                               "Time (sec) normalized",
                                               yAxis,
                                               pngFile,
                                               useDataPointMarkers) ;

        File ff = new File( pngFile );
        assertNotNull( ff );
        assertTrue( ff.exists() );

        if ( MAKE_VISIBLE ) {
            plotter.displayInFrame( 30000 );
        }
    }

}
