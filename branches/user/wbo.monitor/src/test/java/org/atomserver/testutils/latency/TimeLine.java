package org.atomserver.testutils.latency;

import org.apache.log4j.Logger;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class TimeLine {
    private static final Logger log = Logger.getLogger(TimeLine.class);

    private CyclicBarrier cyclicBarrier;
    private AtomicInteger t = new AtomicInteger(0);
    private long time;

    public TimeLine(final int timeStep,
                    final int actors,
                    final Runnable barrierAction) throws Exception {
        this.cyclicBarrier = new CyclicBarrier(
                actors,
                new Runnable() {
                    public void run() {
                        log.debug("timeline @ t" + t.getAndIncrement());
                        long spent = System.currentTimeMillis() - time;
                        if (barrierAction != null) {
                            barrierAction.run();
                        }
                        if (spent < timeStep) {
                            try {
                                Thread.sleep(timeStep - spent);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        time = System.currentTimeMillis();
                    }
                });
        time = System.currentTimeMillis();
    }

    public void tick() throws Exception {
        this.cyclicBarrier.await();
    }
}
