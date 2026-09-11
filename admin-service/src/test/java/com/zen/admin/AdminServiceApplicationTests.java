package com.zen.admin;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("integration") // 依赖本机 MySQL/Nacos/Redis：默认从 test/check 排除，容器就绪时用 -PintegrationTests 跑
class AdminServiceApplicationTests {

    @Test
    void contextLoads() {}
}
