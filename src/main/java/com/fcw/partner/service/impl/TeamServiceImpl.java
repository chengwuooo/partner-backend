package com.fcw.partner.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fcw.partner.model.domain.Team;
import com.fcw.partner.service.TeamService;
import com.fcw.partner.mapper.TeamMapper;
import org.springframework.stereotype.Service;

/**
* @author chengwu
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2024-04-29 17:23:07
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{

}




