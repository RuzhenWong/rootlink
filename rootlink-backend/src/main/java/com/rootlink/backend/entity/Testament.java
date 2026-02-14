package com.rootlink.backend.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("testament")
public class Testament implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private String title;
    private String contentEncrypted;
    private Integer testamentType;   // 1-文字 2-给特定人 3-财产分配 4-其他
    private Integer unlockStatus;    // 0-锁定 1-已解锁
    private Integer visibility;      // 0-仅自己 1-指定人 2-直系两代 3-全公开
    private String visibilityNote;
    private LocalDateTime unlockTime;
    private String attachmentUrls;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    @TableLogic private Integer deleted;
}
