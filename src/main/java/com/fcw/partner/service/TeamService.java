package com.fcw.partner.service;

import com.fcw.partner.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fcw.partner.model.domain.User;
import com.fcw.partner.model.dto.TeamQuery;
import com.fcw.partner.model.request.TeamJoinRequest;
import com.fcw.partner.model.request.TeamQuitRequest;
import com.fcw.partner.model.request.TeamUpdateRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
* @author chengwu
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2024-04-29 17:23:07
*/
public interface TeamService extends IService<Team> {
    /**
     * 创建队伍
     * @param team
     * @param loginUser
     * @return
     */
    long addTeam(Team team, User loginUser);

    List listTeams(TeamQuery teamQuery, boolean isAdmin);

    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    @Transactional(rollbackFor = Exception.class)
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);


    @Transactional(rollbackFor = Exception.class)
    boolean deleteTeam(long id, User loginUser);
}
