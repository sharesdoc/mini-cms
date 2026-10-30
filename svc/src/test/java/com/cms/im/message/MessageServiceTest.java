package com.cms.im.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 单聊会话 key 单元测试（N-U06）：两 ID 排序拼接，与参数顺序无关，保证同一对话 key 稳定。
 */
class MessageServiceTest {

    @Test
    void dmKey_与参数顺序无关() {
        String k1 = MessageService.dmKey("11111111111", "22222222222");
        String k2 = MessageService.dmKey("22222222222", "11111111111");
        assertEquals(k1, k2, "同一对话的两个方向 key 必须一致");
        assertEquals("11111111111_22222222222", k1);
    }

    @Test
    void dmKey_字典序拼接() {
        assertEquals("12345678901_98765432109", MessageService.dmKey("98765432109", "12345678901"));
    }
}
