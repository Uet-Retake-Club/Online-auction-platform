package com.auction.shared.dto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Test cho lớp Response DTO.
 */
public class ResponseTest {

    @Test
    void testSuccessResponse() {
        Response response = new Response(MessageType.BID_SUCCESS, "SUCCESS", "Đặt giá thành công!", "{\"bid\":1500}");
        assertEquals(MessageType.BID_SUCCESS, response.getType());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("Đặt giá thành công!", response.getMessage());
        assertEquals("{\"bid\":1500}", response.getPayload());
    }

    @Test
    void testFailResponse() {
        Response response = new Response(MessageType.BID_ERROR, "FAIL", "Giá quá thấp", null);
        assertEquals("FAIL", response.getStatus());
        assertNull(response.getPayload());
    }

    @Test
    void testLoginResponse() {
        Response response = new Response(MessageType.LOGIN, "SUCCESS", "Đăng nhập thành công", null);
        assertEquals(MessageType.LOGIN, response.getType());
        assertEquals("SUCCESS", response.getStatus());
    }
}
