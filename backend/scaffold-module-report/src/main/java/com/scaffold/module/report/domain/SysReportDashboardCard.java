package com.scaffold.module.report.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 看板内的卡片（关联报表模板 + 图表配置）。
 *
 * @author scaffold
 */
@Schema(description = "报表 - 看板卡片")
public class SysReportDashboardCard
{
    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "看板 ID")
    private Long dashboardId;

    @Schema(description = "关联模板 ID")
    private Long templateId;

    @Schema(description = "卡片标题")
    private String title;

    @Schema(description = "图表类型", example = "table",
            allowableValues = {"table", "line", "bar", "pie", "number"})
    private String chartType;

    @Schema(description = "图表 / 列映射等配置 JSON")
    private String configJson;

    @Schema(description = "默认参数 JSON")
    private String paramJson;

    @Schema(description = "栅格 x")
    private Integer posX;

    @Schema(description = "栅格 y")
    private Integer posY;

    @Schema(description = "宽度（24 列栅格）", defaultValue = "6")
    private Integer posW;

    @Schema(description = "高度", defaultValue = "6")
    private Integer posH;

    @Schema(description = "排序")
    private Integer orderNum;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDashboardId() { return dashboardId; }
    public void setDashboardId(Long dashboardId) { this.dashboardId = dashboardId; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getChartType() { return chartType; }
    public void setChartType(String chartType) { this.chartType = chartType; }
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
    public String getParamJson() { return paramJson; }
    public void setParamJson(String paramJson) { this.paramJson = paramJson; }
    public Integer getPosX() { return posX; }
    public void setPosX(Integer posX) { this.posX = posX; }
    public Integer getPosY() { return posY; }
    public void setPosY(Integer posY) { this.posY = posY; }
    public Integer getPosW() { return posW; }
    public void setPosW(Integer posW) { this.posW = posW; }
    public Integer getPosH() { return posH; }
    public void setPosH(Integer posH) { this.posH = posH; }
    public Integer getOrderNum() { return orderNum; }
    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
