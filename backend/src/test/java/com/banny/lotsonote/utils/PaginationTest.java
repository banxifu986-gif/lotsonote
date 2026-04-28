package com.banny.lotsonote.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PaginationUtils 单元测试
 * 覆盖正常场景、边界条件和异常场景
 */
public class PaginationTest {

    // -------- 正常场景 --------

    @Test
    public void testFirstPage() {
        // 第1页，每页10条 => offset = 0
        assertEquals(0, PaginationUtils.calculateOffset(1, 10));
    }

    @Test
    public void testSecondPage() {
        // 第2页，每页10条 => offset = 10
        assertEquals(10, PaginationUtils.calculateOffset(2, 10));
    }

    @Test
    public void testThirdPage() {
        // 第3页，每页5条 => offset = 10
        assertEquals(10, PaginationUtils.calculateOffset(3, 5));
    }

    @Test
    public void testLargePageNumber() {
        // 第100页，每页20条 => offset = 1980
        assertEquals(1980, PaginationUtils.calculateOffset(100, 20));
    }

    // -------- 边界条件 --------

    @Test
    public void testPageSizeOne() {
        // 每页1条，第5页 => offset = 4
        assertEquals(4, PaginationUtils.calculateOffset(5, 1));
    }

    @Test
    public void testPageOnePageSizeOne() {
        // 最小合法输入 => offset = 0
        assertEquals(0, PaginationUtils.calculateOffset(1, 1));
    }

    @Test
    public void testLargePageSize() {
        // 每页1000条，第2页 => offset = 1000
        assertEquals(1000, PaginationUtils.calculateOffset(2, 1000));
    }

    // -------- 异常场景 --------

    @Test
    public void testPageZeroThrows() {
        // page = 0 应抛出 IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                () -> PaginationUtils.calculateOffset(0, 10));
    }

    @Test
    public void testNegativePageThrows() {
        // page < 0 应抛出 IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                () -> PaginationUtils.calculateOffset(-1, 10));
    }

    @Test
    public void testPageSizeZeroThrows() {
        // pageSize = 0 应抛出 IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                () -> PaginationUtils.calculateOffset(1, 0));
    }

    @Test
    public void testNegativePageSizeThrows() {
        // pageSize < 0 应抛出 IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                () -> PaginationUtils.calculateOffset(1, -5));
    }

    @Test
    public void testBothInvalidThrows() {
        // page 和 pageSize 都非法，应抛出 IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                () -> PaginationUtils.calculateOffset(0, 0));
    }
}
