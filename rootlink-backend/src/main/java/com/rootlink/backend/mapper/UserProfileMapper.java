package com.rootlink.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rootlink.backend.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户详细信息Mapper
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
