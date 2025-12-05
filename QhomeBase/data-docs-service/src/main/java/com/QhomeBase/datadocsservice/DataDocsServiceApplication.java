package com.QhomeBase.datadocsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DataDocsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataDocsServiceApplication.class, args);
    }

}
