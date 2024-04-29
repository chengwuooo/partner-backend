package com.fcw.partner.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {
    private String host;
    private int port;
    @Bean
    public RedissonClient redissionClient() {
        // 1. Create config object
        Config config = new Config();

        String redisUrl = String.format("redis://%s:%d", host, port);
        config.useSingleServer().setAddress(redisUrl).setDatabase(3);

        // 2. Create Redisson instance
        // Sync and Async API
        RedissonClient redisson = Redisson.create(config);

        return redisson;
    }
}
