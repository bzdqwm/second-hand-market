package com.yjx.market.common.controller;

import com.yjx.market.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 骨架连通性测试接口。
 * 用途：验证「前端 -> Java -> 数据库」链路是否打通。
 * 注意：Step 3 引入统一返回体后，这里要改成 Result 包装。
 */
@RestController
@RequestMapping("/api")
public class PingController {

    private static final Logger log = LoggerFactory.getLogger(PingController.class);

    private final RestClient pythonRestClient;

    public PingController(RestClient pythonRestClient) {
        this.pythonRestClient = pythonRestClient;
    }

    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {          // 原来是 Map<String, Object>
        log.info("收到请求: GET /api/ping");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "ok");
        data.put("app", "market");
        data.put("time", LocalDateTime.now());
        return Result.ok(data);                          // 原来是 return data
    }

    /**
     * 验证 Java -> Python 链路：Java 发 HTTP 请求，把 Python 返回的 JSON 原样带回来。
     */
    @GetMapping("/ping-ai")
    public Result<Map<String, Object>> pingAi() {
        log.info("收到请求: GET /api/ping-ai，准备调用 Python");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "ok");

        Map<String, Object> pythonResp = pythonRestClient.get()
                .uri("/ai/ping")
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        data.put("python", pythonResp);
        return Result.ok(data);
    }
}