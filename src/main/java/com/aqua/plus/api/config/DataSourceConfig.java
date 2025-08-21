package com.aqua.plus.api.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.aqua.plus.api.utils.EncriptarDesencriptar;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

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
	
	@Value("${spring.datasource.hikari.maximum-pool-size}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle}")
    private int minIdle;

    @Value("${spring.datasource.hikari.idle-timeout}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.connection-timeout}")
    private long connectionTimeout;
	
	private final EncriptarDesencriptar encriptarDesencriptar;
	
	@Bean
	public DataSource dataSource() {
	    HikariConfig config = new HikariConfig();
	    config.setDriverClassName(driver);
	    config.setJdbcUrl(url);
	    config.setUsername(username);
	    config.setPassword(encriptarDesencriptar.desencriptar(password));
	    
	    config.setMaximumPoolSize(maxPoolSize);
	    config.setMinimumIdle(minIdle);
	    config.setIdleTimeout(idleTimeout);
	    config.setMaxLifetime(maxLifetime);
	    config.setConnectionTimeout(connectionTimeout);
	    config.setPoolName("HikariPoolAquaPlus");

	    return new HikariDataSource(config);
	}
}
