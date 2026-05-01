package com.example.webrtctest.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

/**
 * 海康摄像头实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("hikvision_camera")
public class HikvisionCamera {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 摄像头名称
     */
    @TableField
    private String name;
    
    /**
     * 摄像头编码（唯一标识）
     */
    @TableField
    private String code;
    
    /**
     * IP地址
     */
    @TableField
    private String ipAddress;
    
    /**
     * RTSP端口，默认554
     */
    @TableField
    private Integer rtspPort = 554;
    
    /**
     * 用户名
     */
    @TableField
    private String username;
    
    /**
     * 密码
     */
    @TableField
    private String password;
    
    /**
     * 通道号
     */
    @TableField
    private Integer channel = 1;
    
    /**
     * 码流类型：1-主码流，2-子码流
     */
    @TableField
    private Integer streamType = 1;
    
    /**
     * 位置信息
     */
    @TableField
    private String location;
    
    /**
     * 状态：0-离线，1-在线
     */
    @TableField
    private Integer status = 1;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
