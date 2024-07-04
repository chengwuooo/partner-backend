package com.fcw.partner.constant;

/**
 * @author Chengwu Fang
 * date 2024/7/4
 */
public interface MessageConstant {
    /**
     * 私聊
     */
    int PRIVATE_MESSAGE = 1;
    /**
     * 队伍群聊
     */
    int TEAM_MESSAGE = 2;
    /**
     * 大厅聊天
     */
    int HALL_MESSAGE = 3;

    String MESSAGE_PRIVATE = "partner:message:message_private:";

    String MESSAGE_HALL = "partner:message:message_hall";

    String MESSAGE_TEAM = "partner:message:message_team:";

}
