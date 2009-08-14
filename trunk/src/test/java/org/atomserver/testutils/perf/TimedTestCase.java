/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.testutils.perf;

import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.AssertionFailedError;

/**
 * This class wraps a JUnit TestCase, determines the elapsed time of the test,
 * and validates the time against maximum expected time. It fails the
 * test if the elapsed time is greater than the maximum expected time.
 * If the test itself fails before the timing is validated, test failure status is returned.
 */
public class TimedTestCase extends TestCase {

    private String methodName = null;

    private long maxElapsedTime = 0;

    private Class testClass = null;

    /**
     * Constructs a timed test case.
     * @param testClass        JUnit test class
     * @param methodName       name of test method of the  test class
     * @param maxElapsedTime   maximum elasped time in milliseconds
     */
    public TimedTestCase(Class testClass, String methodName, long maxElapsedTime) {
        this.testClass = testClass;
        this.methodName = methodName;
        this.maxElapsedTime = maxElapsedTime;
    }

    public String getMethodName() {
        return methodName;
    }

    public long getMaxElapsedTime() {
        return maxElapsedTime;
    }

    public void run(TestResult result) {

        if(this.methodName == null || "".equals(this.methodName)) {
            throw new IllegalArgumentException("TestCase method name must be set before running the test");
        }
        
        TestCase testCase = null;
        try {
            // Create test case
            testCase = (TestCase) this.testClass.newInstance();
            testCase.setName(methodName);

            result.startTest(testCase);
            long start = System.currentTimeMillis();

            // Run the test
            testCase.runBare();

            long elapsedTime = System.currentTimeMillis() - start;
            if (elapsedTime > getMaxElapsedTime()) {
                result.addFailure(
                    testCase,
                    new AssertionFailedError(testCase.getClass().getName() + "-" + getMethodName() + " takes longer than expected!" +
                    " Expected " + maxElapsedTime + "msec, Actual " + elapsedTime + "msec.")
                );
            }
        } catch( AssertionFailedError ae) {
            result.addFailure(testCase, ae);
        }
        catch (Throwable e) {
            result.addError( testCase, e);
        }
        result.endTest(testCase);
    }

}