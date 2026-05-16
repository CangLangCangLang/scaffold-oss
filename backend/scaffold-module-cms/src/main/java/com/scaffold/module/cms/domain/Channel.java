package com.scaffold.module.cms.domain;

import com.scaffold.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CMS 栏目（树）。<br>
 * 规则：
 * <ul>
 *   <li>{@code code} 唯一，公开 API URL 参数。</li>
 *   <li>{@code parent_id=0} 表示根栏目。</li>
 *   <li>软删除使用 {@code del_flag='2'}；列表 / 树查询都强制 {@code del_flag='0'} 过滤。</li>
 *   <li>{@code status='1'} 表示停用——树查询仍能拿到，但公开 API 会过滤掉。</li>
 * </ul>
 */
@Schema(description = "CMS 栏目；既作为请求体（add/edit）也作为响应体（list/getInfo）；继承 BaseEntity 含 createBy/createTime/updateBy/updateTime")
public class Channel extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Schema(description = "栏目 id；新建留空，编辑必填")
    private Long id;

    @Schema(description = "父栏目 id；根栏目 parentId=0", example = "0")
    private Long parentId;

    @Schema(description = "栏目编码（全局唯一），公开 API 的 URL 标识；建议小写英文 + 连字符",
            example = "news", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Schema(description = "栏目名称（展示用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "新闻动态")
    private String name;

    @Schema(description = "同级排序权重；数字越小越靠前")
    private Integer orderNum;

    @Schema(description = "启用状态：'0'=启用，'1'=停用；公开 API 仅返回 '0'",
            allowableValues = {"0", "1"}, example = "0")
    private String status;

    @Schema(description = "SEO meta keywords（逗号分隔）")
    private String keywords;

    @Schema(description = "栏目描述（SEO 用）")
    private String description;

    @Schema(description = "栏目页模板名（前端门户切换布局用）")
    private String template;

    @Schema(description = "软删标记：'0'=正常，'2'=软删；列表 / 树查询都强制 '0' 过滤",
            allowableValues = {"0", "2"})
    private String delFlag;

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getParentId() { return parentId; }

    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getCode() { return code; }

    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public Integer getOrderNum() { return orderNum; }

    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public String getKeywords() { return keywords; }

    public void setKeywords(String keywords) { this.keywords = keywords; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getTemplate() { return template; }

    public void setTemplate(String template) { this.template = template; }

    public String getDelFlag() { return delFlag; }

    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
