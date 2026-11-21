package com.exemple.historique;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
@EnableFeignClients
public class HistoriqueServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(HistoriqueServiceApplication.class, args);
	}

	@Bean
	OpenAPI openAPI() {
		return new OpenAPI().servers(List.of(new Server().url("/")));
	}
}
