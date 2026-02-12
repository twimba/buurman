package com.buurman.config;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;

import static org.jooq.impl.DSL.using;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;

@Configuration
public class JooqConfig {

    @Bean
    public DSLContext dslContext(DataSource dataSource) {
        DefaultConfiguration configuration = new DefaultConfiguration();
        configuration.set(new TransactionAwareDataSourceProxy(dataSource));
        configuration.set(SQLDialect.POSTGRES);

        return using(configuration);
    }
}
