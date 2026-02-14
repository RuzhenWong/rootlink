package com.rootlink.backend.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user_relation")
public class UserRelation implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Long relatedUserId;
    private Integer relationType;
    private String relationDesc;
    private String relationChain;  // JSON数组，如 ["父","母"]
    private Long inferredFrom;     // 由哪条关系推断（user_relation.id）
    private Integer inferStatus;   // 0-手动 1-推断待确认 2-推断已确认
    private Integer confirmStatus;
    private LocalDateTime confirmTime;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    @TableLogic private Integer deleted;
}
