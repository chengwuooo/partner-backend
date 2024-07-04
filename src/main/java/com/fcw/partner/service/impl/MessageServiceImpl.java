package com.fcw.partner.service.impl;


import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fcw.partner.common.ErrorCode;
import com.fcw.partner.exception.BusinessException;
import com.fcw.partner.model.domain.Message;
import com.fcw.partner.model.domain.User;
import com.fcw.partner.model.request.MessageRequest;
import com.fcw.partner.model.vo.MessageVo;
import com.fcw.partner.service.MessageService;
import com.fcw.partner.mapper.MessageMapper;
import com.fcw.partner.service.TeamService;
import com.fcw.partner.service.UserService;
import net.bytebuddy.implementation.bytecode.Throw;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.fcw.partner.constant.MessageConstant.MESSAGE_HALL;
import static com.fcw.partner.constant.MessageConstant.MESSAGE_PRIVATE;

/**
 * @author chengwu
 * @description 针对表【message(聊天消息表)】的数据库操作Service实现
 * @createDate 2024-07-04 21:13:20
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message>
        implements MessageService {

    @Resource
    private RedisTemplate<String, List<MessageVo>> redisTemplate;

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    /**
     * 保存消息到缓存中。
     *
     * @param redisKey   缓存的key。
     * @param id         与消息关联的唯一标识，如会话ID。
     * @param messageVos 需要保存的消息列表。
     */
    @Override
    public void saveCache(String redisKey, String id, List<MessageVo> messageVos) {
        try {
            ValueOperations<String, List<MessageVo>> valueOperations = redisTemplate.opsForValue();

            // 基础过期时间（2分钟）
            long baseExpireTime = 2L;
            // 随机增加0到2分钟，避免集中过期
            long randomExtension = RandomUtil.randomLong(0,120); // 转换为秒

            long expireTime = baseExpireTime * 60 + randomExtension; // 总过期时间，单位转换为秒

            if (redisKey.equals(MESSAGE_HALL)) {
                // todo:对于公共聊天室，可以考虑更短的基础过期时间以加快信息流动
                valueOperations.set(redisKey, messageVos, expireTime, TimeUnit.SECONDS);
            } else {
                // todo:私人聊天可以根据实际情况调整过期时间策略
                valueOperations.set(redisKey + id, messageVos, expireTime, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
    }

    /**
     * 从缓存中获取指定key和id对应的消息列表。
     *
     * @param redisKey 缓存的key。
     * @param id       与消息关联的唯一标识，如会话ID。
     * @return 消息列表。
     */
    @Override
    public List<MessageVo> getCache(String redisKey, String id) {
        ValueOperations<String, List<MessageVo>> valueOperations = redisTemplate.opsForValue();
        List<MessageVo> chatRecords;
        if (redisKey.equals(MESSAGE_HALL)) {
            chatRecords = valueOperations.get(redisKey);
        } else {
            chatRecords = valueOperations.get(redisKey + id);
        }
        return chatRecords;
    }

    /**
     * 获取私聊消息记录。
     *
     * @param chatRequest 包含聊天请求详情的对象。
     * @param chatType    聊天类型，用于区分不同的聊天场景。
     * @param loginUser   当前登录用户的信息。
     * @return 私聊消息列表。
     */
    @Override
    public List<MessageVo> getPrivateChat(MessageRequest chatRequest, int chatType, User loginUser) {
        Long fromId = chatRequest.getFromId();
        Long toId = chatRequest.getToId();
        if (fromId == null || toId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "发送者ID和接收者ID不能为空");
        }

        //查看消息有没有缓存
        List<MessageVo> messageVos = getCache(MESSAGE_PRIVATE, fromId + "-" + toId);
        if (messageVos != null && !messageVos.isEmpty()) {
            return messageVos;
        }

        final LambdaQueryWrapper<Message> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        lambdaQueryWrapper
                .and(wrapper -> wrapper
                        .eq(Message::getFromId, fromId).eq(Message::getToId, toId)
                        .or()
                        .eq(Message::getFromId, toId).eq(Message::getToId, fromId))
                .eq(Message::getIsDelete, 0)
                .eq(Message::getMessageType, chatType)
                .orderByAsc(Message::getDate);

        List<Message> list = this.list(lambdaQueryWrapper);

        final List<MessageVo> messageVoList = list.stream().map(message -> {
            String text = message.getText();
            Integer messageType = message.getMessageType();
            Date date = message.getDate();
            MessageVo messageVo = chatResult(fromId, toId, text, messageType, date);
            if (loginUser.getId().equals(message.getFromId()) || loginUser.getId().equals(message.getToId())) {
                messageVo.setIsMy(true);
            }
            return messageVo;
        }).collect(Collectors.toList());

        saveCache(MESSAGE_PRIVATE, fromId + "-" + toId, messageVoList);

        return messageVoList;
    }

    /**
     * 获取大厅聊天消息记录。
     *
     * @param chatType  聊天类型，用于区分不同的聊天场景。
     * @param loginUser 当前登录用户的信息。
     * @return 大厅聊天消息列表。
     */
    @Override
    public List<MessageVo> getHallChat(int chatType, User loginUser) {
        return Collections.emptyList();
    }

    /**
     * 根据消息内容创建聊天结果对象。
     *
     * @param fromId     发送者ID。
     * @param toId       接收者ID。
     * @param text       消息文本内容。
     * @param chatType   聊天类型。
     * @param createTime 消息创建时间。
     * @return 聊天结果的VO对象。
     */
    @Override
    public MessageVo chatResult(Long fromId, Long toId, String text, Integer chatType, Date createTime) {
        return null;
    }

    /**
     * 获取团队聊天消息记录。
     *
     * @param chatRequest 包含聊天请求详情的对象。
     * @param chatType    聊天类型，用于区分不同的聊天场景。
     * @param loginUser   当前登录用户的信息。
     * @return 团队聊天消息列表。
     */
    @Override
    public List<MessageVo> getTeamChat(MessageRequest chatRequest, int chatType, User loginUser) {
        return Collections.emptyList();
    }

    /**
     * 根据指定的key和id删除缓存中的消息数据。
     *
     * @param key 缓存的key。
     * @param id  与消息关联的唯一标识，如会话ID。
     */
    @Override
    public void deleteKey(String key, String id) {

    }
}




