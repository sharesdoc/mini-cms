package com.cms.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 级别硬上限单元测试（N-U01）。
 * 验证 CmsProperties.effectiveMaxLevel() 对配置值做 [1, HARD_MAX=3] clamp，
 * 任何 >3 的配置（含部署配置文件误配 4）都不可突破代码硬上限（REQ-LVL-01）。
 */
class CmsPropertiesTest {

    private CmsProperties props;

    @BeforeEach
    void setUp() {
        props = new CmsProperties();
    }

    @Test
    void clamp_配置4_被限制为3() {
        props.getSystem().setMaxLevel(4);
        assertEquals(3, props.effectiveMaxLevel(), "配置 4 必须被 clamp 到硬上限 3");
    }

    @Test
    void clamp_配置5_被限制为3() {
        props.getSystem().setMaxLevel(5);
        assertEquals(3, props.effectiveMaxLevel());
    }

    @Test
    void clamp_配置0_被限制为1() {
        props.getSystem().setMaxLevel(0);
        assertEquals(1, props.effectiveMaxLevel(), "下限为 1");
    }

    @Test
    void clamp_配置负数_被限制为1() {
        props.getSystem().setMaxLevel(-2);
        assertEquals(1, props.effectiveMaxLevel());
    }

    @Test
    void 正常配置2_保持2() {
        props.getSystem().setMaxLevel(2);
        assertEquals(2, props.effectiveMaxLevel());
    }

    @Test
    void 正常配置3_保持3() {
        props.getSystem().setMaxLevel(3);
        assertEquals(3, props.effectiveMaxLevel());
    }

    @Test
    void admin账号默认不包含口令() {
        // 管理员口令必须由运行环境注入，源码不提供默认口令。
        assertEquals("admin", props.getAdmin().getName());
        assertEquals("", props.getAdmin().getPassword());
    }

    @Test
    void HARD_MAX_常量为3() {
        assertEquals(3, CmsProperties.HARD_MAX_LEVEL);
    }
}
