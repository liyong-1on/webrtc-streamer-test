package com.example.webrtctest;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.example.webrtctest.mapper")
public class WebrtcTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebrtcTestApplication.class, args);
    }

}
