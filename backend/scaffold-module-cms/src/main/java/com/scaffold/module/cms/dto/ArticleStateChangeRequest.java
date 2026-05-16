package com.scaffold.module.cms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 状态流转请求。<br>
 * 不强制 {@code reason}，但驳回（{@code REJECT}）通常前端会带上原因落到审计 comment。
 */
@Schema(description = "状态机流转请求体；6 个流转端点共用此结构，目前仅含 reason 字段")
public class ArticleStateChangeRequest
{
    @Schema(description = "操作原因；reject / unpublish / back-to-draft 时建议带上，会落到审计 comment 与作者站内信",
            example = "标题不合规")
    private String reason;

    public String getReason() { return reason; }

    public void setReason(String reason) { this.reason = reason; }
}
