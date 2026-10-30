package com.cms.im.group;

import com.cms.common.BizException;
import com.cms.common.ResultCode;
import com.cms.im.friend.mapper.FriendshipMapper;
import com.cms.im.group.dto.GroupDtos;
import com.cms.im.group.mapper.GroupMapper;
import com.cms.im.group.mapper.GroupMemberMapper;
import com.cms.im.message.Message;
import com.cms.im.message.mapper.MessageMapper;
import com.cms.user.User;
import com.cms.user.dto.PublicUserVO;
import com.cms.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 群组领域服务：建群、列表、详情、加/删成员（群主）、禁言（群主）。
 * 成员须为群主好友（REQ-IM-03「从好友列表中选择成员」）。
 */
@Service
public class GroupService {

    private final GroupMapper groupMapper;
    private final GroupMemberMapper memberMapper;
    private final FriendshipMapper friendshipMapper;
    private final UserMapper userMapper;
    private final MessageMapper messageMapper;

    public GroupService(GroupMapper groupMapper, GroupMemberMapper memberMapper,
                        FriendshipMapper friendshipMapper, UserMapper userMapper,
                        MessageMapper messageMapper) {
        this.groupMapper = groupMapper;
        this.memberMapper = memberMapper;
        this.friendshipMapper = friendshipMapper;
        this.userMapper = userMapper;
        this.messageMapper = messageMapper;
    }

    @Transactional
    public GroupDtos.GroupDetail create(String meId, GroupDtos.CreateGroupRequest req) {
        String name = req.name() == null ? "" : req.name().trim();
        if (name.isEmpty()) throw biz("errGroupName");
        List<String> members = req.memberIds() == null ? List.of() : req.memberIds();
        if (members.isEmpty()) throw biz("errGroupMember");
        // 成员须为我的好友且存在
        for (String mid : members) {
            if (mid.equals(meId)) continue;
            if (friendshipMapper.exists(meId, mid) <= 0) throw biz("errGroupMember");
        }
        Group g = new Group();
        g.setId("g" + System.currentTimeMillis() + (int) (Math.random() * 1000));
        g.setName(name);
        g.setOwnerId(meId);
        g.setMuted(0);
        g.setCreatedAt(LocalDateTime.now());
        groupMapper.insert(g);

        List<String> all = new ArrayList<>(members);
        if (!all.contains(meId)) all.add(meId);
        memberMapper.insertBatch(g.getId(), all, g.getCreatedAt());

        return detail(g.getId(), meId);
    }

    /** 我的群列表（带成员数 + 最后消息预览） */
    public List<GroupDtos.GroupVO> myGroups(String meId) {
        List<String> ids = groupMapper.selectIdsByUser(meId);
        List<GroupDtos.GroupVO> out = new ArrayList<>();
        for (String gid : ids) {
            Group g = groupMapper.selectById(gid);
            if (g == null) continue;
            Message last = messageMapper.selectLast("group", gid);
            out.add(new GroupDtos.GroupVO(g.getId(), g.getName(), g.getOwnerId(), g.getMuted(),
                    memberMapper.countMembers(gid), g.getCreatedAt(),
                    last != null ? last.getContent() : null, last != null ? last.getCreatedAt() : null));
        }
        return out;
    }

    /** 群详情（须为成员） */
    public GroupDtos.GroupDetail detail(String groupId, String meId) {
        Group g = groupMapper.selectById(groupId);
        if (g == null) throw new BizException(ResultCode.GROUP_FAIL, "errNotFound");
        if (memberMapper.isMember(groupId, meId) <= 0) throw new BizException(ResultCode.FORBIDDEN);
        List<PublicUserVO> members = memberMapper.selectMembers(groupId).stream()
                .map(userMapper::selectById)
                .filter(u -> u != null && !"deleted".equals(u.getStatus()))
                .map(PublicUserVO::of).toList();
        return new GroupDtos.GroupDetail(g.getId(), g.getName(), g.getOwnerId(), g.getMuted(),
                g.getOwnerId().equals(meId), members);
    }

    /** 加成员（仅群主，新成员须为群主好友） */
    @Transactional
    public void addMembers(String groupId, String meId, GroupDtos.AddMembersRequest req) {
        Group g = mustOwn(groupId, meId);
        List<String> userIds = req == null || req.userIds() == null ? List.of() : req.userIds();
        if (userIds.isEmpty()) throw biz("errGroupMember");
        for (String uid : userIds) {
            if (!uid.equals(g.getOwnerId()) && friendshipMapper.exists(meId, uid) <= 0) throw biz("errGroupMember");
        }
        memberMapper.insertBatch(groupId, userIds, LocalDateTime.now());
    }

    /** 删成员（仅群主，不可删群主自己） */
    public void removeMember(String groupId, String meId, String userId) {
        mustOwn(groupId, meId);
        if (userId.equals(meId)) throw new BizException(ResultCode.BAD_PARAM);
        memberMapper.deleteMember(groupId, userId);
    }

    /** 禁言/解禁（仅群主，REQ-IM-05） */
    public void setMuted(String groupId, String meId, int muted) {
        mustOwn(groupId, meId);
        groupMapper.updateMuted(groupId, muted);
    }

    // ---- helpers ----

    private Group mustOwn(String groupId, String meId) {
        Group g = groupMapper.selectById(groupId);
        if (g == null) throw new BizException(ResultCode.GROUP_FAIL, "errNotFound");
        if (!g.getOwnerId().equals(meId)) throw new BizException(ResultCode.FORBIDDEN);
        return g;
    }

    private static BizException biz(String i18nKey) {
        return new BizException(ResultCode.GROUP_FAIL, i18nKey);
    }
}
