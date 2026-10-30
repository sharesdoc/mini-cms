package com.cms.im.message;

import com.cms.common.CurrentUser;
import com.cms.common.Result;
import com.cms.im.message.dto.MessageDtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 消息接口：会话历史 + 发消息 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public Result<List<MessageDtos.MessageVO>> thread(
            @RequestParam String scopeType,
            @RequestParam(required = false) String peerId,
            @RequestParam(required = false) String groupId) {
        String uid = CurrentUser.requireCustomerId();
        return Result.ok(messageService.thread(uid, scopeType, peerId, groupId));
    }

    @PostMapping
    public Result<MessageDtos.MessageVO> send(@RequestBody MessageDtos.SendRequest req) {
        String uid = CurrentUser.requireCustomerId();
        return Result.ok(messageService.send(uid, req));
    }
}
