package com.inspien.eai.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@Configuration
@EnableScheduling
public class EaiConfig {

    @Primary
    @Bean
    public DataSource oracleDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driver-class-name}") String driverClassName) {
        return DataSourceBuilder.create()
                .url(url).username(username).password(password).driverClassName(driverClassName)
                .build();
    }

    @Primary
    @Bean
    public JdbcTemplate jdbcTemplate(@Qualifier("oracleDataSource") DataSource oracleDataSource) {
        return new JdbcTemplate(oracleDataSource);
    }

    @Primary
    @Bean
    public PlatformTransactionManager transactionManager(@Qualifier("oracleDataSource") DataSource oracleDataSource) {
        return new DataSourceTransactionManager(oracleDataSource);
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    public DataSource localDataSource(@Value("${eai.local-db.url}") String url) {
        DataSource ds = DataSourceBuilder.create()
                .url(url)
                .driverClassName("org.h2.Driver")
                .build();

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("local-schema.sql"));
        populator.setContinueOnError(true);
        DatabasePopulatorUtils.execute(populator, ds);

        return ds;
    }

    @Bean
    public JdbcTemplate localJdbcTemplate(@Qualifier("localDataSource") DataSource localDataSource) {
        return new JdbcTemplate(localDataSource);
    }
}
