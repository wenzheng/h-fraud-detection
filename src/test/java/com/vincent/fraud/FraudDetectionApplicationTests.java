package com.vincent.fraud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class FraudDetectionApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldAcceptTransactionAndEventuallyFlagAlert() throws Exception {
        String payload = """
                {
                  "accountId": "acct-blacklist-001",
                  "merchantId": "merchant-123",
                  "deviceId": "device-007",
                  "ipAddress": "192.168.1.25",
                  "currency": "USD",
                  "amount": 250.00
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        String transactionId = jsonNode.get("id").asText();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            mockMvc.perform(get("/api/v1/transactions/{id}", transactionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FLAGGED"));
        });

        String alertsJson = mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(alertsJson).contains(transactionId);
        assertThat(alertsJson).contains("suspicious watchlist");
    }
}
