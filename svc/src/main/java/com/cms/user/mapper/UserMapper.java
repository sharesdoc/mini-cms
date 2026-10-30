package com.cms.user.mapper;

import com.cms.user.User;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户表 MyBatis Mapper。所有用户输入一律用 #{} 占位，防注入。
 */
public interface UserMapper {

    User selectById(@Param("id") String id);

    User selectByUsername(@Param("username") String username);

    /** 用户名是否存在（排除已删除） */
    int countByUsername(@Param("username") String username);

    int insert(User user);

    int updateProfile(@Param("id") String id, @Param("name") String name,
                      @Param("idcard") String idcard, @Param("phone") String phone,
                      @Param("address") String address, @Param("profileDone") int profileDone);

    int updateAvatar(@Param("id") String id, @Param("avatar") int avatar);

    int updatePassword(@Param("id") String id, @Param("password") String password);

    /**
     * 原子认领目标为客户下游：仅当目标 parent_id IS NULL 时设置上级与级别。
     * 返回受影响行数（0 表示目标已被并发抢走或有上级）。消除「两上级并发抢同一目标」竞态。
     */
    int claimTarget(@Param("id") String id, @Param("parentId") String parentId, @Param("level") int level);

    /** 指标 +1（用于并发抢目标失败时退还刚扣的指标） */
    int incrementQuota(@Param("id") String id);

    int updateLevel(@Param("id") String id, @Param("level") int level);

    int updateQuota(@Param("id") String id, @Param("quota") int quota);

    /**
     * 原子扣减 1 个可拓展指标：仅当 quota>0 时 quota=quota-1。
     * 返回受影响行数（0 表示无指标）。用于消除「读-检查-写」并发超卖竞态。
     */
    int decrementQuota(@Param("id") String id);

    int updateStatus(@Param("id") String id, @Param("status") String status);

    /** 直接子节点（活跃） */
    List<User> selectChildren(@Param("parentId") String parentId);

    /** 统计某父节点的活跃子节点数 */
    int countChildren(@Param("parentId") String parentId);

    /** 全部活跃客户（status != deleted） */
    List<User> selectAllLive();

    List<User> selectAll();

    /** 按级别筛选活跃客户 */
    List<User> selectLiveByLevel(@Param("level") int level);

    /** 搜索：姓名 / ID / 用户名（活跃） */
    List<User> searchLive(@Param("q") String q);

    /** 区间内注册的客户（按 created_at） */
    List<User> selectByCreatedAfter(@Param("from") LocalDateTime from);

    /** 最近 N 个注册（活跃） */
    List<User> selectRecent(@Param("n") int n);
}
