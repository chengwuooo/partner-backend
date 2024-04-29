package com.fcw.partner.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fcw.partner.model.domain.UserTeam;
import com.fcw.partner.service.UserTeamService;
import com.fcw.partner.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
* @author chengwu
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2024-04-29 17:26:03
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

}




