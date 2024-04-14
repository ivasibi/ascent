package org.ascent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class AscentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AscentApplication.class, args);
    }
}