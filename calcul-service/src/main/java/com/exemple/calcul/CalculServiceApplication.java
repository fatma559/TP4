package com.exemple.calcul;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class CalculServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CalculServiceApplication.class, args);
	}

	// Swagger envoie ses requêtes à l'adresse de la page (la passerelle)
	@Bean
	OpenAPI openAPI() {
		return new OpenAPI().servers(List.of(new Server().url("/")));
	}
}