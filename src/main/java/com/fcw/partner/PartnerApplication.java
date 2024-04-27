package com.fcw.partner;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.fcw.partner.mapper")
@EnableScheduling
public class PartnerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PartnerApplication.class, args);
    }

}
