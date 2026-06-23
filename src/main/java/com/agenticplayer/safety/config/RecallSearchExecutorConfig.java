package com.agenticplayer.safety.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecallSearchExecutorConfig {

    @Bean(name = "recallSearchExecutor", destroyMethod = "shutdown")
    ExecutorService recallSearchExecutor() {
        AtomicInteger sequence = new AtomicInteger();
        return Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "recall-search-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }
}
