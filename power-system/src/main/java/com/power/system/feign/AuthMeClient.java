package com.power.system.feign;

import com.power.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "power-auth", url = "${power.feign.auth-url:}", path = "/auth")
public interface AuthMeClient {

    @GetMapping("/me")
    R<Map<String, Object>> me();
}
