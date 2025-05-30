package com.variavel.sportsdataservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling

public class SportsDataServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SportsDataServiceApplication.class, args);
	}

}
