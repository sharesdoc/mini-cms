package com.cms.customer;

import com.cms.common.BizException;
import com.cms.common.ResultCode;
import com.cms.customer.dto.CustomerDtos;
import com.cms.system.SystemConfigProvider;
import com.cms.user.User;
import com.cms.user.mapper.UserMapper;
import com.cms.user.dto.PublicUserVO;
import com.cms.user.dto.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 客户网络领域服务：拓展下游（含自动升级）、首页汇总、网络列表/树/详情、可见性控制。
 * 校验失败以 i18n key 作为异常 message（前端按 key 本地化）。
 */
@Service
public class CustomerService {

    private final UserMapper userMapper;
    private final SystemConfigProvider cfg;

    public CustomerService(UserMapper userMapper, SystemConfigProvider cfg) {
        this.userMapper = userMapper;
        this.cfg = cfg;
    }

    // ==================== 拓展下游（REQ-LVL-02/06/07） ====================

    @Transactional
    public CustomerDtos.ExpandResult expand(String meId, String targetId) {
        User me = mustGet(meId);
        String tid = targetId == null ? "" : targetId.trim();
        // 5 项前置校验（E §3.3）
        if (tid.equals(me.getId())) throw biz("errSelf");
        User target = userMapper.selectById(tid);
        if (target == null || "deleted".equals(target.getStatus())) throw biz("errNotFound");
        if (target.getParentId() != null) throw biz("errHasParent");
        // 注：不在 me.level >= maxLevel 处拦截。需求 REQ-LVL-07 明确「普通客户(最低级)拓展客户后升级」，
        // 若阻止最低级拓展则新注册用户(level=maxLevel)永远无法升级，核心机制失效。
        // 设计原型的 errLevel 检查与此冲突，按铁律 0 以需求为准；目标级别由下方 min(maxLevel, me.level+1) 封顶。

        int maxLevel = cfg.maxLevel();
        // 原子扣减 1 个指标（同时完成 noQuota 校验，消除并发超卖竞态）
        int affected = userMapper.decrementQuota(me.getId());
        if (affected == 0) throw biz("errNoQuota");
        // 扣减后重新读取，保证返回的剩余指标与 DB 一致
        me = userMapper.selectById(meId);
        int meQuotaLeft = me.getQuota();

        // 应用挂靠：原子认领目标（仅当尚无上级时），防止两上级并发抢同一目标
        int newChildLevel = Math.min(maxLevel, me.getLevel() + 1);
        int claimed = userMapper.claimTarget(target.getId(), me.getId(), newChildLevel);
        if (claimed == 0) {
            // 目标已被并发抢走（或刚被他人挂靠），退还刚扣的指标
            userMapper.incrementQuota(me.getId());
            throw biz("errHasParent");
        }

        // 升级判定（E §3.4）
        boolean upgraded = false;
        int levelAfter = me.getLevel();
        int count = userMapper.countChildren(me.getId());
        int threshold = cfg.upgradeThreshold(me.getLevel());
        if (threshold > 0 && count >= threshold && me.getLevel() > 1) {
            levelAfter = me.getLevel() - 1;
            userMapper.updateLevel(me.getId(), levelAfter);
            meQuotaLeft += cfg.quota(levelAfter);   // 补齐新级别指标
            userMapper.updateQuota(me.getId(), meQuotaLeft);
            upgraded = true;
        }
        return new CustomerDtos.ExpandResult(upgraded, upgraded ? levelAfter : null, count, meQuotaLeft);
    }

    // ==================== 首页汇总 ====================

    public CustomerDtos.HomeSummary home(String meId) {
        User me = mustGet(meId);
        List<User> kids = userMapper.selectChildren(meId);
        List<CustomerDtos.CustomerNode> kidNodes = kids.stream().map(u -> node(u)).toList();
        int threshold = cfg.upgradeThreshold(me.getLevel());
        boolean canUpgrade = me.getLevel() > 1 && threshold > 0;
        int need = canUpgrade ? Math.max(0, threshold - kids.size()) : 0;
        PublicUserVO parent = null;
        if (me.getParentId() != null) {
            User p = userMapper.selectById(me.getParentId());
            if (p != null && !"deleted".equals(p.getStatus())) parent = PublicUserVO.of(p);
        }
        return new CustomerDtos.HomeSummary(me.getLevel(), me.getQuota(), kids.size(),
                canUpgrade, threshold, need, parent, kidNodes);
    }

    // ==================== 网络列表/树/详情 ====================

    /** 普通客户：自己的下游 */
    public List<CustomerDtos.CustomerNode> myKids(String meId) {
        return userMapper.selectChildren(meId).stream().map(u -> node(u)).toList();
    }

    /** 管理员：全量，可按级别/关键字筛选 */
    public List<CustomerDtos.CustomerNode> adminList(Integer level, String q) {
        List<User> users;
        if (q != null && !q.isBlank()) {
            users = userMapper.searchLive(q.trim());
        } else if (level != null && level > 0) {
            users = userMapper.selectLiveByLevel(level);
        } else {
            users = userMapper.selectAllLive();
        }
        return users.stream().map(u -> node(u)).toList();
    }

    /** 管理员：树根（无上级的活跃客户） */
    public List<CustomerDtos.CustomerNode> roots() {
        return userMapper.selectAllLive().stream()
                .filter(u -> u.getParentId() == null)
                .map(u -> node(u)).toList();
    }

    /** 管理员：某节点直接子节点 */
    public List<CustomerDtos.CustomerNode> children(String parentId) {
        return userMapper.selectChildren(parentId).stream().map(u -> node(u)).toList();
    }

    /**
     * 客户详情。可见性：
     * - 管理员：任意。
     * - 普通客户：仅自己或自己的直接下游（REQ-LVL-04 不可跨级）。
     */
    public CustomerDtos.CustomerDetail detail(String id, String viewerId, boolean isAdmin) {
        User u = userMapper.selectById(id);
        if (u == null || "deleted".equals(u.getStatus())) throw new BizException(ResultCode.USER_NOT_FOUND);
        if (!isAdmin) {
            boolean allowed = id.equals(viewerId) || viewerId.equals(u.getParentId());
            if (!allowed) throw new BizException(ResultCode.FORBIDDEN);  // 跨级越权
        }
        List<CustomerDtos.CustomerNode> kids = userMapper.selectChildren(id).stream()
                .map(k -> node(k)).toList();
        return new CustomerDtos.CustomerDetail(UserVO.of(u), kids.size(), kids);
    }

    // ==================== helpers ====================

    /** 构建客户节点（含直接下游数量） */
    private CustomerDtos.CustomerNode node(User u) {
        Integer kidCount = userMapper.countChildren(u.getId());
        return new CustomerDtos.CustomerNode(u.getId(), u.getName(), u.getUsername(), u.getAvatar(),
                u.getLevel(), u.getQuota(), u.getStatus(), kidCount, u.getCreatedAt());
    }

    private User mustGet(String id) {
        User u = userMapper.selectById(id);
        if (u == null || "deleted".equals(u.getStatus())) throw new BizException(ResultCode.USER_NOT_FOUND);
        return u;
    }

    /** 业务异常，message 为前端 i18n key */
    private static BizException biz(String i18nKey) {
        return new BizException(ResultCode.EXPAND_FAIL, i18nKey);
    }
}
