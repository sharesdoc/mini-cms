package com.cms.admin;

import com.cms.admin.dto.AdminDtos;
import com.cms.common.BizException;
import com.cms.common.ResultCode;
import com.cms.customer.dto.CustomerDtos;
import com.cms.system.SystemConfigProvider;
import com.cms.user.User;
import com.cms.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 管理员领域服务：统计报表、账户启停、伪删除、调整指标、系统配置。
 */
@Service
public class AdminService {

    private final UserMapper userMapper;
    private final SystemConfigProvider cfg;

    public AdminService(UserMapper userMapper, SystemConfigProvider cfg) {
        this.userMapper = userMapper;
        this.cfg = cfg;
    }

    /** 统计报表（REQ-ADM-03） */
    public AdminDtos.Stats stats() {
        List<User> live = userMapper.selectAllLive();
        int maxLevel = cfg.maxLevel();
        LocalDate today = LocalDate.now();

        // 7 日趋势
        List<AdminDtos.DayPoint> series = new ArrayList<>();
        int weekNew = 0;
        for (int off = 6; off >= 0; off--) {
            LocalDate d = today.minusDays(off);
            int c = 0;
            for (User u : live) {
                if (u.getCreatedAt() != null && u.getCreatedAt().toLocalDate().equals(d)) c++;
            }
            series.add(new AdminDtos.DayPoint(d.getMonthValue() + "/" + d.getDayOfMonth(), c));
            if (off < 7) weekNew += c; // 近 7 日（含今日）
        }
        int todayNew = series.get(series.size() - 1).count();

        // 级别分布
        List<AdminDtos.LevelDist> dist = new ArrayList<>();
        int total = live.size();
        for (int l = 1; l <= maxLevel; l++) {
            int lvl = l;
            int n = (int) live.stream().filter(u -> u.getLevel() != null && u.getLevel() == lvl).count();
            int pct = total == 0 ? 0 : Math.round(n * 100f / total);
            dist.add(new AdminDtos.LevelDist(lvl, n, pct));
        }

        int disabled = (int) live.stream().filter(u -> "disabled".equals(u.getStatus())).count();

        // 最近 5 注册
        List<CustomerDtos.CustomerNode> recent = live.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .map(u -> new CustomerDtos.CustomerNode(u.getId(), u.getName(), u.getUsername(), u.getAvatar(),
                        u.getLevel(), u.getQuota(), u.getStatus(), userMapper.countChildren(u.getId()), u.getCreatedAt()))
                .toList();

        return new AdminDtos.Stats(total, todayNew, weekNew, disabled, series, dist, recent);
    }

    /** 启停账户（REQ-ADM-02） */
    public void updateStatus(String id, String status) {
        User u = mustGetAny(id);
        if (!"active".equals(status) && !"disabled".equals(status)) {
            throw new BizException(ResultCode.BAD_PARAM, "status 仅支持 active/disabled");
        }
        userMapper.updateStatus(id, status);
    }

    /** 伪删除（REQ-ADM-01） */
    public void softDelete(String id) {
        User u = mustGetAny(id);
        if ("admin".equals(u.getId())) throw new BizException(ResultCode.BAD_PARAM);
        userMapper.updateStatus(id, "deleted");
    }

    /** 调整指标（REQ-LVL-03） */
    public void updateQuota(String id, Integer quota) {
        mustGetAny(id);
        if (quota == null || quota < 0) throw new BizException(ResultCode.BAD_PARAM, "指标须为非负整数");
        userMapper.updateQuota(id, quota);
    }

    /** 配置只读快照 */
    public Map<String, Object> config() {
        return cfg.snapshot();
    }

    /** 更新配置（maxLevel 只读不可改） */
    public Map<String, Object> updateConfig(AdminDtos.ConfigUpdate req) {
        cfg.update(req.quota(), req.upgrade());
        return cfg.snapshot();
    }

    private User mustGetAny(String id) {
        User u = userMapper.selectById(id);
        if (u == null) throw new BizException(ResultCode.USER_NOT_FOUND);
        return u;
    }
}
