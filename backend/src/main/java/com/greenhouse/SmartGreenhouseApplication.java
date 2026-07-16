package com.greenhouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * 智慧大棚AIoT系统 — 后端启动类
 */
@SpringBootApplication
@EnableAutoConfiguration(exclude = {
    org.springframework.ai.autoconfigure.vectorstore.chroma.ChromaVectorStoreAutoConfiguration.class
})
public class SmartGreenhouseApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartGreenhouseApplication.class, args);
    }
}
