package com.fcw.partner.mapper;

import com.fcw.partner.model.domain.Follows;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fcw.partner.model.domain.User;
import io.lettuce.core.dynamic.annotation.Param;

import java.util.List;

/**
* @author chengwu
* @description 针对表【follows(关注表)】的数据库操作Mapper
* @createDate 2024-07-10 19:29:39
* @Entity com.fcw.partner.model.domain.Follows
*/
public interface FollowsMapper extends BaseMapper<Follows> {
    List<User> listMutualFollowUsers(@Param("loginUserId") long loginUserId, @Param("isActive") boolean isActive);
}




