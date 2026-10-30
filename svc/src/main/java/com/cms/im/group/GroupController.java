package com.cms.im.group;

import com.cms.common.CurrentUser;
import com.cms.common.Result;
import com.cms.im.group.dto.GroupDtos;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 群组接口 */
@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public Result<List<GroupDtos.GroupVO>> myGroups() {
        return Result.ok(groupService.myGroups(CurrentUser.requireCustomerId()));
    }

    @PostMapping
    public Result<GroupDtos.GroupDetail> create(@RequestBody GroupDtos.CreateGroupRequest req) {
        return Result.ok(groupService.create(CurrentUser.requireCustomerId(), req));
    }

    @GetMapping("/{id}")
    public Result<GroupDtos.GroupDetail> detail(@PathVariable String id) {
        return Result.ok(groupService.detail(id, CurrentUser.requireCustomerId()));
    }

    @PostMapping("/{id}/members")
    public Result<Void> addMembers(@PathVariable String id, @RequestBody GroupDtos.AddMembersRequest req) {
        groupService.addMembers(id, CurrentUser.requireCustomerId(), req);
        return Result.ok();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public Result<Void> removeMember(@PathVariable String id, @PathVariable String userId) {
        groupService.removeMember(id, CurrentUser.requireCustomerId(), userId);
        return Result.ok();
    }

    @PutMapping("/{id}/muted")
    public Result<Void> setMuted(@PathVariable String id, @RequestBody GroupDtos.MuteRequest req) {
        int muted = (req.muted() == null ? 0 : (req.muted() != 0 ? 1 : 0));
        groupService.setMuted(id, CurrentUser.requireCustomerId(), muted);
        return Result.ok();
    }
}
