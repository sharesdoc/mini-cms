package com.cms.im.friend;

import com.cms.common.BizException;
import com.cms.common.ResultCode;
import com.cms.im.friend.mapper.FriendshipMapper;
import com.cms.user.User;
import com.cms.user.dto.PublicUserVO;
import com.cms.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 好友领域服务：按 ID 搜索、添加好友（对称）、好友列表。
 * 对外返回脱敏 PublicUserVO（仅 id/name/avatar），不暴露 idcard/phone/address。
 * 校验失败以 i18n key 作为异常 message。
 */
@Service
public class FriendService {

    private final FriendshipMapper friendshipMapper;
    private final UserMapper userMapper;

    public FriendService(FriendshipMapper friendshipMapper, UserMapper userMapper) {
        this.friendshipMapper = friendshipMapper;
        this.userMapper = userMapper;
    }

    /** 按 11 位 ID 搜索可添加的用户（排除自己/已删；返回脱敏信息） */
    public PublicUserVO search(String meId, String targetId) {
        if (targetId == null || targetId.isBlank()) return null;
        User u = userMapper.selectById(targetId.trim());
        if (u == null || "deleted".equals(u.getStatus())) return null;
        if (u.getId().equals(meId)) throw biz("errFriendSelf");
        return PublicUserVO.of(u);
    }

    /** 添加好友（对称两行） */
    public void add(String meId, String targetId) {
        String tid = targetId == null ? "" : targetId.trim();
        if (tid.equals(meId)) throw biz("errFriendSelf");
        User u = userMapper.selectById(tid);
        if (u == null || "deleted".equals(u.getStatus())) throw biz("noCandidates");
        if (friendshipMapper.exists(meId, tid) > 0) throw biz("errFriendExists");
        friendshipMapper.insertPair(meId, tid, LocalDateTime.now());
    }

    /** 好友列表（脱敏） */
    public List<PublicUserVO> list(String meId) {
        List<String> ids = friendshipMapper.selectFriendIds(meId);
        return ids.stream()
                .map(userMapper::selectById)
                .filter(u -> u != null && !"deleted".equals(u.getStatus()))
                .map(PublicUserVO::of)
                .toList();
    }

    private static BizException biz(String i18nKey) {
        return new BizException(ResultCode.FRIEND_FAIL, i18nKey);
    }
}
