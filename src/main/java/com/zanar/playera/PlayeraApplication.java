package com.zanar.playera;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.zanar.playera.repo")
@EnableMethodSecurity
@EnableScheduling
public class PlayeraApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlayeraApplication.class, args);
	}

}
