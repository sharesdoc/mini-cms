package com.cms.im.message.dto;

import java.time.LocalDateTime;

/** 消息相关 DTO */
public final class MessageDtos {

    public record MessageVO(
            Long id, String senderId, String senderName, String content,
            LocalDateTime createdAt, boolean mine
    ) {}

    public record SendRequest(
            String scopeType,   // dm | group
            String peerId,      // scopeType=dm 时必填
            String groupId,     // scopeType=group 时必填
            String content
    ) {}

    private MessageDtos() {}
}
