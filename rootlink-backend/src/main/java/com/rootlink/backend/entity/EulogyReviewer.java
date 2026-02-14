package com.rootlink.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("eulogy_reviewer")
public class EulogyReviewer implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long eulogyId;
    private Long reviewerUserId;
    private Integer relationType;
    private Integer reviewStatus;
    private LocalDateTime reviewTime;
    private String reviewOpinion;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
