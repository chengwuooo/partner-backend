package com.fcw.partner.ws;


import com.fcw.partner.common.ErrorCode;
import com.fcw.partner.exception.BusinessException;
import com.fcw.partner.model.domain.User;
import com.fcw.partner.model.request.MessageRequest;
import com.fcw.partner.model.vo.MessageVo;
import com.fcw.partner.model.vo.WebSocketVo;
import com.fcw.partner.service.UserService;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Chengwu Fang
 * date 2021-07-03
 * WebSocket服务端类，负责处理客户端的连接和消息收发。
 * &#064;@Slf4j  SLF4J日志门面，用于日志记录。
 * &#064;ServerEndpoint  注解标识这个类是一个WebSocket的端点，/webSocket/{userId}指定了连接的路径。
 * &#064;Component  注解将这个类纳入到Spring的管理中，使其成为一个Bean。
 */
@Slf4j
@ServerEndpoint("/webSocket/{userId}")
@Component
public class WebSocketServer {
    private static UserService userService;

    @Resource
    public void setUserService(UserService userService) {
        WebSocketServer.userService = userService;
    }

    /**
     * 使用ConcurrentHashMap来存储用户的WebSocket会话，保证线程安全。
     */
    private static ConcurrentHashMap<String, Session> sessionPools = new ConcurrentHashMap<>();

    /**
     * 当客户端连接打开时的处理方法。
     *
     * @param session WebSocket的会话对象，用于和客户端进行通信。
     * @param userId  用户账户，作为会话的路径参数。
     */
    @OnOpen
    public synchronized void onOpen(Session session, @PathParam(value = "userId") String userId) {
        if (StringUtils.isBlank(userId) || "undefined".equals(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数有误");
        }
        sessionPools.put(userId, session);
        System.err.println(sessionPools);
        log.info("有新用户加入，userId={}, 当前在线人数为：{}", userId, sessionPools.size());
        sendAllUsers();
    }

    @OnClose
    public void onClose(@PathParam("userId") String userId) {
        try {
            if (!sessionPools.isEmpty()) {
                sessionPools.remove(userId);
                log.info("【WebSocket消息】连接断开{}", userId);
            }
            log.info("【WebSocket消息】连接断开，总数为：{}", sessionPools.size());
            sendAllUsers();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnMessage
    public void onMessage(String message, @PathParam("userId") String userId) {
        if ("PING".equals(message)) {
            sendAllMessage("pong");
            return;
        }
        log.info("服务端收到用户username={}的消息:{}", userId, message);
        MessageRequest messageRequest = new Gson().fromJson(message, MessageRequest.class);
        Long toId = messageRequest.getToId();
        String text = messageRequest.getText();

        Session toSession = sessionPools.get(toId.toString());
        if (toSession != null) {
            MessageVo messageVo = new MessageVo();
            User fromUser = userService.getById(userId);
            User toUser = userService.getById(toId);
            WebSocketVo fromWebSocketVo = new WebSocketVo();
            WebSocketVo toWebSocketVo = new WebSocketVo();
            BeanUtils.copyProperties(fromUser, fromWebSocketVo);
            BeanUtils.copyProperties(toUser, toWebSocketVo);
            messageVo.setFromUser(fromWebSocketVo);
            messageVo.setConsumer(toWebSocketVo);
            messageVo.setText(text);
            String toJson = new Gson().toJson(messageVo);
            sendOneMessage(toId.toString(), toJson);
            log.info("发送给用户username={}，消息：{}", messageVo.getConsumer(), toJson);
        } else {
            log.info("发送失败，未找到用户username={}的session", toId);
        }
    }

    /**
     * 此为广播消息
     *
     * @param message 消息
     */
    public void sendAllMessage(String message) {
        log.info("【WebSocket消息】广播消息：" + message);

        for (Session session : sessionPools.values()) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
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
        Session session = sessionPools.get(userId);
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
        HashMap<String, List<WebSocketVo>> stringListHashMap = new HashMap<>();
        List<WebSocketVo> webSocketVos = new ArrayList<>();
        stringListHashMap.put("users", webSocketVos);
        for (Serializable key : sessionPools.keySet()) {
            User user = userService.getById(key);
            WebSocketVo webSocketVo = new WebSocketVo();
            BeanUtils.copyProperties(user, webSocketVo);
            webSocketVos.add(webSocketVo);
        }
        sendAllMessage(new Gson().toJson(stringListHashMap));
    }
}

