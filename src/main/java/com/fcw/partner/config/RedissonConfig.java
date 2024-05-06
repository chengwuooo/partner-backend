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

    //线上环境所需
    private String password;

    @Bean
    public RedissonClient redissonClient() {
        // 1. Create config object
        Config config = new Config();

        // 2. Create Redisson instance
        // Sync and Async API

        String redisAddress = String.format("redis://%s:%s", host, port);

        //设置参数（生产环境和测试环境参数不同，注意修改）
        config.useSingleServer()
                .setAddress(redisAddress)
                .setDatabase(3)
                .setPassword(password);
        //2.创建实例
        RedissonClient redissonClient = Redisson.create(config);

        System.out.println("RedissonClient初始化成功");
        return redissonClient;

    }
}
