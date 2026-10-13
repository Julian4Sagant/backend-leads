package com.sagant.leads.leads_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BackendLeadsApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendLeadsApplication.class, args);
	}

}
