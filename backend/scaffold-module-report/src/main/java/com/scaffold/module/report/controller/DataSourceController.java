package com.scaffold.module.report.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.scaffold.common.annotation.AuditLog;
import com.scaffold.common.core.controller.BaseController;
import com.scaffold.common.core.domain.AjaxResult;
import com.scaffold.module.report.dto.DataSourceUpsertRequest;
import com.scaffold.module.report.service.DataSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 报表外部数据源 CRUD + 测连接（M-8）。
 *
 * @author scaffold
 */
@Tag(name = "报表中心 - 数据源（M-8）",
        description = "外部 JDBC 数据源 CRUD（密码 AES 加密落库；列表 / 详情不返回密文）+ 测连接")
@RestController
@RequestMapping("/report/datasource")
public class DataSourceController extends BaseController
{
    @Autowired
    private DataSourceService dsService;

    @Operation(summary = "数据源列表")
    @PreAuthorize("@ss.hasPermi('report:datasource:list')")
    @GetMapping
    public AjaxResult list()
    {
        return success(dsService.list());
    }

    @Operation(summary = "数据源详情")
    @PreAuthorize("@ss.hasPermi('report:datasource:list')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return success(dsService.detail(id));
    }

    @Operation(summary = "新增数据源")
    @PreAuthorize("@ss.hasPermi('report:datasource:add')")
    @AuditLog(module = "report.datasource", action = "ADD", resourceType = "datasource",
            resourceId = "#result?.data",
            comment = "'新增数据源 ' + #req.code")
    @PostMapping
    public AjaxResult add(@RequestBody DataSourceUpsertRequest req)
    {
        req.setId(null);
        return success(dsService.save(req));
    }

    @Operation(summary = "编辑数据源（password=null 表示不动；空串清空）")
    @PreAuthorize("@ss.hasPermi('report:datasource:edit')")
    @AuditLog(module = "report.datasource", action = "EDIT", resourceType = "datasource",
            resourceId = "#req.id",
            comment = "'编辑数据源 ' + #req.id")
    @PutMapping
    public AjaxResult edit(@RequestBody DataSourceUpsertRequest req)
    {
        return success(dsService.save(req));
    }

    @Operation(summary = "删除数据源")
    @PreAuthorize("@ss.hasPermi('report:datasource:remove')")
    @AuditLog(module = "report.datasource", action = "REMOVE", resourceType = "datasource",
            resourceId = "#id",
            comment = "'删除数据源 ' + #id")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        dsService.remove(id);
        return success();
    }

    @Operation(summary = "测试连接（不入池）")
    @PreAuthorize("@ss.hasPermi('report:datasource:test')")
    @PostMapping("/test")
    public AjaxResult test(@RequestBody DataSourceUpsertRequest req)
    {
        dsService.test(req);
        return success("连接成功");
    }
}
