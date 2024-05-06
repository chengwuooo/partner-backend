package com.fcw.partner.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fcw.partner.common.BaseResponse;
import com.fcw.partner.common.DeleteRequest;
import com.fcw.partner.common.ErrorCode;
import com.fcw.partner.common.ResultUtils;
import com.fcw.partner.exception.BusinessException;
import com.fcw.partner.model.domain.Team;
import com.fcw.partner.model.domain.User;
import com.fcw.partner.model.domain.UserTeam;
import com.fcw.partner.model.dto.TeamQuery;
import com.fcw.partner.model.request.TeamAddRequest;
import com.fcw.partner.model.request.TeamJoinRequest;
import com.fcw.partner.model.request.TeamQuitRequest;
import com.fcw.partner.model.vo.TeamUserVO;
import com.fcw.partner.service.TeamService;
import com.fcw.partner.service.UserService;
import com.fcw.partner.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户接口
 *
 * @author fcw
 */
@RestController
@RequestMapping("/team")
@Slf4j
//@CrossOrigin(origins = {"http://localhost:3000","http://47.109.196.49"})
public class TeamController {
    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;

    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        long result = teamService.addTeam(team, userService.getLoginUser(request));
        return ResultUtils.success(result);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.deleteTeam(id, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }
        return ResultUtils.success(true);
    }

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody Team team) {
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = teamService.updateById(team);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(team);
    }

    //    @PostMapping("/list")
//    public BaseResponse<List<Team>> listTeams(@RequestBody  TeamQuery teamQuery) {
//
//        if (teamQuery == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        System.out.println(teamQuery);
//        Team team = new Team();
//        BeanUtils.copyProperties(teamQuery, team);
//        System.out.println(team);
//        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
//        System.out.println(queryWrapper);
//        List<Team> teamList = teamService.list(queryWrapper);
//        return ResultUtils.success(teamList);
//    }
//
    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> listTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean isAdmin = userService.isAdmin(request);
        // 1、查询队伍列表
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, isAdmin);
        final List<Long> teamIdList = teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        // 2、判断当前用户是否已加入队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        try {
            User loginUser = userService.getLoginUser(request);
            userTeamQueryWrapper.eq("userId", loginUser.getId());
            userTeamQueryWrapper.in("teamId", teamIdList);
            List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
            // 已加入的队伍 id 集合
            Set<Long> hasJoinTeamIdSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
            teamList.forEach(team -> {
                boolean hasJoin = hasJoinTeamIdSet.contains(team.getId());
                team.setHasJoin(hasJoin);
            });
        } catch (Exception e) {
        }
        teamList = teamService.AddTeamsJoinNum( teamList,  teamIdList);
        return ResultUtils.success(teamList);
    }


    /**
     * 分页查询团队列表
     *
     * @param teamQuery 包含分页信息和团队查询条件的实体对象
     * @return 返回团队分页列表的响应对象
     */
//    @GetMapping("/list/page")
//    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery) {
//        // 校验团队查询参数是否为空，若为空则抛出业务异常
//        if (teamQuery == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        // 创建一个空的Team对象，用于后续设置查询条件
//        Team team = new Team();
//        // 从查询参数中获取每页的大小和页码
//        int pageSize = teamQuery.getPageSize();
//        int pageNum = teamQuery.getPageNum();
//        // 将查询参数的属性值复制到Team对象中，用于构建查询Wrapper
//        BeanUtils.copyProperties(teamQuery, team);
//        // 创建Page对象，用于分页查询
//        Page<Team> teamPage = new Page<>(pageSize, pageNum);
//
//        // 构建查询条件
//        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
//        // 执行分页查询
//        Page<Team> teamList = teamService.page(teamPage, queryWrapper);
//        // 将查询结果封装成成功响应对象返回
//        return ResultUtils.success(teamList);
//    }
// todo 查询分页
    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        Page<Team> page = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        Page<Team> resultPage = teamService.page(page, queryWrapper);
        return ResultUtils.success(resultPage);
    }

    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(result);
    }


    /**
     * 获取我创建的队伍
     *
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyCreateTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        teamQuery.setUserId(loginUser.getId());
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
        teamList = teamService.AddTeamsJoinNum(teamList);
        teamList.forEach(team -> {
            team.setHasJoin(true);
        });
        return ResultUtils.success(teamList);
    }

    /**
     * 获取我加入的队伍列表
     *
     * @param teamQuery 包含队伍查询条件的对象，例如队伍ID列表
     * @param request 用户的请求对象，用于获取登录用户信息
     * @return 返回一个包含我加入的队伍信息的列表
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeams(TeamQuery teamQuery, HttpServletRequest request) {
        // 校验查询参数是否为空
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取登录用户信息
        User loginUser = userService.getLoginUser(request);
        // 构建查询条件，查询我加入的队伍
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        // 将查询结果按队伍ID分组，以便后续处理
        Map<Long, List<UserTeam>> listMap = userTeamList.stream()
                .collect(Collectors.groupingBy(UserTeam::getTeamId));
        // 提取分组后的队伍ID列表
        List<Long> idList = new ArrayList<>(listMap.keySet());
        // 更新查询条件，指定需要查询的队伍ID列表
        teamQuery.setIdList(idList);
        System.out.println("teamQuery"+teamQuery);
        // 查询并返回满足条件的队伍信息列表
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
        final List<Long> teamIdList = teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        teamList.forEach(team -> {
            team.setHasJoin(true);
        });
        teamList = teamService.AddTeamsJoinNum(teamList,  teamIdList);
        // 构建并返回成功响应
        return ResultUtils.success(teamList);
    }

}




