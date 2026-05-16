package com.scaffold.framework.config.properties;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 启动期凭据守卫。
 * <p>
 * 当 spring.profiles.active 包含 {@code prod} 时，扫描以下危险默认值：
 * <ul>
 *   <li>token.secret 长度过短或仍为示例值 abcdefghijklmnopqrstuvwxyz</li>
 *   <li>spring.datasource.druid.master.password / spring.data.redis.password 仍为弱默认（password、123456）</li>
 *   <li>spring.datasource.druid.statViewServlet.login-username/password 仍为示例值</li>
 * </ul>
 * 一旦命中将抛出 {@link IllegalStateException}，让应用立即停止启动而非进入危险状态。
 *
 * @author scaffold
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class SecretGuardEnvironmentPostProcessor implements EnvironmentPostProcessor
{
    private static final int MIN_JWT_SECRET_BYTES = 32;

    private static final String EXAMPLE_JWT_SECRET = "abcdefghijklmnopqrstuvwxyz";

    private static final List<String> WEAK_PASSWORDS = Arrays.asList(
            "123456", "password", "scaffold", "admin", "root", "");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application)
    {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean prodActive = false;
        for (String profile : activeProfiles)
        {
            if ("prod".equalsIgnoreCase(profile))
            {
                prodActive = true;
                break;
            }
        }
        if (!prodActive)
        {
            return;
        }

        Map<String, String> failures = new LinkedHashMap<>();

        String jwtSecret = environment.getProperty("token.secret", "");
        if (EXAMPLE_JWT_SECRET.equals(jwtSecret))
        {
            failures.put("token.secret", "仍为示例值，请通过环境变量 JWT_SECRET 注入随机密钥");
        }
        else if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_JWT_SECRET_BYTES)
        {
            failures.put("token.secret",
                    "长度不足 " + MIN_JWT_SECRET_BYTES + " 字节，建议生成 ≥ 64 字节随机字符串");
        }

        String dbPassword = environment.getProperty("spring.datasource.druid.master.password", "");
        if (WEAK_PASSWORDS.contains(dbPassword))
        {
            failures.put("spring.datasource.druid.master.password",
                    "仍为弱默认值，请通过环境变量 DB_PASSWORD 注入强密码");
        }

        String redisPassword = environment.getProperty("spring.data.redis.password", "");
        if (WEAK_PASSWORDS.contains(redisPassword))
        {
            failures.put("spring.data.redis.password",
                    "仍为弱默认值，请通过环境变量 REDIS_PASSWORD 注入强密码（如 Redis 已开启鉴权）");
        }

        boolean druidStatEnabled = environment.getProperty(
                "spring.datasource.druid.statViewServlet.enabled", Boolean.class, false);
        if (druidStatEnabled)
        {
            String druidUser = environment.getProperty(
                    "spring.datasource.druid.statViewServlet.login-username", "");
            String druidPwd = environment.getProperty(
                    "spring.datasource.druid.statViewServlet.login-password", "");
            if ("scaffold".equals(druidUser) || WEAK_PASSWORDS.contains(druidPwd))
            {
                failures.put("spring.datasource.druid.statViewServlet",
                        "Druid 监控控制台仍为弱默认凭据，请通过 DRUID_USERNAME / DRUID_PASSWORD 修改或关闭 DRUID_STAT_VIEW_ENABLED");
            }
        }

        if (!failures.isEmpty())
        {
            StringBuilder sb = new StringBuilder();
            sb.append("\n========== 启动期凭据守卫拒绝启动 ==========\n");
            sb.append("当前 profile=prod，但以下配置仍为脚手架默认值，存在严重安全风险：\n");
            failures.forEach((k, v) -> sb.append("  - ").append(k).append(": ").append(v).append('\n'));
            sb.append("请修复后再启动；如需在生产复现该模式，请新建 profile（如 prod-internal）并显式承担风险。\n");
            sb.append("=========================================");
            throw new IllegalStateException(sb.toString());
        }
    }
}
