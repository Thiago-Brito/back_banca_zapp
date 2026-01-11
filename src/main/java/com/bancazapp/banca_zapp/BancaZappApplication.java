package com.bancazapp.banca_zapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BancaZappApplication {

	public static void main(String[] args) {
		SpringApplication.run(BancaZappApplication.class, args);
	}

}
