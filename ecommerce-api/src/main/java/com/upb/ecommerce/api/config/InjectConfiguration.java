package com.upb.ecommerce.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@EnableAsync          // habilita los métodos @Async (trabajadores en paralelo)
@EnableScheduling     // habilita las tareas @Scheduled (barrido programado)
@EnableJpaAuditing    // habilita @LastModifiedDate en las entidades (fechaActualizacion)
public class InjectConfiguration {

    @Value("${async.core-pool-size:5}")
    private int corePoolSize;

    @Value("${async.max-pool-size:5}")
    private int maxPoolSize;

    @Value("${async.queue-capacity:50}")
    private int queueCapacity;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** Pool de hilos que ejecuta los métodos {@code @Async} (p. ej. el envío de correos del barrido). */
    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
