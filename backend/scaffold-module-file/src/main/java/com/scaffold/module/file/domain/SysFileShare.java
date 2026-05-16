package com.scaffold.module.file.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.scaffold.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文件中心：分享链接（带过期 / 一次性 token / 可选密码）。
 *
 * <p>分享访问走 {@code GET /file/share/access/{token}}，校验：
 * <ul>
 *   <li>{@link #status}=0 (有效)</li>
 *   <li>{@link #expireAt} == NULL 或 > now()</li>
 *   <li>{@link #oneTime}=1 时，{@link #visits} 必须 < 1</li>
 *   <li>{@link #passwordHash} 不为空时校验密码（BCrypt.matches）</li>
 * </ul>
 *
 * <p>每次访问 visits +1；oneTime=1 且 visits>=1 时把 status 置 2（已用尽）。
 *
 * @author scaffold
 */
@Schema(description = "文件中心 - 分享链接")
public class SysFileShare extends BaseEntity
{
    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "关联 sys_file.id", example = "12")
    private Long fileId;

    @Schema(description = "随机 token（URL 友好）")
    private String token;

    @Schema(description = "过期时间（NULL = 永久）", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireAt;

    @Schema(description = "1=一次性（用过即销毁）", example = "0", allowableValues = {"0", "1"})
    private String oneTime;

    @Schema(description = "已访问次数", example = "0")
    private Integer visits;

    /** BCrypt hash；只在写入路径用，列表 / 详情都不返给前端。 */
    @Schema(hidden = true)
    @JsonIgnore
    private String passwordHash;

    @Schema(description = "状态：0=有效 / 1=已停用 / 2=已用尽", allowableValues = {"0", "1", "2"})
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Date getExpireAt() { return expireAt; }
    public void setExpireAt(Date expireAt) { this.expireAt = expireAt; }
    public String getOneTime() { return oneTime; }
    public void setOneTime(String oneTime) { this.oneTime = oneTime; }
    public Integer getVisits() { return visits; }
    public void setVisits(Integer visits) { this.visits = visits; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
