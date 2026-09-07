package com.shiptrack.shiptrackpro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ShiptrackProApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShiptrackProApplication.class, args);
    }
}
