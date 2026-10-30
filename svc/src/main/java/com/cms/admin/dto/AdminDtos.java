package com.cms.admin.dto;

import com.cms.customer.dto.CustomerDtos;

import java.util.List;
import java.util.Map;

/**
 * 管理员域 DTO。
 */
public final class AdminDtos {

    /** 7 日趋势单点 */
    public record DayPoint(String date, int count) {}

    /** 级别分布 */
    public record LevelDist(int level, int count, int pct) {}

    /** 统计报表 */
    public record Stats(
            int total, int todayNew, int weekNew, int disabledCount,
            List<DayPoint> series7, List<LevelDist> levelDist,
            List<CustomerDtos.CustomerNode> recent
    ) {}

    /** 状态切换 */
    public record StatusRequest(String status) {}

    /** 调整指标 */
    public record QuotaRequest(Integer quota) {}

    /** 配置更新 */
    public record ConfigUpdate(Map<String, Integer> quota, Map<String, Integer> upgrade) {}

    private AdminDtos() {}
}
