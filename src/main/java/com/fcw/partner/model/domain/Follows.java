package com.fcw.partner.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 关注表
 * @TableName follows
 */
@TableName(value ="follows")
@Data
public class Follows implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关注者用户ID
     */
    private Long user_id;

    /**
     * 被关注者用户ID
     */
    private Long followed_id;

    /**
     * 关注时间
     */
    private Date created_at;

    /**
     * 关注状态,0表示取消关注,1表示正在关注
     */
    private Integer is_active;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}