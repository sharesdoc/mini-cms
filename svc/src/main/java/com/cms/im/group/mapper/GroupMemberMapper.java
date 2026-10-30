package com.cms.im.group.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 群成员 Mapper */
public interface GroupMemberMapper {
    int insertOne(@Param("groupId") String groupId, @Param("userId") String userId, @Param("now") LocalDateTime now);
    int insertBatch(@Param("groupId") String groupId, @Param("userIds") List<String> userIds, @Param("now") LocalDateTime now);
    List<String> selectMembers(@Param("groupId") String groupId);
    int isMember(@Param("groupId") String groupId, @Param("userId") String userId);
    int deleteMember(@Param("groupId") String groupId, @Param("userId") String userId);
    int countMembers(@Param("groupId") String groupId);
}
