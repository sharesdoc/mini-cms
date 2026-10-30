package com.cms.im.message.mapper;

import com.cms.im.message.Message;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 消息 Mapper */
public interface MessageMapper {
    int insert(Message m);

    /** 会话消息列表（按时间升序） */
    List<Message> selectByScope(@Param("scopeType") String scopeType, @Param("scopeKey") String scopeKey);

    /** 会话最后一条消息 */
    Message selectLast(@Param("scopeType") String scopeType, @Param("scopeKey") String scopeKey);
}
