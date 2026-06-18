package org.gulla.service.gulla.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "easyapply")
public class EasyApplyConfigProperties {

    private double matchThreshold = 0.70;
    private int automationTimeoutMs = 15000;
    private int maxRetryAttempts = 3;
    private String candidateDataRoot = "candidate-data";
    private ApiConfig api = new ApiConfig();

    public double getMatchThreshold() {
        return matchThreshold;
    }

    public void setMatchThreshold(double matchThreshold) {
        this.matchThreshold = matchThreshold;
    }

    public int getAutomationTimeoutMs() {
        return automationTimeoutMs;
    }

    public void setAutomationTimeoutMs(int automationTimeoutMs) {
        this.automationTimeoutMs = automationTimeoutMs;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public String getCandidateDataRoot() {
        return candidateDataRoot;
    }

    public void setCandidateDataRoot(String candidateDataRoot) {
        this.candidateDataRoot = candidateDataRoot;
    }

    public ApiConfig getApi() {
        return api;
    }

    public void setApi(ApiConfig api) {
        this.api = api;
    }

    public static class ApiConfig {
        private String key;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }
}
