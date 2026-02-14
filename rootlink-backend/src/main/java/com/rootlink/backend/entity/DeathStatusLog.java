package com.rootlink.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("death_status_log")
public class DeathStatusLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer oldStatus;
    private Integer newStatus;
    private String changeReason;
    private Long operatorId;
    private Integer markCount;
    private Integer proofCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
