package com.cms.im.message;

import com.cms.common.BizException;
import com.cms.common.ResultCode;
import com.cms.im.group.Group;
import com.cms.im.group.mapper.GroupMapper;
import com.cms.im.group.mapper.GroupMemberMapper;
import com.cms.im.message.dto.MessageDtos;
import com.cms.im.message.mapper.MessageMapper;
import com.cms.user.User;
import com.cms.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 消息领域服务：会话历史、发消息。
 * 仅纯文本（REQ-IM-07）；群禁言时非群主不可发（REQ-IM-05）。
 */
@Service
public class MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final GroupMapper groupMapper;
    private final GroupMemberMapper memberMapper;

    public MessageService(MessageMapper messageMapper, UserMapper userMapper,
                          GroupMapper groupMapper, GroupMemberMapper memberMapper) {
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.groupMapper = groupMapper;
        this.memberMapper = memberMapper;
    }

    /** 会话消息列表 */
    public List<MessageDtos.MessageVO> thread(String meId, String scopeType, String peerId, String groupId) {
        if ("group".equalsIgnoreCase(scopeType)) {
            Group g = groupMapper.selectById(groupId);
            if (g == null || memberMapper.isMember(groupId, meId) <= 0) throw new BizException(ResultCode.FORBIDDEN);
            return toVO(messageMapper.selectByScope("group", groupId), meId);
        }
        // dm
        User peer = userMapper.selectById(peerId);
        if (peer == null || "deleted".equals(peer.getStatus())) throw new BizException(ResultCode.USER_NOT_FOUND);
        return toVO(messageMapper.selectByScope("dm", dmKey(meId, peerId)), meId);
    }

    /** 发消息（纯文本） */
    public MessageDtos.MessageVO send(String meId, MessageDtos.SendRequest req) {
        String text = req.content() == null ? "" : req.content().trim();
        if (text.isEmpty()) throw new BizException(ResultCode.BAD_PARAM);
        if (text.length() > 2000) throw new BizException(ResultCode.BAD_PARAM, "消息过长");

        String scopeType;
        String scopeKey;
        if ("group".equalsIgnoreCase(req.scopeType())) {
            String gid = req.groupId();
            Group g = groupMapper.selectById(gid);
            if (g == null) throw new BizException(ResultCode.GROUP_FAIL, "errNotFound");
            if (memberMapper.isMember(gid, meId) <= 0) throw new BizException(ResultCode.FORBIDDEN);
            // 群禁言：非群主不可发（REQ-IM-05）
            if (g.getMuted() != null && g.getMuted() == 1 && !g.getOwnerId().equals(meId)) {
                throw new BizException(ResultCode.GROUP_FAIL, "muted");
            }
            scopeType = "group";
            scopeKey = gid;
        } else {
            User peer = userMapper.selectById(req.peerId());
            if (peer == null || "deleted".equals(peer.getStatus())) throw new BizException(ResultCode.USER_NOT_FOUND);
            scopeType = "dm";
            scopeKey = dmKey(meId, req.peerId());
        }

        Message m = new Message();
        m.setScopeType(scopeType);
        m.setScopeKey(scopeKey);
        m.setSenderId(meId);
        m.setContent(text);
        m.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(m);

        User sender = userMapper.selectById(meId);
        return new MessageDtos.MessageVO(m.getId(), meId, sender != null ? sender.getName() : null,
                text, m.getCreatedAt(), true);
    }

    private List<MessageDtos.MessageVO> toVO(List<Message> msgs, String meId) {
        return msgs.stream().map(m -> {
            User u = userMapper.selectById(m.getSenderId());
            return new MessageDtos.MessageVO(m.getId(), m.getSenderId(),
                    u != null ? u.getName() : null, m.getContent(), m.getCreatedAt(),
                    m.getSenderId().equals(meId));
        }).toList();
    }

    /** 单聊会话 key：两 ID 字典序排序拼接 */
    public static String dmKey(String a, String b) {
        String[] arr = {a, b};
        Arrays.sort(arr);
        return arr[0] + "_" + arr[1];
    }
}
