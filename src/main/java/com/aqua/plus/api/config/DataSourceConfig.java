package com.aqua.plus.api.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.aqua.plus.api.utils.EncriptarDesencriptar;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

	@Value("${spring.datasource.url}")
	private String url;

	@Value("${spring.datasource.username}")
	private String username;

	@Value("${spring.datasource.password}") 
	private String password;
	
	@Value("${spring.datasource.driver-class-name}") 
	private String driver;
	
	private final EncriptarDesencriptar encriptarDesencriptar;
	
	@Bean
    public DataSource dataSource() {

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(this.driver);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(this.encriptarDesencriptar.desencriptar(password));

        return dataSource;
    }
}
