package com.cms;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CMS 集成测试：@SpringBootTest 全量上下文 + TestRestTemplate（真实 HTTP/拦截器/DB）。
 * @BeforeEach 清空 6 表保证用例隔离与可重复。覆盖认证/拓展/可见性/管理员/IM 关键链路。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "cms.admin.password=test-admin-password")
class CmsIntegrationTest {

    @LocalServerPort int port;
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    private final ObjectMapper om = new ObjectMapper();

    private String base() { return "http://localhost:" + port; }

    @BeforeEach
    void cleanTables() {
        jdbc.execute("SET FOREIGN_KEY_CHECKS=0");
        jdbc.execute("TRUNCATE TABLE cms_message");
        jdbc.execute("TRUNCATE TABLE cms_group_member");
        jdbc.execute("TRUNCATE TABLE cms_group");
        jdbc.execute("TRUNCATE TABLE cms_friendship");
        jdbc.execute("TRUNCATE TABLE cms_user");
        jdbc.execute("TRUNCATE TABLE cms_sys_config");
        jdbc.execute("SET FOREIGN_KEY_CHECKS=1");
        jdbc.execute("INSERT INTO cms_sys_config(cfg_key,cfg_value) VALUES " +
                "('quota.1','8'),('quota.2','5'),('quota.3','3'),('upgrade.3','3'),('upgrade.2','5')");
    }

