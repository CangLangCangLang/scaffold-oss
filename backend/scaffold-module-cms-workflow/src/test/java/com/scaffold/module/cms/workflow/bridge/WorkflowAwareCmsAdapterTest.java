package com.scaffold.module.cms.workflow.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.module.cms.domain.Article;
import com.scaffold.module.cms.mapper.ArticleMapper;
import com.scaffold.module.workflow.dto.ProcessInstanceView;
import com.scaffold.module.workflow.dto.StartProcessRequest;
import com.scaffold.module.workflow.service.WorkflowFacade;

/**
 * 验证桥模块 {@link WorkflowAwareCmsAdapter} 的核心契约：
 * <ul>
 *   <li>onSubmit 启动正确的流程定义 + businessKey + 关键变量</li>
 *   <li>onSubmit 返回 true（让 ArticleService 跳过自闭环）</li>
 *   <li>onSubmit 把 piid 回写到 cms_article 表</li>
 *   <li>onSubmit 把 article.status 切到 PENDING</li>
 *   <li>onApprove / onReject 触发 cancelInstance（前端绕过 workflow 直接通过/驳回时清理引擎）</li>
 *   <li>文章不存在时抛错</li>
 * </ul>
 */
class WorkflowAwareCmsAdapterTest
{
    private WorkflowFacade workflowFacade;
    private ArticleMapper articleMapper;
    private WorkflowAwareCmsAdapter adapter;

    @BeforeEach
    void setUp()
    {
        workflowFacade = mock(WorkflowFacade.class);
        articleMapper = mock(ArticleMapper.class);

        adapter = new WorkflowAwareCmsAdapter();
        org.springframework.test.util.ReflectionTestUtils.setField(adapter, "workflowFacade", workflowFacade);
        org.springframework.test.util.ReflectionTestUtils.setField(adapter, "articleMapper", articleMapper);
    }

    private static Article article(Long id, String status, String piid)
    {
        Article a = new Article();
        a.setId(id);
        a.setTitle("文章 " + id);
        a.setStatus(status);
        a.setProcessInstanceId(piid);
        a.setCreateBy("alice");
        return a;
    }

    private static ProcessInstanceView piView(String piid)
    {
        ProcessInstanceView v = new ProcessInstanceView();
        v.setId(piid);
        return v;
    }

    @Test
    void onSubmitStartsProcessAndStampsPiidAndPending()
    {
        Article draft = article(1L, Article.STATUS_DRAFT, null);
        when(articleMapper.selectById(eq(1L))).thenReturn(draft);
        when(workflowFacade.startProcess(any(StartProcessRequest.class), eq("alice"))).thenReturn(piView("piid-001"));

        boolean handed = adapter.onSubmit(1L, "alice");

        assertThat(handed).isTrue();

        // 校验启动流程参数
        ArgumentCaptor<StartProcessRequest> cap = ArgumentCaptor.forClass(StartProcessRequest.class);
        verify(workflowFacade).startProcess(cap.capture(), eq("alice"));
        StartProcessRequest req = cap.getValue();
        assertThat(req.getProcessDefinitionKey()).isEqualTo(WorkflowAwareCmsAdapter.DEF_KEY);
        assertThat(req.getBusinessKey()).isEqualTo("1");
        assertThat(req.getVariables())
                .containsEntry(WorkflowAwareCmsAdapter.VAR_ARTICLE_ID, 1L)
                .containsEntry(WorkflowAwareCmsAdapter.VAR_AUTHOR, "alice")
                .containsEntry(WorkflowAwareCmsAdapter.VAR_SUBMITTER, "alice");

        verify(articleMapper).updateProcessInstanceId(eq(1L), eq("piid-001"));
        verify(articleMapper).updateStatus(eq(1L), eq(Article.STATUS_PENDING), any(), eq("alice"));
    }

    @Test
    void onSubmitDoesNotStartIfArticleAlreadyHasPiid()
    {
        Article inFlight = article(2L, Article.STATUS_DRAFT, "piid-existing");
        when(articleMapper.selectById(eq(2L))).thenReturn(inFlight);

        boolean handed = adapter.onSubmit(2L, "alice");
        assertThat(handed).isTrue();

        verify(workflowFacade, never()).startProcess(any(), anyString());
        verify(articleMapper, never()).updateProcessInstanceId(any(), any());
    }

    @Test
    void onSubmitFailsLoudlyIfStartProcessThrows()
    {
        Article draft = article(3L, Article.STATUS_DRAFT, null);
        when(articleMapper.selectById(eq(3L))).thenReturn(draft);
        when(workflowFacade.startProcess(any(), anyString())).thenThrow(new RuntimeException("flowable boom"));

        // 桥模块设计上：失败应该抛错让前端可见，避免"流程没起来但状态已变 PENDING"的诡异状态
        assertThatThrownBy(() -> adapter.onSubmit(3L, "alice"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("启动审批流程失败");

        verify(articleMapper, never()).updateProcessInstanceId(any(), any());
        verify(articleMapper, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    void onSubmitFailsIfArticleNotFound()
    {
        when(articleMapper.selectById(any())).thenReturn(null);

        assertThatThrownBy(() -> adapter.onSubmit(99L, "alice"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("文章不存在");
    }

    @Test
    void onApproveCancelsLinkedFlowableInstance()
    {
        Article a = article(4L, Article.STATUS_PENDING, "piid-4");
        when(articleMapper.selectById(eq(4L))).thenReturn(a);

        adapter.onApprove(4L);

        verify(workflowFacade, times(1)).cancelInstance(eq("piid-4"), anyString());
    }

    @Test
    void onRejectCancelsLinkedFlowableInstanceAndPropagatesReason()
    {
        Article a = article(5L, Article.STATUS_PENDING, "piid-5");
        when(articleMapper.selectById(eq(5L))).thenReturn(a);

        adapter.onReject(5L, "标题不合规");

        ArgumentCaptor<String> reasonCap = ArgumentCaptor.forClass(String.class);
        verify(workflowFacade).cancelInstance(eq("piid-5"), reasonCap.capture());
        assertThat(reasonCap.getValue()).contains("标题不合规");
    }

    @Test
    void onApproveIsNoOpWhenArticleHasNoLinkedInstance()
    {
        Article a = article(6L, Article.STATUS_PENDING, null);
        when(articleMapper.selectById(eq(6L))).thenReturn(a);

        adapter.onApprove(6L);
        verify(workflowFacade, never()).cancelInstance(anyString(), anyString());
    }

    @Test
    void onApproveSwallowsCancelExceptionToAvoidBreakingMainFlow()
    {
        Article a = article(7L, Article.STATUS_PENDING, "piid-7");
        when(articleMapper.selectById(eq(7L))).thenReturn(a);
        org.mockito.Mockito.doThrow(new RuntimeException("already ended"))
                .when(workflowFacade).cancelInstance(eq("piid-7"), anyString());

        // 不应抛错，仅 warn log
        adapter.onApprove(7L);
        verify(workflowFacade, atLeastOnce()).cancelInstance(eq("piid-7"), anyString());
    }
}
