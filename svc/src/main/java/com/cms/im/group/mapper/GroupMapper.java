package com.cms.im.group.mapper;

import com.cms.im.group.Group;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 群组 Mapper */
public interface GroupMapper {
    int insert(Group g);
    Group selectById(@Param("id") String id);
    /** 用户所在的所有群 */
    List<String> selectIdsByUser(@Param("userId") String userId);
    int updateMuted(@Param("id") String id, @Param("muted") int muted);
}