    // ---------- helpers ----------
    private Map<String, Object> body(ResponseEntity<String> r) {
        try { return om.readValue(r.getBody(), Map.class); } catch (Exception e) { throw new RuntimeException(e); }
    }
    private Map<String, Object> post(String path, Object payload, String token) {
        HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) h.setBearerAuth(token);
        return body(http.exchange(base() + path, HttpMethod.POST, new HttpEntity<>(payload, h), String.class));
    }
    private Map<String, Object> get(String path, String token) {
        HttpHeaders h = new HttpHeaders(); if (token != null) h.setBearerAuth(token);
        return body(http.exchange(base() + path, HttpMethod.GET, new HttpEntity<>(h), String.class));
    }
    private Map<String, Object> put(String path, Object payload, String token) {
        HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) h.setBearerAuth(token);
        return body(http.exchange(base() + path, HttpMethod.PUT, new HttpEntity<>(payload, h), String.class));
    }
    private Map<String, Object> del(String path, String token) {
        HttpHeaders h = new HttpHeaders(); if (token != null) h.setBearerAuth(token);
        return body(http.exchange(base() + path, HttpMethod.DELETE, new HttpEntity<>(h), String.class));
    }
    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> r) { return (Map<String, Object>) r.get("data"); }

    /** 注册并返回 token */
    private String register(String uname) {
        Map<String, Object> r = post("/api/auth/register",
                Map.of("username", uname, "password", "123456", "confirmPwd", "123456"), null);
        assertEquals(0, r.get("code"), "注册失败: " + r);
        return (String) data(r).get("token");
    }
    private String login(String uname) {
        Map<String, Object> r = post("/api/auth/login", Map.of("username", uname, "password", "123456"), null);
        assertEquals(0, r.get("code"), "登录失败: " + r);
        return (String) data(r).get("token");
    }
    private String adminLogin() {
        Map<String, Object> r = post("/api/auth/login",
                Map.of("username", "admin", "password", "test-admin-password"), null);
        assertEquals(0, r.get("code"));
        assertEquals("ADMIN", data(r).get("role"));
        return (String) data(r).get("token");
    }

    // ==================== 认证 ====================

    @Test
    void 注册_登录_token鉴权_用户名重复40901() {
        String tok = register("13900000001");
        assertNotNull(tok);
        // 重复注册
        Map<String, Object> dup = post("/api/auth/register",
                Map.of("username", "13900000001", "password", "123456", "confirmPwd", "123456"), null);
        assertEquals(40901, dup.get("code"));
        // 登录
        assertNotNull(login("13900000001"));
        // 无 token 访问受保护接口 → 40101
        Map<String, Object> noAuth = get("/api/user/me", null);
        assertEquals(40101, noAuth.get("code"));
    }

    @Test
    void admin写死口令登录_错误口令40903() {
        assertNotNull(adminLogin());
        Map<String, Object> wrong = post("/api/auth/login", Map.of("username", "admin", "password", "wrong"), null);
        assertEquals(40903, wrong.get("code"));
    }

    @Test
    void CORS预检OPTIONS放行_不被401阻断() {
        // 生产跨源场景：浏览器对带 Authorization 的非简单请求先发 OPTIONS 预检。
        // AuthInterceptor 须放行 OPTIONS，否则预检被 401 阻断、实际请求无法发出。
        HttpHeaders h = new HttpHeaders();
        h.setOrigin("http://localhost:5173");
        h.add("Access-Control-Request-Method", "DELETE");
        h.add("Access-Control-Request-Headers", "authorization");
        ResponseEntity<String> resp = http.exchange(base() + "/api/admin/customers/x",
                HttpMethod.OPTIONS, new HttpEntity<>(h), String.class);
        // 核心：预检须成功（2xx），不被 AuthInterceptor 的无 token→401 阻断。
        // （完整 CORS 头由真实 curl 预检实测确认；测试 harness 中 Spring CORS 头注入有差异，此处只校验放行）
        assertTrue(resp.getStatusCode().is2xxSuccessful(), "OPTIONS 预检应成功，不应被 401 阻断: " + resp.getStatusCode());
    }

    @Test
    void 密码不一致与过短被拒() {
        Map<String, Object> mismatch = post("/api/auth/register",
                Map.of("username", "13900000010", "password", "123456", "confirmPwd", "654321"), null);
        assertEquals(40001, mismatch.get("code"));
        Map<String, Object> shortPwd = post("/api/auth/register",
                Map.of("username", "13900000011", "password", "123", "confirmPwd", "123"), null);
        assertEquals(40001, shortPwd.get("code"));
    }

    // ==================== 拓展下游 + 升级（N-U02/U03/U04） ====================

    @Test
    void 拓展校验_自己_无此人_有上级_成功_升级() {
        String a = register("13900000101");           // level3
        String b = register("13900000102");           // level3, 无上级
        String bid = (String) data(get("/api/user/me", b)).get("id");

        String aid = (String) data(get("/api/user/me", a)).get("id");

        // 不能拓展自己（用 a 自己的 id）
        assertEquals(40905, post("/api/user/expand", Map.of("targetId", aid), a).get("code"));
        // 不存在的 ID
        assertEquals(40905, post("/api/user/expand", Map.of("targetId", "99999999999"), a).get("code"));

        // 成功拓展：a(level3) 拓展 b → b 挂靠，目标级别封顶 min(maxLevel, level+1)=min(3,4)=3
        Map<String, Object> ok = post("/api/user/expand", Map.of("targetId", bid), a);
        assertEquals(0, ok.get("code"), "拓展应成功: " + ok);
        Integer bLevel = jdbc.queryForObject("SELECT level FROM cms_user WHERE id=?", Integer.class, bid);
        String bParent = jdbc.queryForObject("SELECT parent_id FROM cms_user WHERE id=?", String.class, bid);
        assertEquals(3, bLevel);
        assertEquals(aid, bParent);

        // 再次拓展 b → errHasParent
        assertEquals(40905, post("/api/user/expand", Map.of("targetId", bid), a).get("code"));

        // 另验证：level1 父级拓展产生 level2 子级 min(3,1+1)=2
        jdbc.update("UPDATE cms_user SET level=1, quota=8 WHERE id=?", aid);
        String c = register("13900000103");
        String cid = (String) data(get("/api/user/me", c)).get("id");
        assertEquals(0, post("/api/user/expand", Map.of("targetId", cid), a).get("code"));
        assertEquals(2, jdbc.queryForObject("SELECT level FROM cms_user WHERE id=?", Integer.class, cid));
    }

    @Test
    void 拓展达门槛自动升级() {
        String a = register("13900000201");
        String aid = (String) data(get("/api/user/me", a)).get("id");
        // a 设为 level2，upgrade.2=5（门槛），quota 足够
        jdbc.update("UPDATE cms_user SET level=2, quota=20 WHERE id=?", aid);
        // 注册 5 个无上级客户并由 a 拓展
        for (int i = 0; i < 5; i++) {
            String t = register("1390000021" + i);
            String tid = (String) data(get("/api/user/me", t)).get("id");
            Map<String, Object> r = post("/api/user/expand", Map.of("targetId", tid), a);
            assertEquals(0, r.get("code"), "第 " + i + " 次拓展失败: " + r);
        }
        // 第 5 个拓展后 count=5 >= 门槛 5，level2→level1
        Integer level = jdbc.queryForObject("SELECT level FROM cms_user WHERE id=?", Integer.class, aid);
        assertEquals(1, level, "拓展满门槛应自动升级为 level1");
    }

    @Test
    void 拓展无指标被拒() {
        String a = register("13900000301");
        String b = register("13900000302");
        String aid = (String) data(get("/api/user/me", a)).get("id");
        String bid = (String) data(get("/api/user/me", b)).get("id");
        jdbc.update("UPDATE cms_user SET level=1, quota=0 WHERE id=?", aid);  // 0 指标
        Map<String, Object> r = post("/api/user/expand", Map.of("targetId", bid), a);
        assertEquals(40905, r.get("code"));
    }

    @Test
    void 并发拓展同一目标_仅一个成功_另一个退还指标() throws Exception {
        // 两上级(a,b)并发抢同一目标(c)：原子 claim 保证恰一个成功，另一个 errHasParent 且退还指标
        String a = register("13900000411");
        String b = register("13900000412");
        String c = register("13900000413");
        String aid = (String) data(get("/api/user/me", a)).get("id");
        String bid = (String) data(get("/api/user/me", b)).get("id");
        String cid = (String) data(get("/api/user/me", c)).get("id");
        jdbc.update("UPDATE cms_user SET level=1, quota=5 WHERE id=?", aid);
        jdbc.update("UPDATE cms_user SET level=1, quota=5 WHERE id=?", bid);

        CountDownLatch start = new CountDownLatch(1);
        List<Integer> codes = Collections.synchronizedList(new ArrayList<>());
        ExecutorService ex = Executors.newFixedThreadPool(2);
        for (String tok : List.of(a, b)) {
            ex.submit(() -> {
                try { start.await(); } catch (InterruptedException ignored) {}
                codes.add((Integer) post("/api/user/expand", Map.of("targetId", cid), tok).get("code"));
            });
        }
        start.countDown();
        ex.shutdown();
        assertTrue(ex.awaitTermination(15, TimeUnit.SECONDS));

        long success = codes.stream().filter(x -> x == 0).count();
        long fail = codes.stream().filter(x -> x == 40905).count();
        assertEquals(1, success, "并发抢同一目标应恰一个成功");
        assertEquals(1, fail, "另一个应被拒 errHasParent");
        // 目标最终只有一个上级
        String parent = jdbc.queryForObject("SELECT parent_id FROM cms_user WHERE id=?", String.class, cid);
        assertTrue(parent.equals(aid) || parent.equals(bid));
        // 失败方退还指标：两人初始各 5，仅成功方扣 1，合计 9
        int aQ = jdbc.queryForObject("SELECT quota FROM cms_user WHERE id=?", Integer.class, aid);
        int bQ = jdbc.queryForObject("SELECT quota FROM cms_user WHERE id=?", Integer.class, bid);
        assertEquals(9, aQ + bQ, "失败方应退还指标，两人合计仅扣减 1");
    }

    @Test
    void 新注册level3用户可拓展并可升级_回归errLevel移除() {
        // 新注册用户默认 level=maxLevel=3。需求 REQ-LVL-07：普通客户(最低级)拓展后可升级。
        // 若误留 errLevel 拦截，level3 无法拓展 → 核心机制失效。此为回归保护。
        String a = register("13900000303");   // level3
        String aid = (String) data(get("/api/user/me", a)).get("id");
        Integer aLevel = jdbc.queryForObject("SELECT level FROM cms_user WHERE id=?", Integer.class, aid);
        assertEquals(3, aLevel, "新注册用户应为最低级 level3");
        jdbc.update("UPDATE cms_user SET quota=20 WHERE id=?", aid);  // 给足指标

        // a 拓展 3 个客户（upgrade.3=3 门槛）
        for (int i = 0; i < 3; i++) {
            String t = register("1390000031" + i);
            String tid = (String) data(get("/api/user/me", t)).get("id");
            Map<String, Object> r = post("/api/user/expand", Map.of("targetId", tid), a);
            assertEquals(0, r.get("code"), "level3 用户应可拓展: " + r);
            // 目标级别封顶为 3（min(maxLevel, 3+1)）
            Integer tLevel = jdbc.queryForObject("SELECT level FROM cms_user WHERE id=?", Integer.class, tid);
            assertEquals(3, tLevel);
        }
        // 拓展满 3 → 升级为 level2
        Integer after = jdbc.queryForObject("SELECT level FROM cms_user WHERE id=?", Integer.class, aid);
        assertEquals(2, after, "level3 拓展满门槛应升级为 level2");
    }

    @Test
    void home上级字段不暴露敏感PII() {
        // 上级信息仅 id/name/avatar，下游不应看到上级 idcard/phone/address
        String a = register("13900000921");
        String b = register("13900000922");
        String aid = (String) data(get("/api/user/me", a)).get("id");
        String bid = (String) data(get("/api/user/me", b)).get("id");
        // a 完善含敏感资料
        put("/api/user/profile", Map.of("name", "上级甲", "idcard", "330101199001011999",
                "phone", "13900000921", "address", "上级秘密地址"), a);
        // a 设为 level1 并拓展 b（让 b 的上级为 a）
        jdbc.update("UPDATE cms_user SET level=1, quota=8 WHERE id=?", aid);
        assertEquals(0, post("/api/user/expand", Map.of("targetId", bid), a).get("code"));

        // b 看 home，parent 应脱敏
        Map<String, Object> parent = (Map<String, Object>) data(get("/api/user/home", b)).get("parent");
        assertNotNull(parent);
        assertEquals(aid, parent.get("id"));
        assertNull(parent.get("idcard"), "home.parent 不得暴露 idcard");
        assertNull(parent.get("phone"), "home.parent 不得暴露 phone");
        assertNull(parent.get("address"), "home.parent 不得暴露 address");
    }

    // ==================== 可见性（N-U08/N-A09） ====================

    @Test
    void 客户跨级查看非下游详情_40301_调admin接口_40301() {
        String a = register("13900000401");
        String b = register("13900000402");
        String bid = (String) data(get("/api/user/me", b)).get("id");
        // a 无权看 b（非自己下游）
        Map<String, Object> r = get("/api/customers/" + bid, a);
        assertEquals(40301, r.get("code"), "客户不可跨级查看非下游客户");
        // a 调管理员接口 → 40301
        assertEquals(40301, del("/api/admin/customers/" + bid, a).get("code"));
        assertEquals(40301, put("/api/admin/customers/" + bid + "/status", Map.of("status", "disabled"), a).get("code"));
    }

    @Test
    void 管理员可查看任意客户并执行管理操作() {
        String adm = adminLogin();
        String a = register("13900000501");
        String aid = (String) data(get("/api/user/me", a)).get("id");
        // 管理员看任意
        assertEquals(0, get("/api/customers/" + aid, adm).get("code"));
        // 调指标
        assertEquals(0, put("/api/admin/customers/" + aid + "/quota", Map.of("quota", 30), adm).get("code"));
        assertEquals(30, jdbc.queryForObject("SELECT quota FROM cms_user WHERE id=?", Integer.class, aid));
        // 伪删除
        assertEquals(0, del("/api/admin/customers/" + aid, adm).get("code"));
        assertEquals("deleted", jdbc.queryForObject("SELECT status FROM cms_user WHERE id=?", String.class, aid));
    }

    @Test
    void 禁用账户登录被拒40904() {
        String adm = adminLogin();
        String a = register("13900000601");
        String aid = (String) data(get("/api/user/me", a)).get("id");
        put("/api/admin/customers/" + aid + "/status", Map.of("status", "disabled"), adm);
        Map<String, Object> r = post("/api/auth/login", Map.of("username", "13900000601", "password", "123456"), null);
        assertEquals(40904, r.get("code"));
    }

    @Test
    void 统计报表数值正确() {
        String adm = adminLogin();
        register("13900000701");
        register("13900000702");
        Map<String, Object> stats = data(get("/api/admin/stats", adm));
        assertEquals(2, stats.get("total"));
        assertNotNull(stats.get("series7"));
        assertNotNull(stats.get("levelDist"));
    }

    // ==================== 资料 ====================

    @Test
    void 完善资料_profileDone翻转() {
        String a = register("13900000801");
        assertFalse((Boolean) data(get("/api/user/me", a)).get("profileDone"));
        put("/api/user/profile", Map.of("name", "张三", "idcard", "110101199001011234",
                "phone", "13900000801", "address", "北京市"), a);
        assertTrue((Boolean) data(get("/api/user/me", a)).get("profileDone"));
    }

    // ==================== IM：好友/群组/消息/禁言（N-A08/N-U07） ====================

    @Test
    void 加好友_建群_发消息_群禁言非群主被拒() {
        String a = register("13900000901");
        String b = register("13900000902");
        String aid = (String) data(get("/api/user/me", a)).get("id");
        String bid = (String) data(get("/api/user/me", b)).get("id");
        // a 加 b 为好友
        assertEquals(0, post("/api/friends", Map.of("targetId", bid), a).get("code"));
        // 好友列表互见
        assertTrue(((List<?>) get("/api/friends", a).get("data")).size() >= 1);
        assertTrue(((List<?>) get("/api/friends", b).get("data")).size() >= 1);
        // a 建群（成员 b）
        Map<String, Object> grp = post("/api/groups", Map.of("name", "测试群", "memberIds", List.of(bid)), a);
        assertEquals(0, grp.get("code"));
        @SuppressWarnings("unchecked")
        String gid = (String) ((Map<String, Object>) grp.get("data")).get("id");
        // a 发群消息
        assertEquals(0, post("/api/messages", Map.of("scopeType", "group", "groupId", gid, "content", "大家好"), a).get("code"));
        // b 发群消息（未禁言，可发）
        assertEquals(0, post("/api/messages", Map.of("scopeType", "group", "groupId", gid, "content", "群主好"), b).get("code"));
        // a 禁言群
        assertEquals(0, put("/api/groups/" + gid + "/muted", Map.of("muted", 1), a).get("code"));
        // b 再发 → 被拒 40907
        assertEquals(40907, post("/api/messages", Map.of("scopeType", "group", "groupId", gid, "content", "还能发吗"), b).get("code"));
        // a 群主仍可发
        assertEquals(0, post("/api/messages", Map.of("scopeType", "group", "groupId", gid, "content", "我还能发"), a).get("code"));
        // 单聊消息
        assertEquals(0, post("/api/messages", Map.of("scopeType", "dm", "peerId", bid, "content", "私聊你好"), a).get("code"));
        Map<String, Object> thread = get("/api/messages?scopeType=dm&peerId=" + bid, a);
        assertEquals(0, thread.get("code"));
    }

    @Test
    void 好友搜索与列表与群成员不暴露敏感PII() {
        // 隐私：陌生/半陌生场景仅返回 id/name/avatar，不得含 idcard/phone/address
        String a = register("13900000911");
        String b = register("13900000912");
        // b 先完善含敏感信息的资料
        String bid = (String) data(get("/api/user/me", b)).get("id");
        put("/api/user/profile", Map.of("name", "李四", "idcard", "110101199001011234",
                "phone", "13900000912", "address", "秘密地址"), b);

        // a 搜索 b（未加好友，陌生场景）
        Map<String, Object> cand = data(get("/api/friends/search?id=" + bid, a));
        assertNotNull(cand);
        assertEquals(bid, cand.get("id"));
        assertEquals("李四", cand.get("name"));
        assertNull(cand.get("idcard"), "搜索结果不得暴露 idcard");
        assertNull(cand.get("phone"), "搜索结果不得暴露 phone");
        assertNull(cand.get("address"), "搜索结果不得暴露 address");

        // 加好友后，列表仍脱敏
        post("/api/friends", Map.of("targetId", bid), a);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> friends = (List<Map<String, Object>>) get("/api/friends", a).get("data");
        Map<String, Object> f = friends.stream().filter(x -> bid.equals(x.get("id"))).findFirst().orElseThrow();
        assertNull(f.get("idcard"), "好友列表不得暴露 idcard");
        assertNull(f.get("address"), "好友列表不得暴露 address");

        // 群成员同样脱敏
        Map<String, Object> grp = post("/api/groups", Map.of("name", "g", "memberIds", List.of(bid)), a);
        @SuppressWarnings("unchecked")
        String gid = (String) ((Map<String, Object>) grp.get("data")).get("id");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> members = (List<Map<String, Object>>) ((Map<String, Object>) data(get("/api/groups/" + gid, a))).get("members");
        Map<String, Object> m = members.stream().filter(x -> bid.equals(x.get("id"))).findFirst().orElseThrow();
        assertNull(m.get("idcard"), "群成员列表不得暴露 idcard");
    }
}
