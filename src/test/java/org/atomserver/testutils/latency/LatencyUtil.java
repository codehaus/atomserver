package org.atomserver.testutils.latency;

import org.apache.log4j.Logger;
import org.atomserver.utils.conf.ConfigurationAwareClassLoader;

public class LatencyUtil {
    private static final Logger log = Logger.getLogger(LatencyUtil.class);

    // NOTE: this defaults to 2.1s because we assume that the tests use;
    //       db.latency.seconds=2  and  db.timeout.sql.stmts=1
    //       We can use the actual db.latency.seconds value
    //       but it should never be too large (i.e. at 10s the tests take ~1hr, which is unacceptable)

    public static final int ACCOUNT_FOR_LATENCY_DEFAULT = 2100;

    public static int accountForLatency = -1;
    private static long lastWrote;

    public static void updateLastWrote()  {
        lastWrote = System.currentTimeMillis();
        log.debug("LATENCY::last wrote at " + lastWrote);
    }

    public static void accountForLatency() {

        int latency = getDbLatency();

        if (latency > 0) {
            long wait = lastWrote + latency - System.currentTimeMillis();

            if (wait > 0) {
                log.debug("LATENCY::need to account for latency - sleeping for " + wait + " ms");
                try {
                    Thread.sleep(Math.min(accountForLatency, wait));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static int getDbLatency() {
        if (accountForLatency == -1)  {
            String latencyInSecs = ConfigurationAwareClassLoader.getENV().getProperty( "db.latency.seconds" );
            if ( latencyInSecs != null ) {
                try {
                    accountForLatency = (Integer.parseInt( latencyInSecs ) * 1000);
                    if ( accountForLatency != 0 ) {
                        accountForLatency += 100;
                    }
                } catch ( NumberFormatException ee ) { /* do nothing */ }
            }
            if ( accountForLatency == -1
                 || (accountForLatency > 0 && accountForLatency < ACCOUNT_FOR_LATENCY_DEFAULT )) {
                accountForLatency = ACCOUNT_FOR_LATENCY_DEFAULT;
            }
            log.debug( "ACCOUNT_FOR_LATENCY = " + accountForLatency );
        }
        return accountForLatency;
    }
}
