package com.cms.im.friend.mapper;

import com.cms.im.friend.Friendship;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 好友关系 Mapper（对称存储两行） */
public interface FriendshipMapper {
    /** 我的好友 ID 列表 */
    List<String> selectFriendIds(@Param("userId") String userId);

    /** 是否已是好友（单向存在即视为已建立） */
    int exists(@Param("userId") String userId, @Param("friendId") String friendId);

    /** 对称插入两行 */
    int insertPair(@Param("a") String a, @Param("b") String b, @Param("now") java.time.LocalDateTime now);

    /** 我的所有好友完整记录 */
    List<Friendship> selectAll(@Param("userId") String userId);
}
