package com.scaffold.module.cms.workflow.bridge;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.flowable.engine.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * 启动时自动部署 cms_article_review 流程定义。<br>
 * 部署策略：
 * <ul>
 *   <li>资源文件：{@code classpath:processes/cms_article_review.bpmn20.xml}</li>
 *   <li>启动时按 key 查最新版本，对比当前 BPMN 内容的 MD5 摘要</li>
 *   <li>引擎中无该 key，或 key 存在但 MD5 不一致 → 部署新版本</li>
 *   <li>MD5 一致 → 跳过</li>
 * </ul>
 *
 * <p>这样做的好处：
 * <ol>
 *   <li>头一次启动桥模块自动建好流程，不需要运维去 workflow 设计器手动部署</li>
 *   <li>桥模块自带的 BPMN 升级时（修复审核节点 candidateGroups 等）自动跟随</li>
 *   <li>用户在 workflow 设计器手动覆盖了同 key 的 xml 后，启动时桥模块只会替换它（如果内容不同）；
 *       用户不希望桥模块覆盖自己的版本时，可以把 BPMN 内容（包括 dummy 注释）改成与桥模块自带的一致即可，
 *       或干脆关闭 cms-workflow 桥（{@code app.module.cms.workflow.enabled=false}）</li>
 * </ol>
 */
@Component
public class ArticleProcessDeployer
{
    private static final Logger log = LoggerFactory.getLogger(ArticleProcessDeployer.class);

    private static final String RESOURCE = "processes/cms_article_review.bpmn20.xml";
    private static final String DEPLOYMENT_NAME_PREFIX = "cms-bridge:" + WorkflowAwareCmsAdapter.DEF_KEY + ":";

    private final RepositoryService repositoryService;

    public ArticleProcessDeployer(RepositoryService repositoryService)
    {
        this.repositoryService = repositoryService;
    }

    /** 用 ApplicationReadyEvent 而不是 PostConstruct：保证 Flowable 引擎已初始化，repositoryService 可用 */
    @EventListener(ApplicationReadyEvent.class)
    public void deployIfNeeded()
    {
        byte[] bytes;
        try
        {
            bytes = readResource();
        }
        catch (IOException ex)
        {
            log.error("读取 BPMN 资源失败 path={} reason={}", RESOURCE, ex.getMessage(), ex);
            return;
        }
        String md5 = md5(bytes);
        String deploymentName = DEPLOYMENT_NAME_PREFIX + md5;

        // 已经按当前 md5 部署过 → 跳过
        long existing = repositoryService.createDeploymentQuery()
                .deploymentName(deploymentName).count();
        if (existing > 0)
        {
            log.info("cms_article_review 流程已是最新版本 (md5={}), 跳过部署", md5);
            return;
        }

        try
        {
            repositoryService.createDeployment()
                    .name(deploymentName)
                    .key(WorkflowAwareCmsAdapter.DEF_KEY)
                    .addInputStream("cms_article_review.bpmn20.xml", new ByteArrayInputStream(bytes))
                    .deploy();
            log.info("已部署 cms_article_review 流程 (md5={})", md5);
        }
        catch (Exception ex)
        {
            log.error("部署 cms_article_review 流程失败 reason={}", ex.getMessage(), ex);
        }
    }

    private byte[] readResource() throws IOException
    {
        try (InputStream is = new ClassPathResource(RESOURCE).getInputStream())
        {
            return StreamUtils.copyToByteArray(is);
        }
    }

    private static String md5(byte[] bytes)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] dig = md.digest(bytes);
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        }
        catch (NoSuchAlgorithmException ex)
        {
            // JDK 总是提供 MD5；理论上不可能到这
            return Integer.toHexString(java.util.Arrays.hashCode(bytes));
        }
    }
}
