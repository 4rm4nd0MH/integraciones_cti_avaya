package com.pruebafullstack.cti.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cti")
public class CtiProperties {

    private boolean enabled = true;
    private String websocketUrl;
    private Duration reconnectInitialDelay = Duration.ofSeconds(2);
    private Duration reconnectMaxDelay = Duration.ofSeconds(30);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWebsocketUrl() {
        return websocketUrl;
    }

    public void setWebsocketUrl(String websocketUrl) {
        this.websocketUrl = websocketUrl;
    }

    public Duration getReconnectInitialDelay() {
        return reconnectInitialDelay;
    }

    public void setReconnectInitialDelay(Duration reconnectInitialDelay) {
        this.reconnectInitialDelay = reconnectInitialDelay;
    }

    public Duration getReconnectMaxDelay() {
        return reconnectMaxDelay;
    }

    public void setReconnectMaxDelay(Duration reconnectMaxDelay) {
        this.reconnectMaxDelay = reconnectMaxDelay;
    }
}
