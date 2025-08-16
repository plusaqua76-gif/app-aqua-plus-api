package com.aqua.plus.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@EnableAutoConfiguration(exclude = { JpaRepositoriesAutoConfiguration.class })
@EntityScan(basePackages = { "com.codemakers.commons.entities" })
@EnableJpaRepositories(basePackages = { "com.codemakers.commons.repositories" })
@EnableJpaAuditing
@ComponentScan(basePackages = {"com.codemakers.api", "com.codemakers.commons"})
public class AquaPlusApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AquaPlusApiApplication.class, args);
	}
}
