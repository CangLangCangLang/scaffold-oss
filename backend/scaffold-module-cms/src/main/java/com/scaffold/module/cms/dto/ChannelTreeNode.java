package com.scaffold.module.cms.dto;

import java.util.ArrayList;
import java.util.List;
import com.scaffold.module.cms.domain.Channel;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 栏目树节点（前端 el-tree 直渲）。
 */
@Schema(description = "栏目树节点；GET /cms/channel/tree 与 /cms/public/channels 都返回此结构（递归 children）")
public class ChannelTreeNode
{
    @Schema(description = "栏目 id")
    private Long id;

    @Schema(description = "父栏目 id；根栏目 parentId=0")
    private Long parentId;

    @Schema(description = "栏目编码（全局唯一，作为公开 API URL 标识）")
    private String code;

    @Schema(description = "栏目名称（展示用）")
    private String name;

    @Schema(description = "同级排序权重；数字越小越靠前")
    private Integer orderNum;

    @Schema(description = "启用状态：'0'=启用，'1'=停用；公开 API 仅返回 '0'",
            allowableValues = {"0", "1"})
    private String status;

    @Schema(description = "栏目描述（SEO 用）")
    private String description;

    @Schema(description = "关键字（SEO meta keywords，逗号分隔）")
    private String keywords;

    @Schema(description = "栏目页模板名（前端门户切换布局用）")
    private String template;

    @Schema(description = "子栏目（递归结构）；叶子节点为空数组")
    private List<ChannelTreeNode> children = new ArrayList<>();

    public static ChannelTreeNode of(Channel c)
    {
        ChannelTreeNode n = new ChannelTreeNode();
        n.id = c.getId();
        n.parentId = c.getParentId();
        n.code = c.getCode();
        n.name = c.getName();
        n.orderNum = c.getOrderNum();
        n.status = c.getStatus();
        n.description = c.getDescription();
        n.keywords = c.getKeywords();
        n.template = c.getTemplate();
        return n;
    }

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

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getKeywords() { return keywords; }

    public void setKeywords(String keywords) { this.keywords = keywords; }

    public String getTemplate() { return template; }

    public void setTemplate(String template) { this.template = template; }

    public List<ChannelTreeNode> getChildren() { return children; }

    public void setChildren(List<ChannelTreeNode> children) { this.children = children; }
}
