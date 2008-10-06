package org.atomserver.testutils.latency;

import org.apache.log4j.Logger;

public class LatencyUtil {
    private static final Logger log = Logger.getLogger(LatencyUtil.class);

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
