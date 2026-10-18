package com.dremioplatform.ai.service.dto;

public record AiFunctionResponse(String result, String model, Integer promptTokens, Integer completionTokens, long latencyMillis) {
}
