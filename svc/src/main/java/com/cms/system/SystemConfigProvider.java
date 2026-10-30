package com.cms.system;

import com.cms.config.CmsProperties;
import com.cms.system.mapper.SysConfigMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置读取门面：
 * - maxLevel 来自部署配置（CmsProperties），代码硬上限 3（REQ-LVL-01）。
 * - quota.N / upgrade.N 来自 cms_sys_config 表。
 * 启动时与每次写配置后刷新缓存。
 */
@Component
public class SystemConfigProvider {

    private final CmsProperties props;
    private final SysConfigMapper mapper;
    private volatile Map<String, String> cache = new HashMap<>();

    public SystemConfigProvider(CmsProperties props, SysConfigMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        refresh();
    }

    /** 重新加载配置缓存 */
    public synchronized void refresh() {
        List<SysConfig> all = mapper.selectAll();
        Map<String, String> m = new HashMap<>();
        for (SysConfig c : all) {
            m.put(c.getCfgKey(), c.getCfgValue());
        }
        // 兜底默认值（表为空时）
        ensure(m, "quota.1", "8");
        ensure(m, "quota.2", "5");
        ensure(m, "quota.3", "3");
        ensure(m, "upgrade.3", "3");
        ensure(m, "upgrade.2", "5");
        this.cache = m;
    }

    private void ensure(Map<String, String> m, String k, String def) {
        if (!m.containsKey(k) || m.get(k) == null || m.get(k).isBlank()) {
            m.put(k, def);
        }
    }

    /** 有效最大级别（clamp 到 [1,3]） */
    public int maxLevel() {
        return props.effectiveMaxLevel();
    }

    /** 某级别默认可拓展指标 */
    public int quota(int level) {
        return parseInt(cache.get("quota." + level), 0);
    }

    /** 某级别升级门槛（拓展满 N 个可升级）；返回 0 表示不可升级 */
    public int upgradeThreshold(int level) {
        return parseInt(cache.get("upgrade." + level), 0);
    }

    /** 全部配置（含 maxLevel 只读） */
    public Map<String, Object> snapshot() {
        Map<String, Object> snap = new HashMap<>();
        snap.put("maxLevel", maxLevel());
        Map<String, Integer> quota = new HashMap<>();
        Map<String, Integer> upgrade = new HashMap<>();
        for (int l = 1; l <= maxLevel(); l++) {
            quota.put(String.valueOf(l), quota(l));
        }
        for (int l = 2; l <= maxLevel(); l++) {
            upgrade.put(String.valueOf(l), upgradeThreshold(l));
        }
        snap.put("quota", quota);
        snap.put("upgrade", upgrade);
        return snap;
    }

    /** 写入配置项并刷新 */
    public synchronized void update(Map<String, Integer> quotaUpdates, Map<String, Integer> upgradeUpdates) {
        int max = maxLevel();
        if (quotaUpdates != null) {
            for (Map.Entry<String, Integer> e : quotaUpdates.entrySet()) {
                int lvl = parseInt(e.getKey(), -1);
                if (lvl < 1 || lvl > max) continue;   // 级别越界忽略
                mapper.upsert("quota." + lvl, String.valueOf(clamp(e.getValue(), 0, 999)));
            }
        }
        if (upgradeUpdates != null) {
            for (Map.Entry<String, Integer> e : upgradeUpdates.entrySet()) {
                int lvl = parseInt(e.getKey(), -1);
                if (lvl < 2 || lvl > max) continue;
                mapper.upsert("upgrade." + lvl, String.valueOf(clamp(e.getValue(), 0, 999)));
            }
        }
        refresh();
    }

    private static int parseInt(String s, int def) {
        try { return s == null ? def : Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.min(hi, Math.max(lo, v));
    }
}
