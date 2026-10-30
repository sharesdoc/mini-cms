package com.cms.system.mapper;

import com.cms.system.SysConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 系统配置 Mapper */
public interface SysConfigMapper {
    List<SysConfig> selectAll();
    SysConfig selectByKey(@Param("key") String key);
    int upsert(@Param("key") String key, @Param("value") String value);
}
