
package com.havana.harness.context;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class ContextBuilderConfig {

    @Bean
    public ContextBuilder contextBuilder() {
        Path steeringPath = Path.of("..", ".kiro", "steering")
                .toAbsolutePath()
                .normalize();

        return new ContextBuilder(steeringPath);
    }
}
