package com.scaffold.module.inbox.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.framework.web.websocket.bus.PushMessage;
import com.scaffold.module.inbox.domain.MessageInboxEntry;
import com.scaffold.module.inbox.dto.InboxQueryRequest;
import com.scaffold.module.inbox.mapper.MessageInboxMapper;

class MessageInboxServiceTest
{
    private MessageInboxMapper mapper;
    private MessageInboxService service;

    @BeforeEach
    void setUp()
    {
        mapper = mock(MessageInboxMapper.class);
        service = new MessageInboxService(mapper, new ObjectMapper());
        ReflectionTestUtils.setField(service, "persist", true);
        ReflectionTestUtils.setField(service, "defaultTtlSeconds", 3600L);
        lenient().when(mapper.insert(any())).thenAnswer(inv -> {
            MessageInboxEntry e = inv.getArgument(0);
            e.setId(1L);
            return 1;
        });
    }

    @Test
    void persistUserMessageInsertsRow()
    {
        PushMessage msg = PushMessage.toUser("alice", "notice", "m-1", "hello");
        Long id = service.persistIfEnabled(msg);
        assertThat(id).isEqualTo(1L);
        verify(mapper, times(1)).insert(any());
    }

    @Test
    void persistTopicMessageIsSkipped()
    {
        PushMessage msg = PushMessage.toTopic("ops", "alert", "m-2", "fire");
        Long id = service.persistIfEnabled(msg);
        assertThat(id).isNull();
        verify(mapper, never()).insert(any());
    }

    @Test
    void persistDisabledFlagShortCircuits()
    {
        ReflectionTestUtils.setField(service, "persist", false);
        Long id = service.persistIfEnabled(PushMessage.toUser("alice", "notice", "m-3", "x"));
        assertThat(id).isNull();
        verify(mapper, never()).insert(any());
    }

    @Test
    void ackDelegatesToMapper()
    {
        when(mapper.markRead(eq(7L), eq("alice"))).thenReturn(1);
        assertThat(service.ack(7L, "alice")).isTrue();
    }

    @Test
    void cleanupExpiredCombinesMarkAndDelete()
    {
        when(mapper.expireBeforeNow()).thenReturn(2);
        when(mapper.deleteExpired(anyInt())).thenReturn(3);
        assertThat(service.cleanupExpired(30)).isEqualTo(5);
    }

    // ========== P2 全页面分页 / 批量操作 ==========

    @Test
    void pageReturnsEmptyWhenUsernameMissing()
    {
        InboxQueryRequest req = new InboxQueryRequest();
        MessageInboxService.PageResult<MessageInboxEntry> result = service.page(null, req);
        assertThat(result.getRows()).isEmpty();
        assertThat(result.getTotal()).isZero();
        verify(mapper, never()).countPageByUser(anyString(), anyList(), anyString(), any(), any());
    }

    @Test
    void pageShortCircuitsWhenCountIsZero()
    {
        // total=0 时不应再发 selectPage 查询，节省一次 SQL
        when(mapper.countPageByUser(eq("alice"), any(), any(), any(), any())).thenReturn(0);
        InboxQueryRequest req = new InboxQueryRequest();
        MessageInboxService.PageResult<MessageInboxEntry> result = service.page("alice", req);
        assertThat(result.getRows()).isEmpty();
        assertThat(result.getTotal()).isZero();
        verify(mapper, never()).selectPageByUser(anyString(), anyList(), anyString(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void pageDelegatesAndComputesOffset()
    {
        when(mapper.countPageByUser(eq("alice"), any(), any(), any(), any())).thenReturn(35);
        MessageInboxEntry row = new MessageInboxEntry();
        row.setId(99L);
        when(mapper.selectPageByUser(eq("alice"), any(), any(), any(), any(), eq(20), eq(10)))
                .thenReturn(Arrays.asList(row));

        InboxQueryRequest req = new InboxQueryRequest();
        req.setPageNum(3);
        req.setPageSize(10);
        MessageInboxService.PageResult<MessageInboxEntry> result = service.page("alice", req);

        assertThat(result.getTotal()).isEqualTo(35L);
        assertThat(result.getRows()).hasSize(1);
        assertThat(result.getRows().get(0).getId()).isEqualTo(99L);
    }

    @Test
    void ackBatchSkipsEmptyIds()
    {
        assertThat(service.ackBatch("alice", Collections.emptyList())).isZero();
        assertThat(service.ackBatch("alice", null)).isZero();
        assertThat(service.ackBatch(null, Arrays.asList(1L))).isZero();
        verify(mapper, never()).markBatchReadByIds(anyString(), anyList());
    }

    @Test
    void ackBatchDelegatesToMapper()
    {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(mapper.markBatchReadByIds("alice", ids)).thenReturn(2);
        assertThat(service.ackBatch("alice", ids)).isEqualTo(2);
    }

    @Test
    void removeBatchSkipsEmptyIds()
    {
        assertThat(service.removeBatch("alice", Collections.emptyList())).isZero();
        verify(mapper, never()).deleteBatchByIds(anyString(), anyList());
    }

    @Test
    void removeBatchDelegatesToMapper()
    {
        List<Long> ids = Arrays.asList(1L, 2L);
        when(mapper.deleteBatchByIds("alice", ids)).thenReturn(2);
        assertThat(service.removeBatch("alice", ids)).isEqualTo(2);
    }

    @Test
    void removeOneRequiresBothFields()
    {
        assertThat(service.removeOne(null, 1L)).isFalse();
        assertThat(service.removeOne("alice", null)).isFalse();
        verify(mapper, never()).deleteByIdAndUser(any(), anyString());
    }

    @Test
    void removeOneTrueWhenAffected()
    {
        when(mapper.deleteByIdAndUser(7L, "alice")).thenReturn(1);
        assertThat(service.removeOne("alice", 7L)).isTrue();
    }

    @Test
    void queryRequestClampsPageSize()
    {
        // 防御性边界：>100 应被夹到 100，<1 / null 应回落 10
        InboxQueryRequest req = new InboxQueryRequest();
        req.setPageSize(999);
        assertThat(req.getPageSize()).isEqualTo(100);

        req.setPageSize(0);
        assertThat(req.getPageSize()).isEqualTo(10);

        req.setPageNum(-5);
        assertThat(req.getPageNum()).isEqualTo(1);
    }

    @Test
    void queryRequestNormalizesBlankTypeKeyword()
    {
        InboxQueryRequest req = new InboxQueryRequest();
        req.setTypeKeyword("   ");
        assertThat(req.getTypeKeyword()).isNull();
        req.setTypeKeyword(" cms.article  ");
        assertThat(req.getTypeKeyword()).isEqualTo("cms.article");
    }
}
