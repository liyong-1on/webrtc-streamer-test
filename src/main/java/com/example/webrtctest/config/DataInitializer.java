package com.example.webrtctest.config;

import com.example.webrtctest.entity.HikvisionCamera;
import com.example.webrtctest.mapper.CameraMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据初始化器
 * 在应用启动时自动创建测试数据
 */
@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private CameraMapper cameraMapper;

    @Override
    public void run(String... args) throws Exception {
        // MySQL模式下，数据已通过schema.sql初始化，跳过自动创建
        log.info("Using MySQL database, data initialized via schema.sql");
            
        // 如果需要检查数据，可以取消下面的注释
        /*
        Long count = cameraMapper.selectCount(null);
        if (count > 0) {
            log.info("Database already has {} cameras", count);
            return;
        }
    
        log.info("Initializing test camera data...");
    
        List<HikvisionCamera> cameras = new ArrayList<>();
    
        // 创庻64个测试摄像头（模拟一个监控室的情况）
        for (int i = 1; i <= 64; i++) {
            HikvisionCamera camera = new HikvisionCamera();
            camera.setName("测试摄像头-" + String.format("%02d", i));
            camera.setCode("CAM" + String.format("%03d", i));
            camera.setIpAddress("192.168.1." + (100 + i % 50));
            camera.setRtspPort(554);
            camera.setUsername("admin");
            camera.setPassword("password123");
            camera.setChannel(1);
            camera.setStreamType(1);
            camera.setLocation("测试区域-" + (i / 8 + 1));
            camera.setStatus(1); // 在线
    
            cameras.add(camera);
        }
    
        // 批量保存
        cameras.forEach(camera -> cameraMapper.insert(camera));
    
        log.info("Successfully initialized {} test cameras", cameras.size());
        */
    }
}
