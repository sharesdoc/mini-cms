package com.cms.customer.dto;

import com.cms.user.dto.PublicUserVO;
import com.cms.user.dto.UserVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户网络域 DTO 集合。
 */
public final class CustomerDtos {

    /** 网络节点（列表/树通用） */
    public record CustomerNode(
            String id, String name, String username, Integer avatar,
            Integer level, Integer quota, String status, Integer kidCount,
            LocalDateTime createdAt
    ) {}

    /** 拓展请求 */
    public record ExpandRequest(String targetId) {}

    /** 拓展结果 */
    public record ExpandResult(boolean upgraded, Integer newLevel, Integer kidCount, Integer quotaLeft) {}

    /** 首页汇总。parent 为上级脱敏信息（仅 id/name/avatar，不暴露 PII）。 */
    public record HomeSummary(
            Integer level, Integer quota, Integer kidCount,
            boolean canUpgrade, Integer threshold, Integer needCount,
            PublicUserVO parent, List<CustomerNode> kids
    ) {}

    /** 客户详情 */
    public record CustomerDetail(
            UserVO user, Integer kidCount, List<CustomerNode> kids
    ) {}

    private CustomerDtos() {}
}
