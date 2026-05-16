package com.scaffold;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 启动程序
 * <p>
 * 把 {@code com.scaffold.module.*} 从默认 ComponentScan 中排除，让每个业务模块的
 * AutoConfiguration（带 {@code @ConditionalOnProperty(prefix=app.module.<name>)}）
 * 自行决定是否把组件拉进容器：
 * <ul>
 *   <li>jar 在 + enabled=true  → 模块组件全部注册</li>
 *   <li>jar 在 + enabled=false → AutoConfig 跳过，主扫描排除，0 组件加载</li>
 *   <li>jar 不在               → AutoConfig 类不存在，0 组件加载</li>
 * </ul>
 *
 * @author scaffold
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@ComponentScan(
    basePackages = "com.scaffold",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.scaffold\\.module\\..*"
    )
)
public class ScaffoldApplication
{
    public static void main(String[] args)
    {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(ScaffoldApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  脚手架启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " .-------.       ____     __        \n" +
                " |  _ _   \\      \\   \\   /  /    \n" +
                " | ( ' )  |       \\  _. /  '       \n" +
                " |(_ o _) /        _( )_ .'         \n" +
                " | (_,_).' __  ___(_ o _)'          \n" +
                " |  |\\ \\  |  ||   |(_,_)'         \n" +
                " |  | \\ `'   /|   `-'  /           \n" +
                " |  |  \\    /  \\      /           \n" +
                " ''-'   `'-'    `-..-'              ");
    }
}
