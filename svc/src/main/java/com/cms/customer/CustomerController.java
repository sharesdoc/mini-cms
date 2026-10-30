package com.cms.customer;

import com.cms.common.CurrentUser;
import com.cms.common.Principal;
import com.cms.common.Result;
import com.cms.customer.dto.CustomerDtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客户网络浏览接口。
 * - 普通客户 GET /api/customers → 自己的下游（REQ-NET-04）。
 * - 管理员 GET /api/customers → 全量（可筛选/搜索）；/tree/roots、/{id}/children 树形逐级。
 * - GET /api/customers/{id} → 详情（可见性受控）。
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /** 列表 */
    @GetMapping
    public Result<List<CustomerDtos.CustomerNode>> list(
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) String q) {
        Principal p = CurrentUser.require();
        if (p.isAdmin()) {
            return Result.ok(customerService.adminList(level, q));
        }
        return Result.ok(customerService.myKids(p.getUserId()));
    }

    /** 树根（管理员） */
    @GetMapping("/tree/roots")
    public Result<List<CustomerDtos.CustomerNode>> roots() {
        CurrentUser.requireAdmin();
        return Result.ok(customerService.roots());
    }

    /** 某节点直接子节点（管理员逐级深入） */
    @GetMapping("/{id}/children")
    public Result<List<CustomerDtos.CustomerNode>> children(@PathVariable String id) {
        CurrentUser.requireAdmin();
        return Result.ok(customerService.children(id));
    }

    /** 客户详情 */
    @GetMapping("/{id}")
    public Result<CustomerDtos.CustomerDetail> detail(@PathVariable String id) {
        Principal p = CurrentUser.require();
        return Result.ok(customerService.detail(id, p.getUserId(), p.isAdmin()));
    }
}
