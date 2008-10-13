package org.atomserver.testutils.latency;

import org.apache.log4j.Logger;

public class LatencyUtil {
    private static final Logger log = Logger.getLogger(LatencyUtil.class);

    // NOTE: this is hard coded to 2.1s because we assume that the tests use;
    //       db.latency.seconds=2  and  db.timeout.sql.stmts=1
    //       We may want to consider using the actual db.latency.seconds value
    //       but it should never be too large (i.e. at 10s the tests take ~1hr, which is unacceptable)
    public static final int ACCOUNT_FOR_LATENCY = 2100;

    private static long lastWrote;

    public static void updateLastWrote()  {
        lastWrote = System.currentTimeMillis();
        log.debug("LATENCY::last wrote at " + lastWrote);
    }

    public static void accountForLatency()  {
        long wait = lastWrote + ACCOUNT_FOR_LATENCY - System.currentTimeMillis();
        if (wait > 0) {
            log.debug("LATENCY::need to account for latency - sleeping for " + wait + " ms");
            try {
                Thread.sleep(Math.min(ACCOUNT_FOR_LATENCY, wait));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
