package com.rootlink.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("eulogy")
public class Eulogy implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long targetUserId;
    private Long submitterUserId;
    private String content;
    private LocalDateTime submitTime;
    private Integer reviewStatus;
    private LocalDateTime publishTime;
    private String rejectReason;
    private Long rejectorUserId;
    private LocalDateTime rejectTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
