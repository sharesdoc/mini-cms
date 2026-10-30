package com.cms.im.friend;

import com.cms.common.CurrentUser;
import com.cms.common.Result;
import com.cms.user.dto.PublicUserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 好友接口（返回脱敏 PublicUserVO） */
@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping
    public Result<List<PublicUserVO>> list() {
        return Result.ok(friendService.list(CurrentUser.requireCustomerId()));
    }

    @GetMapping("/search")
    public Result<PublicUserVO> search(@RequestParam("id") String id) {
        return Result.ok(friendService.search(CurrentUser.requireCustomerId(), id));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Map<String, String> body) {
        friendService.add(CurrentUser.requireCustomerId(), body.get("targetId"));
        return Result.ok();
    }
}
