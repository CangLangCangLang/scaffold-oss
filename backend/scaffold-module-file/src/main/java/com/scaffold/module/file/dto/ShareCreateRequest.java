package com.scaffold.module.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建分享链接请求。
 * <ul>
 *   <li>{@link #expireDays} = 0/null → 永久；&gt;0 → 过期时间 = now() + {@link #expireDays}</li>
 *   <li>{@link #oneTime} = "1" → 一次性</li>
 *   <li>{@link #password} 非空 → 访问需要密码；存 BCrypt hash</li>
 * </ul>
 *
 * @author scaffold
 */
@Schema(description = "分享链接创建请求")
public class ShareCreateRequest
{
    @Schema(description = "文件 ID", required = true)
    @NotNull
    private Long fileId;

    @Schema(description = "过期天数（0/null = 永久）", example = "7")
    @Min(0)
    private Integer expireDays;

    @Schema(description = "1 = 一次性（用过即销毁）", allowableValues = {"0", "1"}, example = "0")
    private String oneTime;

    @Schema(description = "可选访问密码（明文，server 端 BCrypt）")
    @Size(max = 64)
    private String password;

    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }
    public Integer getExpireDays() { return expireDays; }
    public void setExpireDays(Integer expireDays) { this.expireDays = expireDays; }
    public String getOneTime() { return oneTime; }
    public void setOneTime(String oneTime) { this.oneTime = oneTime; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
