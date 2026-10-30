package com.cms.system;

import com.cms.im.friend.mapper.FriendshipMapper;
import com.cms.im.group.Group;
import com.cms.im.group.mapper.GroupMapper;
import com.cms.im.group.mapper.GroupMemberMapper;
import com.cms.im.message.Message;
import com.cms.im.message.mapper.MessageMapper;
import com.cms.user.User;
import com.cms.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 种子数据初始化：首次启动且 cms_user 为空时注入演示数据（对齐设计原型 seed）。
 * 包含 3 名一级、若干二三级客户、1 个禁用账户、好友关系、1 个群、若干消息。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String[] DEMO_NAMES = {"演示用户甲", "演示用户乙", "演示用户丙", "演示用户丁", "演示用户戊",
            "演示用户己", "演示用户庚", "演示用户辛", "演示用户壬", "演示用户癸"};

    private final UserMapper userMapper;
    private final FriendshipMapper friendshipMapper;
    private final GroupMapper groupMapper;
    private final GroupMemberMapper memberMapper;
    private final MessageMapper messageMapper;
    private final PasswordEncoder passwordEncoder;
    private final SystemConfigProvider cfg;

    public DataInitializer(UserMapper userMapper, FriendshipMapper friendshipMapper,
                           GroupMapper groupMapper, GroupMemberMapper memberMapper,
                           MessageMapper messageMapper, PasswordEncoder passwordEncoder,
                           SystemConfigProvider cfg) {
        this.userMapper = userMapper;
        this.friendshipMapper = friendshipMapper;
        this.groupMapper = groupMapper;
        this.memberMapper = memberMapper;
        this.messageMapper = messageMapper;
        this.passwordEncoder = passwordEncoder;
        this.cfg = cfg;
    }

    @Override
    public void run(String... args) {
        if (userMapper.selectAllLive() != null && !userMapper.selectAllLive().isEmpty()) {
            log.info("种子数据已存在，跳过初始化");
            return;
        }
        log.info("开始注入种子数据...");
        String configuredPassword = System.getenv("CMS_DEMO_USER_PASSWORD");
        String pwd = passwordEncoder.encode(
                configuredPassword == null || configuredPassword.isBlank()
                        ? java.util.UUID.randomUUID().toString()
                        : configuredPassword);
        int maxLevel = cfg.maxLevel();
        LocalDateTime now = LocalDateTime.now();
        List<User> l1s = new ArrayList<>();
        int ni = 0;

        // 三名一级客户
        String[] l1names = {"演示用户甲", "演示用户乙", "演示用户丙"};
        for (int i = 0; i < 3; i++) {
            User u = mkUser(ni++, pwd, l1names[i], 1, null, 30 - i * 4, null, now);
            userMapper.insert(u);
            u.setQuota(cfg.quota(1)); // 重置指标
            userMapper.updateQuota(u.getId(), u.getQuota());
            l1s.add(u);
        }
        User a = l1s.get(0), b = l1s.get(1), c = l1s.get(2);

        // 每个一级挂若干二级
        List<User> l2All = new ArrayList<>();
        int[] l2counts = {3, 2, 2};
        for (int i = 0; i < l1s.size(); i++) {
            User parent = l1s.get(i);
            for (int j = 0; j < l2counts[i]; j++) {
                User u = mkUser(ni++, pwd, DEMO_NAMES[ni % DEMO_NAMES.length], 2, parent.getId(), 20 - j * 2, null, now);
                userMapper.insert(u);
                l2All.add(u);
                parent.setQuota(parent.getQuota() - 1);
                userMapper.updateQuota(parent.getId(), parent.getQuota());
            }
        }

        // 部分二级挂三级
        for (int i = 0; i < l2All.size(); i++) {
            User parent = l2All.get(i);
            int n = (i % 2 == 0) ? 2 : 1;
            for (int j = 0; j < n; j++) {
                User u = mkUser(ni++, pwd, DEMO_NAMES[ni % DEMO_NAMES.length], 3, parent.getId(), 10 - j, null, now);
                userMapper.insert(u);
                parent.setQuota(parent.getQuota() - 1);
                userMapper.updateQuota(parent.getId(), parent.getQuota());
            }
        }

        // 一个禁用账户 + 一个无上级三级
        User disabled = mkUser(ni++, pwd, "演示禁用用户", 3, null, 1, null, now);
        disabled.setStatus("disabled");
        userMapper.insert(disabled);

        // 好友关系：a <-> kid0,kid1,b
        User aKid0 = l2All.get(0), aKid1 = l2All.get(1);
        friendshipMapper.insertPair(a.getId(), aKid0.getId(), now);
        friendshipMapper.insertPair(a.getId(), aKid1.getId(), now);
        friendshipMapper.insertPair(a.getId(), b.getId(), now);

        // 群：演示协作群
        Group g = new Group();
        g.setId("g_seed_001");
        g.setName("演示协作群");
        g.setOwnerId(a.getId());
        g.setMuted(0);
        g.setCreatedAt(now.minusDays(5));
        groupMapper.insert(g);
        List<String> members = Arrays.asList(a.getId(), aKid0.getId(), aKid1.getId(), b.getId());
        memberMapper.insertBatch(g.getId(), members, g.getCreatedAt());

        // 消息
        putMsg("dm", dmKey(a.getId(), aKid0.getId()), aKid0.getId(), "演示消息：客户资料已补齐。", now.minusHours(3));
        putMsg("dm", dmKey(a.getId(), aKid0.getId()), a.getId(), "演示回复：收到，指标还够用吗？", now.minusHours(2));
        putMsg("dm", dmKey(a.getId(), aKid0.getId()), aKid0.getId(), "演示回复：还剩 3 个。", now.minusHours(1));
        putMsg("group", g.getId(), a.getId(), "演示群消息：本月目标已完成。", now.minusHours(9));
        putMsg("group", g.getId(), aKid1.getId(), "演示群消息：收到，我会继续跟进。", now.minusHours(8));
        putMsg("group", g.getId(), b.getId(), "演示群消息：名单已整理完成。", now.minusHours(7));

        log.info("种子数据注入完成：{} 名客户", userMapper.selectAllLive().size());
    }

    private User mkUser(int idx, String pwd, String name, int level, String parentId, int daysAgo, String phone, LocalDateTime now) {
        User u = new User();
        u.setId(genId());
        String uname = phone != null ? phone : randomDemoUsername();
        u.setUsername(uname);
        u.setPassword(pwd);
        u.setName(name);
        u.setIdcard("");
        u.setPhone(phone != null ? phone : "");
        u.setAddress("");
        u.setAvatar(ThreadLocalRandom.current().nextInt(8));
        u.setLevel(level);
        u.setParentId(parentId);
        u.setQuota(cfg.quota(level));
        u.setStatus("active");
        u.setProfileDone(0);
        u.setCreatedAt(now.minusDays(daysAgo));
        return u;
    }

    private void putMsg(String type, String key, String sender, String text, LocalDateTime ts) {
        Message m = new Message();
        m.setScopeType(type);
        m.setScopeKey(key);
        m.setSenderId(sender);
        m.setContent(text);
        m.setCreatedAt(ts);
        messageMapper.insert(m);
    }

    private String genId() {
        for (int i = 0; i < 20; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(1 + ThreadLocalRandom.current().nextInt(9));
            for (int j = 0; j < 10; j++) sb.append(ThreadLocalRandom.current().nextInt(10));
            String id = sb.toString();
            if (userMapper.selectById(id) == null) return id;
        }
        throw new IllegalStateException("无法生成唯一 ID");
    }

    private String randomDemoUsername() {
        StringBuilder sb = new StringBuilder("demo-user-");
        for (int i = 0; i < 8; i++) sb.append(ThreadLocalRandom.current().nextInt(10));
        return sb.toString();
    }

    private static String dmKey(String a, String b) {
        String[] arr = {a, b};
        Arrays.sort(arr);
        return arr[0] + "_" + arr[1];
    }
}
