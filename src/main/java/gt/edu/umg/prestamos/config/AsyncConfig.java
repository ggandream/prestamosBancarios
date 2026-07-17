package gt.edu.umg.prestamos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Habilita el procesamiento asíncrono de eventos ({@code @Async @EventListener}) con
 * un pool simple y acotado, suficiente para la carga del proyecto (Sección 8 del
 * CLAUDE.md: no sobre-ingenierizar el executor).
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("prestamos-async-");
        executor.initialize();
        return executor;
    }
}
