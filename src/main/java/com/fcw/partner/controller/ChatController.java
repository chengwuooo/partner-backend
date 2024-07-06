package com.fcw.partner.controller;

import com.fcw.partner.common.BaseResponse;
import com.fcw.partner.common.ErrorCode;
import com.fcw.partner.common.ResultUtils;
import com.fcw.partner.exception.BusinessException;
import com.fcw.partner.model.domain.Chat;
import com.fcw.partner.model.domain.User;
import com.fcw.partner.model.request.ChatRequest;
import com.fcw.partner.model.vo.ChatVo;
import com.fcw.partner.service.ChatService;
import com.fcw.partner.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.fcw.partner.constant.ChatConstant.*;

/**
 * @author Chengwu Fang
 * date 2024/7/6
 */
@RestController
@RequestMapping("/chat")
public class ChatController {
    @Resource
    private ChatService chatService;
    @Resource
    private UserService userService;

    @PostMapping("/private")
    public BaseResponse<List<ChatVo>> getPrivateChat(@RequestBody ChatRequest chatRequest, HttpServletRequest request) {
        if (chatRequest == null || chatRequest.getChatType()!= PRIVATE_CHAT) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求有误");
        }
        User loginUser = userService.getLoginUser(request);
        List<ChatVo> privateChat = chatService.getPrivateChat(chatRequest, PRIVATE_CHAT, loginUser);
        return ResultUtils.success(privateChat);
    }

    @GetMapping("/hall")
    public BaseResponse<List<ChatVo>> getHallChat(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<ChatVo> hallChat = chatService.getHallChat(HALL_CHAT, loginUser);
        return ResultUtils.success(hallChat);
    }

    @PostMapping("/team")
    public BaseResponse<List<ChatVo>> getTeamChat(@RequestBody ChatRequest chatRequest, HttpServletRequest request) {
        if (chatRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求有误");
        }
        User loginUser = userService.getLoginUser(request);
        List<ChatVo> teamChat = chatService.getTeamChat(chatRequest, TEAM_CHAT, loginUser);
        return ResultUtils.success(teamChat);
    }

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    

}
