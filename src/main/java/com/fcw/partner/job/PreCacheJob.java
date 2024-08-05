package com.fcw.partner.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fcw.partner.model.domain.User;
import com.fcw.partner.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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
    @Resource
    private RedissonClient redissonClient;


    @Scheduled(fixedRate = 30000)
    public void doCacheRecommendUser() {
        RLock lock = redissonClient.getLock("partner:preCacheJob:doCache:lock");
        try {
            if (lock.tryLock(0, -1, TimeUnit.SECONDS)) {
                System.out.println("获取到预热锁" + Thread.currentThread().getId());
                //查数据库
                QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                Page<User> userPage = userService.page(new Page<>(1, 10), queryWrapper);
                String redisKey = String.format("partner:recommendUsers:101");
                ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
                //写缓存,30s过期
                try {
                    valueOperations.set(redisKey, userPage, 30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("redis set key error", e);
                }

            }
        } catch (InterruptedException e) {
            log.error("get lock error", e);
        } finally {
            System.out.println("释放锁" + Thread.currentThread().getId());
            //只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }
}