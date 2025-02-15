package com.sunny.maven.microservice.user.service.impl;

import com.sunny.maven.microservice.bean.User;
import com.sunny.maven.microservice.user.mapper.UserMapper;
import com.sunny.maven.microservice.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author SUNNY
 * @description: 用户业务实现类
 * @create: 2023-03-23 11:04
 */
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }
}
