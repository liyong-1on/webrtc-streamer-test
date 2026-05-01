package com.example.webrtctest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.webrtctest.entity.HikvisionCamera;
import org.apache.ibatis.annotations.Mapper;

/**
 * 摄像头数据访问层
 */
@Mapper
public interface CameraMapper extends BaseMapper<HikvisionCamera> {
    // BaseMapper 已经提供了常用的 CRUD 方法：
    // - insert(T entity)
    // - deleteById(Serializable id)
    // - updateById(T entity)
    // - selectById(Serializable id)
    // - selectList(Wrapper<T> queryWrapper)
    // - selectOne(Wrapper<T> queryWrapper)
    // 等等...
}
