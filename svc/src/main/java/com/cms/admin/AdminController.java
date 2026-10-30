package com.cms.admin;

import com.cms.common.CurrentUser;
import com.cms.common.Result;
import com.cms.admin.dto.AdminDtos;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理员接口：所有方法强制管理员角色。
 * 统计 / 状态切换 / 伪删除 / 调整指标 / 系统配置。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public Result<AdminDtos.Stats> stats() {
        CurrentUser.requireAdmin();
        return Result.ok(adminService.stats());
    }

    @PutMapping("/customers/{id}/status")
    public Result<Void> status(@PathVariable String id, @RequestBody AdminDtos.StatusRequest req) {
        CurrentUser.requireAdmin();
        adminService.updateStatus(id, req.status());
        return Result.ok();
    }

    @DeleteMapping("/customers/{id}")
    public Result<Void> delete(@PathVariable String id) {
        CurrentUser.requireAdmin();
        adminService.softDelete(id);
        return Result.ok();
    }

    @PutMapping("/customers/{id}/quota")
    public Result<Void> quota(@PathVariable String id, @RequestBody AdminDtos.QuotaRequest req) {
        CurrentUser.requireAdmin();
        adminService.updateQuota(id, req.quota());
        return Result.ok();
    }

    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        CurrentUser.requireAdmin();
        return Result.ok(adminService.config());
    }

    @PutMapping("/config")
    public Result<Map<String, Object>> updateConfig(@RequestBody AdminDtos.ConfigUpdate req) {
        CurrentUser.requireAdmin();
        return Result.ok(adminService.updateConfig(req));
    }

    // 恢复伪删除客户（数据可恢复，REQ-ADM-01 隐含能力）
    @PostMapping("/customers/{id}/restore")
    public Result<Void> restore(@PathVariable String id) {
        CurrentUser.requireAdmin();
        adminService.updateStatus(id, "active");
        return Result.ok();
    }
}
