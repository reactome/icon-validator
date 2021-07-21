package org.reactome.server.tools;

import java.time.Duration;
import java.time.Instant;

public interface Checker extends Runnable {
    @Override
    default void run() {
        Instant start = Instant.now();
        process();
        System.out.println(this.getClass().getSimpleName() + " finished in " + Duration.between(start, Instant.now()).toString().substring(2));
    }

    void process();

    int getFailedChecks();

    int getTotalChecks();
}
