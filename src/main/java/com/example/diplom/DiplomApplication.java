package com.example.diplom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DiplomApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiplomApplication.class, args);
	}

}
