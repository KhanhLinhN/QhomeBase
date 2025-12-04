package com.QhomeBase.datadocsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;

@SpringBootApplication(exclude = {RedisAutoConfiguration.class})
public class DataDocsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataDocsServiceApplication.class, args);
    }

}
