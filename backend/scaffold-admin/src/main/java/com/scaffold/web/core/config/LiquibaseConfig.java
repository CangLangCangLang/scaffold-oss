package com.scaffold.web.core.config;

import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.integration.spring.SpringResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Liquibase 配置：绑定主 Druid 数据源，并提供启动期 validate 门控。
 */
@Configuration
public class LiquibaseConfig
{
    @Bean
    @ConditionalOnProperty(prefix = "spring.liquibase", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SpringLiquibase liquibase(
            @Qualifier("masterDataSource") DataSource dataSource,
            @Value("${spring.liquibase.change-log:classpath:/db/changelog/db.changelog-master.yml}") String changeLog)
    {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(changeLog);
        return liquibase;
    }

    /**
     * Liquibase changelog 校验器。
     * <p>
     * 通过 {@code spring.liquibase.validate-on-startup=true} 启用；
     * 校验失败时根据 {@code halt-on-validation-failure} 决定是否终止启动。
     */
    @Component
    @ConditionalOnProperty(prefix = "spring.liquibase", name = "validate-on-startup", havingValue = "true")
    public static class LiquibaseValidator
    {
        private static final Logger log = LoggerFactory.getLogger(LiquibaseValidator.class);

        private final DataSource dataSource;
        private final ResourceLoader resourceLoader;
        private final String changelog;
        private final boolean haltOnFailure;

        public LiquibaseValidator(
                @Qualifier("masterDataSource") DataSource dataSource,
                ResourceLoader resourceLoader,
                @Value("${spring.liquibase.change-log:classpath:/db/changelog/db.changelog-master.yml}") String changelog,
                @Value("${spring.liquibase.halt-on-validation-failure:true}") boolean haltOnFailure)
        {
            this.dataSource = dataSource;
            this.resourceLoader = resourceLoader;
            this.changelog = changelog.replace("classpath:", "");
            this.haltOnFailure = haltOnFailure;
        }

        @EventListener(ApplicationReadyEvent.class)
        public void validateOnReady()
        {
            try
            {
                Database database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(dataSource.getConnection()));
                try (Liquibase liquibase = new Liquibase(
                        changelog,
                        new SpringResourceAccessor(resourceLoader),
                        database))
                {
                    liquibase.validate();
                    liquibase.listUnrunChangeSets(new Contexts(), new LabelExpression());
                }
                log.info("Liquibase 校验通过：{}", changelog);
            }
            catch (Throwable t)
            {
                String msg = "Liquibase 校验失败：" + t.getMessage();
                if (haltOnFailure)
                {
                    log.error(msg, t);
                    throw new IllegalStateException(msg, t);
                }
                log.warn(msg);
            }
        }
    }
}
