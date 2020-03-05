package com.liuty.maven.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description:
 * @author: Sunny
 * @date: 2019/12/28
 */
@RestController
@RequestMapping("/admin/api")
public class AdminController {
    @RequestMapping("hello")
    public String hello() {
        return "hello, admin";
    }
}
