package com.flowerbed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EmotionFlowerbedApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmotionFlowerbedApiApplication.class, args);
    }

}
