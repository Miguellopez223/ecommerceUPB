package com.upb.ecommerce.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

// --- PREGUNTA 5 ---
@Configuration
@EnableAsync
public class AsyncConfig {

    // --- PREGUNTA 5 ---
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);        // hilos mínimos siempre vivos
        executor.setMaxPoolSize(5);         // hilos máximos bajo carga
        executor.setQueueCapacity(50);      // tareas en espera antes de crear más hilos
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
