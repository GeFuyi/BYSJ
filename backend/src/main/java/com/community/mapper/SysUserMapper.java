package com.community.mapper;

import com.community.entity.SysUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysUserMapper {

    int insert(SysUser user);

    int update(SysUser user);

    int deleteById(@Param("id") Long id);

    SysUser selectById(@Param("id") Long id);

    SysUser selectByUsername(@Param("username") String username);

    SysUser selectByPhone(@Param("phone") String phone);

    List<SysUser> selectAll();
}
