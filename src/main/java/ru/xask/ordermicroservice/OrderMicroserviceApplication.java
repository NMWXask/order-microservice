package ru.xask.ordermicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
public class OrderMicroserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderMicroserviceApplication.class, args);
    }

}
