package com.scaffold.framework.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import com.scaffold.framework.observability.domain.SlowRequest;
import com.scaffold.framework.observability.mapper.SlowRequestMapper;
import com.scaffold.framework.web.websocket.bus.MessagePublisher;

/**
 * Q-3 SlowApiAlertJob 单测：覆盖 6 个核心场景：
 * <ul>
 *   <li>禁用 → 直接返回 0，不查 DB</li>
 *   <li>无 pending → 返回 0，不发推送，不 mark</li>
 *   <li>publisher 不可用 → 仍要 mark（防死循环）</li>
 *   <li>多 reason 分组 → 各自一条 inbox，多个 recipient 各发一份</li>
 *   <li>样本截断 — 超过 MAX_SAMPLES_PER_NOTIFICATION 只取前 N 条</li>
 *   <li>parseRecipients 解析 — 空 / null / 多人逗号分隔</li>
 * </ul>
 */
class SlowApiAlertJobTest
{
    private SlowRequestMapper mapper;
    private MessagePublisher publisher;
    private ObjectProvider<MessagePublisher> publisherProvider;
    private ObservabilityProperties props;
    private SlowApiAlertJob job;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp()
    {
        mapper = mock(SlowRequestMapper.class);
        publisher = mock(MessagePublisher.class);
        publisherProvider = mock(ObjectProvider.class);
        when(publisherProvider.getIfAvailable()).thenReturn(publisher);
        props = new ObservabilityProperties();
        props.setEnabled(true);
        props.setAlertWindowMinutes(10);
        props.setAlertRecipients("admin");
        job = new SlowApiAlertJob(props, mapper, publisherProvider);
    }

    @Test
    void disabledReturnsZero()
    {
        props.setEnabled(false);
        int n = job.scanAndAlert();
        assertThat(n).isZero();
        verify(mapper, never()).selectPendingAlerts(any());
    }

    @Test
    void noPendingReturnsZeroAndDoesNothing()
    {
        when(mapper.selectPendingAlerts(any(Date.class))).thenReturn(List.of());
        int n = job.scanAndAlert();
        assertThat(n).isZero();
        verify(publisher, never()).toUser(anyString(), anyString(), any());
        verify(mapper, never()).markAlerted(anyList());
    }

    @Test
    void publisherUnavailableStillMarks()
    {
        when(publisherProvider.getIfAvailable()).thenReturn(null);
        SlowRequest r = newRecord(1L, SlowRequest.REASON_SLOW, 2500);
        when(mapper.selectPendingAlerts(any(Date.class))).thenReturn(List.of(r));

        int n = job.scanAndAlert();

        assertThat(n).isEqualTo(1);
        verify(publisher, never()).toUser(anyString(), anyString(), any());
        verify(mapper, times(1)).markAlerted(anyList());
    }

    @Test
    void groupsByReasonAndFansOutToRecipients()
    {
        props.setAlertRecipients("admin , ops, ");
        SlowRequest slow = newRecord(1L, SlowRequest.REASON_SLOW, 5000);
        SlowRequest err1 = newRecord(2L, SlowRequest.REASON_SERVER_ERROR, 100);
        SlowRequest err2 = newRecord(3L, SlowRequest.REASON_SERVER_ERROR, 200);
        when(mapper.selectPendingAlerts(any(Date.class))).thenReturn(List.of(slow, err1, err2));

        int n = job.scanAndAlert();

        assertThat(n).isEqualTo(3);
        // 2 reason × 2 recipient = 4 次 toUser
        verify(publisher, times(4)).toUser(anyString(), anyString(), any());
        verify(publisher, times(2)).toUser(eq("admin"), anyString(), any());
        verify(publisher, times(2)).toUser(eq("ops"), anyString(), any());
        verify(publisher).toUser(eq("admin"), eq(SlowApiAlertJob.TYPE_SLOW), any());
        verify(publisher).toUser(eq("admin"), eq(SlowApiAlertJob.TYPE_SERVER_ERROR), any());

        // 都 mark 一次（同一个 list）
        ArgumentCaptor<List<Long>> cap = ArgumentCaptor.forClass(List.class);
        verify(mapper, times(1)).markAlerted(cap.capture());
        assertThat(cap.getValue()).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void payloadKeepsOnlyTopNSamplesByCost()
    {
        List<SlowRequest> many = new ArrayList<>();
        for (int i = 0; i < SlowApiAlertJob.MAX_SAMPLES_PER_NOTIFICATION + 3; i++)
        {
            // 故意用乱序耗时
            many.add(newRecord(100L + i, SlowRequest.REASON_SLOW, (long) (i * 200)));
        }
        Map<String, Object> payload = SlowApiAlertJob.buildPayload(SlowRequest.REASON_SLOW, many);
        assertThat(payload).containsEntry("count", many.size());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> samples = (List<Map<String, Object>>) payload.get("samples");
        assertThat(samples).hasSize(SlowApiAlertJob.MAX_SAMPLES_PER_NOTIFICATION);
        // 第 0 条耗时一定 ≥ 第 N 条（按 cost 降序排）
        long first = (long) samples.get(0).get("costMs");
        long last = (long) samples.get(samples.size() - 1).get("costMs");
        assertThat(first).isGreaterThanOrEqualTo(last);
        assertThat(payload).containsKey("link");
    }

    @Test
    void parseRecipientsHandlesEmptyAndCsv()
    {
        assertThat(SlowApiAlertJob.parseRecipients(null)).containsExactly("admin");
        assertThat(SlowApiAlertJob.parseRecipients("")).containsExactly("admin");
        assertThat(SlowApiAlertJob.parseRecipients("  ,  ")).containsExactly("admin");
        assertThat(SlowApiAlertJob.parseRecipients("a,b ,c"))
                .containsExactly("a", "b", "c");
    }

    @Test
    void publisherExceptionDoesNotBlockMark()
    {
        SlowRequest r = newRecord(1L, SlowRequest.REASON_SLOW, 2500);
        when(mapper.selectPendingAlerts(any(Date.class))).thenReturn(List.of(r));
        doThrow(new RuntimeException("inbox down")).when(publisher).toUser(anyString(), anyString(), any());

        int n = job.scanAndAlert();

        assertThat(n).isEqualTo(1);
        verify(mapper, times(1)).markAlerted(anyList());
    }

    private SlowRequest newRecord(long id, String reason, long cost)
    {
        SlowRequest r = new SlowRequest();
        r.setId(id);
        r.setRequestUri("/api/test");
        r.setMethod("GET");
        r.setStatus(reason.equals(SlowRequest.REASON_SERVER_ERROR) ? 500 : 200);
        r.setCostMs(cost);
        r.setReason(reason);
        r.setCreateTime(new Date());
        r.setAlerted(SlowRequest.ALERTED_NO);
        return r;
    }
}
