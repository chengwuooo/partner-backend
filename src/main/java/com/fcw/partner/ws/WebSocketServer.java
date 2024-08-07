package com.fcw.partner.ws;


import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fcw.partner.config.HttpSessionConfig;
import com.fcw.partner.model.domain.Message;
import com.fcw.partner.model.domain.Team;
import com.fcw.partner.model.domain.User;
import com.fcw.partner.model.request.ChatRequest;
import com.fcw.partner.model.vo.MessageVo;
import com.fcw.partner.model.vo.WebSocketVo;
import com.fcw.partner.service.ChatService;
import com.fcw.partner.service.TeamService;
import com.fcw.partner.service.UserService;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.fcw.partner.constant.ChatConstant.*;
import static com.fcw.partner.constant.UserConstant.ADMIN_ROLE;
import static com.fcw.partner.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author Chengwu Fang
 * date 2021-07-03
 * WebSocketServer服务端类，负责处理客户端的连接和消息收发。
 */
@Slf4j
@ServerEndpoint(value = "/websocket/{userId}/{teamId}", configurator = HttpSessionConfig.class)
@Component
public class WebSocketServer {
    /**
     * 保存队伍的连接信息
     */
    private static final Map<String, ConcurrentHashMap<String, WebSocketServer>> ROOMS = new HashMap<>();
    /**
     * 线程安全的无序的集合
     */
    private static final CopyOnWriteArraySet<Session> SESSIONS = new CopyOnWriteArraySet<>();
    /**
     * 存储在线连接数
     */
    private static final Map<String, Session> SESSION_POOL = new HashMap<>(0);
    private static UserService userService;
    private static ChatService chatService;
    private static TeamService teamService;
    /**
     * 房间在线人数
     */
    private static int onlineCount = 0;
    /**
     * 当前信息
     */
    private Session session;
    private HttpSession httpSession;

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount--;
    }

    @Resource
    public void setHeatMapService(UserService userService) {
        WebSocketServer.userService = userService;
    }

    @Resource
    public void setHeatMapService(ChatService chatService) {
        WebSocketServer.chatService = chatService;
    }

    @Resource
    public void setHeatMapService(TeamService teamService) {
        WebSocketServer.teamService = teamService;
    }

    /**
     * 队伍内群发消息
     *
     * @param teamId 队伍标识符，用于定位到特定的队伍
     * @param msg    需要发送的消息内容
     * @throws Exception 如果发送过程中出现错误，可能会抛出异常
     *
     * 本方法通过使用 ConcurrentHashMap 来确保在并发环境下安全地对队伍内的所有连接进行消息广播
     * ConcurrentHashMap 的 key 是用户的唯一标识，value 是对应的 WebSocketServer 实例
     * 遍历 ConcurrentHashMap 的 keySet 来实现对所有用户的广播
     */
    public static void broadcast(String teamId, String msg) {
        ConcurrentHashMap<String, WebSocketServer> map = ROOMS.get(teamId);
        // 遍历所有用户，尝试发送消息
        for (String key : map.keySet()) {
            try {
                WebSocketServer webSocket = map.get(key);
                webSocket.sendMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 发送消息
     *
     * @param message
     * @throws IOException
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    /**
     * 当WebSocket连接打开时调用的方法
     *
     * @param session WebSocket会话对象
     * @param userId 用户ID，用于识别用户
     * @param teamId 团队ID，用于将用户分配到不同的团队房间
     * @param config Endpoint配置，包含会话的配置信息
     */
    @OnOpen
    public void onOpen(Session session, @PathParam(value = "userId") String userId, @PathParam(value = "teamId") String teamId, EndpointConfig config) {
        log.info("userId = {}", userId);
        try {
            // 检查userId是否有效
            if (StringUtils.isBlank(userId) || "undefined".equals(userId)) {
                sendError(userId, "参数有误");
                return;
            }
            // 获取HttpSession对象，用于获取用户信息
            HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
            // 从会话中获取用户信息
            User user = (User) httpSession.getAttribute(USER_LOGIN_STATE);
            // 如果用户已登录，则初始化WebSocket会话信息
            if (user != null) {
                this.session = session;
                this.httpSession = httpSession;
            }
            // 处理团队房间的加入逻辑
            if (!"0".equals(teamId)) {
                // 如果该团队房间不存在，则创建新房间
                if (!ROOMS.containsKey(teamId)) {
                    ConcurrentHashMap<String, WebSocketServer> room = new ConcurrentHashMap<>(0);
                    room.put(userId, this);
                    ROOMS.put(String.valueOf(teamId), room);
                    // 在线数加1
                    addOnlineCount();
                } else {
                    // 如果房间存在，但用户尚未加入，则将用户添加到房间
                    if (!ROOMS.get(teamId).containsKey(userId)) {
                        ROOMS.get(teamId).put(userId, this);
                        // 在线数加1
                        addOnlineCount();
                    }
                }
                log.info("有新连接加入！当前在线人数为" + getOnlineCount());
            } else {
                // 如果团队ID为0，将用户会话添加到全局会话列表
                SESSIONS.add(session);
                SESSION_POOL.put(userId, session);
                log.info("有新用户加入，userId={}, 当前在线人数为：{}", userId, SESSION_POOL.size());
                // 通知其他用户此新用户的加入
                sendAllUsers();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(@PathParam("userId") String userId, @PathParam(value = "teamId") String teamId, Session session) {
        try {
            if (!"0".equals(teamId)) {
                ROOMS.get(teamId).remove(userId);
                if (getOnlineCount() > 0) {
                    subOnlineCount();
                }
                log.info("用户退出:当前在线人数为:" + getOnlineCount());
            } else {
                if (!SESSION_POOL.isEmpty()) {
                    SESSION_POOL.remove(userId);
                    SESSIONS.remove(session);
                }
                log.info("【WebSocket消息】连接断开，总数为：" + SESSION_POOL.size());
                sendAllUsers();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnMessage
    public void onMessage(String message, @PathParam("userId") String userId,@PathParam(value = "teamId") String teamId) {
        if ("PING".equals(message)) {
            sendOneMessage(userId, "pong");
            log.error("心跳包，发送给={},在线:{}人", userId, getOnlineCount());
            return;
        }
        log.info("服务端收到用户username={}的消息:{}", userId, message);
        ChatRequest messageRequest = new Gson().fromJson(message, ChatRequest.class);
        Long toId = messageRequest.getToId();
        String text = messageRequest.getText();
        Integer chatType = messageRequest.getChatType();
        User fromUser = userService.getById(userId);
        if (chatType == PRIVATE_CHAT) {
            // 私聊
            privateChat(fromUser, toId, text, chatType);
        } else if (chatType == TEAM_CHAT) {
            // 队伍内聊天
            teamChat(fromUser, text, teamId, chatType);
        } else {
            // 群聊
            hallChat(fromUser, text, chatType);
        }
    }

    /**
     * 队伍聊天
     *
     */
    private void teamChat(User user, String text, String teamId, Integer chatType) {
        Team team = teamService.getById(teamId);
        MessageVo messageVo = new MessageVo();
        WebSocketVo fromWebSocketVo = new WebSocketVo();
        BeanUtils.copyProperties(user, fromWebSocketVo);
        messageVo.setFromUser(fromWebSocketVo);
        messageVo.setText(text);
        messageVo.setTeamId(team.getId());
        messageVo.setChatType(chatType);
        messageVo.setCreateTime(DateUtil.format(new Date(), "yyyy年MM月dd日 HH:mm:ss"));
        if (user.getId().equals(team.getUserId()) || user.getUserRole().equals(ADMIN_ROLE)) {
            messageVo.setIsAdmin(true);
        }
        User loginUser = (User) this.httpSession.getAttribute(USER_LOGIN_STATE);
        if (loginUser.getId() == user.getId()) {
            messageVo.setIsMy(true);
        }
        String toJson = new Gson().toJson(messageVo);
        try {
            broadcast(String.valueOf(team.getId()), toJson);
            savaChat(user.getId(), team.getId(), text, chatType);
            chatService.deleteKey(CHAT_TEAM, String.valueOf(team.getId()));
            log.error("队伍聊天，发送给={},队伍={},在线:{}人", user.getId(), team.getId(), getOnlineCount());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 大厅聊天
     *
     * @param user
     * @param text
     */
    private void hallChat(User user, String text, Integer chatType) {
        MessageVo messageVo = new MessageVo();
        WebSocketVo fromWebSocketVo = new WebSocketVo();
        BeanUtils.copyProperties(user, fromWebSocketVo);
        messageVo.setFromUser(fromWebSocketVo);
        messageVo.setText(text);
        messageVo.setChatType(chatType);
        messageVo.setCreateTime(DateUtil.format(new Date(), "yyyy年MM月dd日 HH:mm:ss"));
        if (user.getUserRole() == ADMIN_ROLE) {
            messageVo.setIsAdmin(true);
        }
        User loginUser = (User) this.httpSession.getAttribute(USER_LOGIN_STATE);
        if (loginUser.getId() == user.getId()) {
            messageVo.setIsMy(true);
        }
        String toJson = new Gson().toJson(messageVo);
        sendAllMessage(toJson);
        savaChat(user.getId(), null, text, chatType);
        chatService.deleteKey(CHAT_HALL, String.valueOf(user.getId()));
    }

    /**
     * 私人聊天
     *
     * @param user     使用者
     * @param toId     至id
     * @param text     文本
     * @param chatType 聊天类型
     */
    private void privateChat(User user, Long toId, String text, Integer chatType) {
        Session toSession = SESSION_POOL.get(toId.toString());
        if (toSession != null) {
            MessageVo messageVo = chatService.getChatVo(user.getId(), toId, text, chatType, DateUtil.date(System.currentTimeMillis()));
            User loginUser = (User) this.httpSession.getAttribute(USER_LOGIN_STATE);
            if (loginUser.getId() == user.getId()) {
                messageVo.setIsMy(true);
            }
            String toJson = new Gson().toJson(messageVo);
            sendOneMessage(toId.toString(), toJson);
            log.info("发送给用户username={}，消息：{}", messageVo.getToUser(), toJson);
        } else {
            log.info("用户不在线username={}的session", toId);
        }
        savaChat(user.getId(), toId, text, chatType);
        chatService.deleteKey(CHAT_PRIVATE, user.getId() + "" + toId);
        chatService.deleteKey(CHAT_PRIVATE, toId + "" + user.getId());
    }

    /**
     * 保存聊天
     *
     * @param userId   用户id
     * @param toId     至id
     * @param text     文本
     * @param chatType 聊天类型
     */
    private void savaChat(Long userId, Long toId, String text, Integer chatType) {
        Message message = new Message();
        message.setFromId(userId);
        message.setText(String.valueOf(text));
        message.setChatType(chatType);
        message.setDate(new Date());
        if (toId != null && toId > 0) {
            message.setToId(toId);
        }
        chatService.save(message);
    }

    /**
     * 发送失败
     *
     * @param userId       用户id
     * @param errorMessage 错误消息
     */
    private void sendError(String userId, String errorMessage) {
        JSONObject obj = new JSONObject();
        obj.set("error", errorMessage);
        sendOneMessage(userId, obj.toString());
    }

    /**
     * 此为广播消息
     *
     * @param message 消息
     */
    public void sendAllMessage(String message) {
        System.out.println("当前大厅在线人数"+SESSIONS.size());
        for (Session session : SESSIONS) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        log.info("【WebSocket消息】广播消息：" + message);
                        session.getBasicRemote().sendText(message);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 此为单点消息
     *
     * @param userId  用户编号
     * @param message 消息
     */
    public void sendOneMessage(String userId, String message) {
        Session session = SESSION_POOL.get(userId);
        if (session != null && session.isOpen()) {
            try {
                synchronized (session) {
                    log.info("【WebSocket消息】单点消息：" + message);
                    session.getAsyncRemote().sendText(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送所有在线用户信息
     */
    public void sendAllUsers() {
        log.info("【WebSocket消息】发送所有在线用户信息");
        HashMap<String, List<WebSocketVo>> stringListHashMap = new HashMap<>(0);
        List<WebSocketVo> webSocketVos = new ArrayList<>();
        stringListHashMap.put("users", webSocketVos);
        for (Serializable key : SESSION_POOL.keySet()) {
            User user = userService.getById(key);
            WebSocketVo webSocketVo = new WebSocketVo();
            BeanUtils.copyProperties(user, webSocketVo);
            webSocketVos.add(webSocketVo);
        }
        sendAllMessage(JSONUtil.toJsonStr(stringListHashMap));
    }
}