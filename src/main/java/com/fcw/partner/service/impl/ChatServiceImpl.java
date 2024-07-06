package com.fcw.partner.service.impl;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fcw.partner.common.ErrorCode;
import com.fcw.partner.exception.BusinessException;
import com.fcw.partner.model.domain.Chat;
import com.fcw.partner.model.domain.Team;
import com.fcw.partner.model.domain.User;
import com.fcw.partner.model.request.ChatRequest;
import com.fcw.partner.model.vo.ChatVo;
import com.fcw.partner.model.vo.WebSocketVo;
import com.fcw.partner.service.ChatService;
import com.fcw.partner.mapper.ChatMapper;
import com.fcw.partner.service.TeamService;
import com.fcw.partner.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.fcw.partner.constant.ChatConstant.*;
import static com.fcw.partner.constant.UserConstant.ADMIN_ROLE;

/**
 * @author chengwu
 * @description 针对表【chat(聊天消息表)】的数据库操作Service实现
 * @createDate 2024-07-04 21:13:20
 */
@Service
public class ChatServiceImpl extends ServiceImpl<ChatMapper, Chat>
        implements ChatService {

    @Resource
    private RedisTemplate<String, List<ChatVo>> redisTemplate;

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    /**
     * 获取私聊消息记录。
     *
     * @param chatRequest 包含聊天请求详情的对象。
     * @param chatType    聊天类型，用于区分不同的聊天场景。
     * @param loginUser      当前登录用户的信息。
     * @return 私聊消息列表。
     */
    @Override
    public List<ChatVo> getPrivateChat(ChatRequest chatRequest, int chatType, User loginUser) {
        Long fromId = chatRequest.getFromId();
        Long toId = chatRequest.getToId();
        if (fromId == null || toId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "发送者ID和接收者ID不能为空");
        }

        //查看消息有没有缓存
        List<ChatVo> chatVos = getCache(CHAT_PRIVATE, fromId + "-" + toId);
        if (chatVos != null && !chatVos.isEmpty()) {
            return chatVos;
        }

        final LambdaQueryWrapper<Chat> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        lambdaQueryWrapper
                .and(wrapper -> wrapper
                        .eq(Chat::getFromId, fromId).eq(Chat::getToId, toId)
                        .or()
                        .eq(Chat::getFromId, toId).eq(Chat::getToId, fromId))
                .eq(Chat::getIsDelete, 0)
                .eq(Chat::getChatType, chatType)
                .orderByAsc(Chat::getDate);

        List<Chat> list = this.list(lambdaQueryWrapper);

        final List<ChatVo> chatVoList = list.stream().map(chat -> {
            String text = chat.getText();
            Date date = chat.getDate();
            ChatVo chatVo = getChatVo(fromId, toId, text, chatType, date);
            if (loginUser.getId().equals(chat.getFromId()) || loginUser.getId().equals(chat.getToId())) {
                chatVo.setIsMy(true);
            }
            return chatVo;
        }).collect(Collectors.toList());

        saveCache(CHAT_PRIVATE, fromId + "-" + toId, chatVoList);

        return chatVoList;
    }

    /**
     * 获取大厅聊天消息记录。
     *
     * @param chatType 聊天类型，用于区分不同的聊天场景。
     * @param loginUser   当前登录用户的信息。
     * @return 大厅聊天消息列表。
     */
    @Override
    public List<ChatVo> getHallChat(int chatType, User loginUser) {
        List<ChatVo> chatRecords = getCache(CHAT_HALL, String.valueOf(loginUser.getId()));
        if (chatRecords != null) {
            List<ChatVo> chatVos = checkIsMyChat(loginUser, chatRecords);
            saveCache(CHAT_HALL, String.valueOf(loginUser.getId()), chatVos);
            return chatVos;
        }
        LambdaQueryWrapper<Chat> chatLambdaQueryWrapper = new LambdaQueryWrapper<>();
        chatLambdaQueryWrapper.eq(Chat::getChatType, chatType);
        List<ChatVo> chatVos = returnChat(loginUser, null, chatLambdaQueryWrapper);
        saveCache(CHAT_HALL, String.valueOf(loginUser.getId()), chatVos);
        return chatVos;
    }

    /**
     * 获取团队聊天消息记录。
     *
     * @param chatRequest 包含聊天请求详情的对象。
     * @param chatType    聊天类型，用于区分不同的聊天场景。
     * @param loginUser      当前登录用户的信息。
     * @return 团队聊天消息列表。
     */
    @Override
    public List<ChatVo> getTeamChat(ChatRequest chatRequest, int chatType, User loginUser) {
        Long teamId = chatRequest.getToId();
        if (teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求有误");
        }
        List<ChatVo> chatRecords = getCache(CHAT_TEAM, String.valueOf(teamId));
        if (chatRecords != null) {
            List<ChatVo> chatVos = checkIsMyChat(loginUser, chatRecords);
            saveCache(CHAT_TEAM, String.valueOf(teamId), chatVos);
            return chatVos;
        }
        Team team = teamService.getById(teamId);
        LambdaQueryWrapper<Chat> chatLambdaQueryWrapper = new LambdaQueryWrapper<>();
        chatLambdaQueryWrapper
                .eq(Chat::getChatType, chatType)
                .eq(Chat::getToId, teamId);
        List<ChatVo> chatVos = returnChat(loginUser, team.getUserId(), chatLambdaQueryWrapper);
        saveCache(CHAT_TEAM, String.valueOf(teamId), chatVos);
        return chatVos;
    }


    private List<ChatVo> checkIsMyChat(User loginUser, List<ChatVo> chatRecords) {
        return chatRecords.stream().peek(chat -> {
            if (!chat.getFromUser().getId().equals(loginUser.getId()) && chat.getIsMy()) {
                chat.setIsMy(false);
            }
            if (chat.getFromUser().getId().equals(loginUser.getId()) && !chat.getIsMy()) {
                chat.setIsMy(true);
            }
        }).collect(Collectors.toList());
    }


    private List<ChatVo> returnChat(User loginUser, Long userId, LambdaQueryWrapper<Chat> chatLambdaQueryWrapper) {
        List<Chat> chatList = this.list(chatLambdaQueryWrapper);
        return chatList.stream().map(chat -> {
            ChatVo chatVo = getChatVo(chat.getFromId(), chat.getText());
            boolean isCaptain = userId != null && userId.equals(chat.getFromId());
            if (userService.getById(chat.getFromId()).getUserRole().equals(ADMIN_ROLE) || isCaptain) {
                chatVo.setIsAdmin(true);
            }
            if (chat.getFromId().equals(loginUser.getId())) {
                chatVo.setIsMy(true);
            }
            chatVo.setChatType(chat.getChatType());
            chatVo.setCreateTime(DateUtil.format(chat.getDate(), "yyyy年MM月dd日 HH:mm:ss"));
            return chatVo;
        }).collect(Collectors.toList());
    }

    /**
     * 根据消息内容创建聊天结果对象。
     *
     * @return 聊天结果的VO对象。
     */
    @Override
    public ChatVo getChatVo(Long fromId, Long toId, String text, Integer chatType, Date createTime) {
        ChatVo chatVo = new ChatVo();
        User fromUser = userService.getById(fromId);
        User toUser = userService.getById(toId);
        WebSocketVo fromWebSocketVo = new WebSocketVo();
        WebSocketVo toWebSocketVo = new WebSocketVo();
        BeanUtils.copyProperties(fromUser, fromWebSocketVo);
        BeanUtils.copyProperties(toUser, toWebSocketVo);
        chatVo.setFromUser(fromWebSocketVo);
        chatVo.setToUser(toWebSocketVo);
        chatVo.setChatType(chatType);
        chatVo.setText(text);
        chatVo.setCreateTime(DateUtil.format(createTime, "yyyy年MM月dd日 HH:mm:ss"));
        return chatVo;
    }

    /**
     * Vo映射
     */
    public ChatVo getChatVo(Long userId, String text) {
        ChatVo chatVo = new ChatVo();
        User fromUser = userService.getById(userId);
        WebSocketVo fromWebSocketVo = new WebSocketVo();
        BeanUtils.copyProperties(fromUser, fromWebSocketVo);
        chatVo.setFromUser(fromWebSocketVo);
        chatVo.setText(text);
        return chatVo;
    }

    /**
     * 保存消息到缓存中。
     *
     * @param redisKey   缓存的key。
     * @param id         与消息关联的唯一标识，如会话ID。
     * @param chatVos 需要保存的消息列表。
     */
    @Override
    public void saveCache(String redisKey, String id, List<ChatVo> chatVos) {
        try {
            ValueOperations<String, List<ChatVo>> valueOperations = redisTemplate.opsForValue();

            // 基础过期时间（2分钟）
            long baseExpireTime = 2L;
            // 随机增加0到2分钟，避免集中过期
            long randomExtension = RandomUtil.randomLong(0, 120); // 转换为秒

            long expireTime = baseExpireTime * 60 + randomExtension; // 总过期时间，单位转换为秒

            if (redisKey.equals(CHAT_HALL)) {
                // todo:对于公共聊天室，可以考虑更短的基础过期时间以加快信息流动
                valueOperations.set(redisKey, chatVos, 10, TimeUnit.SECONDS);
            } else {
                // todo:私人聊天可以根据实际情况调整过期时间策略
                valueOperations.set(redisKey + id, chatVos, expireTime, TimeUnit.SECONDS);
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
    public List<ChatVo> getCache(String redisKey, String id) {
        ValueOperations<String, List<ChatVo>> valueOperations = redisTemplate.opsForValue();
        List<ChatVo> chatRecords;
        if (redisKey.equals(CHAT_HALL)) {
            chatRecords = valueOperations.get(redisKey);
        } else {
            chatRecords = valueOperations.get(redisKey + id);
        }
        return chatRecords;
    }

    /**
     * 根据指定的key和id删除缓存中的消息数据。
     *
     * @param key 缓存的key。
     * @param id  与消息关联的唯一标识，如会话ID。
     */
    @Override
    public void deleteKey(String key, String id) {
        if (key.equals(CHAT_HALL)) {
            redisTemplate.delete(key);
        } else {
            redisTemplate.delete(key + id);
        }
    }

}




