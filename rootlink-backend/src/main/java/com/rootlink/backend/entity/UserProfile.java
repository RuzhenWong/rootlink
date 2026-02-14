package com.rootlink.backend.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user_profile")
public class UserProfile implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Integer gender;        // 0未知 1男 2女
    private String nation;
    private LocalDate birthDate;
    private LocalDate deathDate;
    private String nativePlace;    // 保留兼容旧数据
    private String province;       // 省份
    private String city;           // 城市
    private String district;       // 区县
    private String residence;
    private String wechat;
    private String qq;
    private String workUnit;
    private String position;
    private String education;
    private Integer maritalStatus; // 0未知 1未婚 2已婚 3离异 4丧偶
    private Integer familyRank;    // 家族排行，1=老大
    private String bio;
    private String avatarUrl;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
}
