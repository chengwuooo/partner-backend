package com.fcw.partner.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fcw.partner.model.domain.User;
import com.fcw.partner.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class PreCacheJob {
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    // 重点用户
    private final List<Long> mainUserList = Collections.singletonList(1L);

    // 每天执行，预热推荐用户
    @Scheduled(cron = "0 1 * * * *")   //自己设置时间测试
    public void doCacheRecommendUser() {
        //查数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        Page<User> userPage = userService.page(new Page<>(1,100),queryWrapper);
        String redisKey = String.format("recommendUsers:%s",mainUserList);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        //写缓存,30s过期
        try {
            valueOperations.set(redisKey,userPage,30000, TimeUnit.MILLISECONDS);
        } catch (Exception e){
            log.error("redis set key error",e);
        }
    }
}