package com.vincent.fraud.alert.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AlertRoutingPropertiesTest {

    @Test
    void shouldApplyDefaultActionsWhenValuesAreMissing() {
        AlertRoutingProperties properties = new AlertRoutingProperties(null, null, null);

        assertThat(properties.highActions()).containsExactly(
                AlertRoutingProperties.Channel.TELEGRAM,
                AlertRoutingProperties.Channel.LOG
        );
        assertThat(properties.mediumActions()).containsExactly(
                AlertRoutingProperties.Channel.EMAIL,
                AlertRoutingProperties.Channel.LOG
        );
        assertThat(properties.lowActions()).containsExactly(AlertRoutingProperties.Channel.LOG);
    }
}
