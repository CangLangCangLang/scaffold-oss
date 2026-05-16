package com.scaffold.framework.web.websocket.bus;

/**
 * 推送总线在 fan-out 之前的"可选记录器"扩展点。
 * <p>
 * 默认没有任何实现 → 总线行为不变。引入 {@code scaffold-module-inbox} 后由
 * {@code MessageInboxService} 提供实现，把每条 USER 消息先落到 {@code sys_message_inbox}，
 * 用户上线时再补投。
 * <p>
 * framework 只依赖本接口，不直接依赖任何业务模块，这样删除业务模块 jar 时
 * framework 仍可独立编译运行。
 *
 * @author scaffold
 */
public interface MessageBusRecorder
{
    /**
     * 把消息记一笔；返回值仅作日志/调试用，{@link RedisMessageBus} 不依赖其取值。
     *
     * @return 对实现自定义的"内部 ID"，没有时返回 {@code null}
     */
    Long record(PushMessage message);
}
