package com.fcw.partner.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fcw.partner.model.domain.Chat;
import com.fcw.partner.model.domain.User;
import com.fcw.partner.model.request.ChatRequest;
import com.fcw.partner.model.vo.ChatVo;

import java.util.Date;
import java.util.List;

/**
* @author chengwu
* @description 针对表【chat(聊天消息表)】的数据库操作Service
* @createDate 2024-07-04 21:13:20
*/
public interface ChatService extends IService<Chat> {

    /**
     * 保存消息到缓存中。
     *
     * @param redisKey 缓存的key。
     * @param id 与消息关联的唯一标识，如会话ID。
     * @param chatVos 需要保存的消息列表。
     */
    void saveCache(String redisKey, String id, List<ChatVo> chatVos);

    /**
     * 从缓存中获取指定key和id对应的消息列表。
     *
     * @param redisKey 缓存的key。
     * @param id 与消息关联的唯一标识，如会话ID。
     * @return 消息列表。
     */
    List<ChatVo> getCache(String redisKey, String id);

    /**
     * 获取私聊消息记录。
     *
     * @param chatRequest 包含聊天请求详情的对象。
     * @param chatType 聊天类型，用于区分不同的聊天场景。
     * @param loginUser 当前登录用户的信息。
     * @return 私聊消息列表。
     */
    List<ChatVo> getPrivateChat(ChatRequest chatRequest, int chatType, User loginUser);

    /**
     * 获取大厅聊天消息记录。
     *
     * @param chatType 聊天类型，用于区分不同的聊天场景。
     * @param loginUser 当前登录用户的信息。
     * @return 大厅聊天消息列表。
     */
    List<ChatVo> getHallChat(int chatType, User loginUser);

    /**
     * 根据消息内容创建聊天结果对象。
     *
     * @param fromId 发送者ID。
     * @param toId 接收者ID。
     * @param text 消息文本内容。
     * @param chatType 聊天类型。
     * @param createTime 消息创建时间。
     * @return 聊天结果的VO对象。
     */
    ChatVo getChatVo(Long fromId, Long toId, String text, Integer chatType, Date createTime);

    /**
     * 获取团队聊天消息记录。
     *
     * @param chatRequest 包含聊天请求详情的对象。
     * @param chatType 聊天类型，用于区分不同的聊天场景。
     * @param loginUser 当前登录用户的信息。
     * @return 团队聊天消息列表。
     */
    List<ChatVo> getTeamChat(ChatRequest chatRequest, int chatType, User loginUser);

    /**
     * 根据指定的key和id删除缓存中的消息数据。
     *
     * @param key 缓存的key。
     * @param id 与消息关联的唯一标识，如会话ID。
     */
    void deleteKey(String key, String id);
}

