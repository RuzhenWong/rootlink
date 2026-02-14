package com.rootlink.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("death_proof")
public class DeathProof implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long targetUserId;
    private Long uploaderUserId;
    private Integer proofType;
    private String proofUrl;
    private String description;
    private Integer auditStatus;
    private Long auditorId;
    private LocalDateTime auditTime;
    private String auditReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
