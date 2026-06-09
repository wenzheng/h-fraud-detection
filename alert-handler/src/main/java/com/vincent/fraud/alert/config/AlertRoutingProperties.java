package com.vincent.fraud.alert.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.alert-routing")
public record AlertRoutingProperties(
        List<Channel> highActions,
        List<Channel> mediumActions,
        List<Channel> lowActions
) {
    public AlertRoutingProperties {
        highActions = highActions == null ? List.of(Channel.TELEGRAM, Channel.LOG) : List.copyOf(highActions);
        mediumActions = mediumActions == null ? List.of(Channel.EMAIL, Channel.LOG) : List.copyOf(mediumActions);
        lowActions = lowActions == null ? List.of(Channel.LOG) : List.copyOf(lowActions);
    }

    public enum Channel {
        TELEGRAM,
        EMAIL,
        LOG
    }
}
