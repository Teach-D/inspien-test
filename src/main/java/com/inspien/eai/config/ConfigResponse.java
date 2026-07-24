package com.inspien.eai.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record ConfigResponse(
        @JsonProperty("APPLICANT_KEY") String applicantKey,
        @JsonProperty("ORDER_TB_CONN") Map<String, String> orderTbConn,
        @JsonProperty("SHIPMENT_TB_CONN") Map<String, String> shipmentTbConn,
        @JsonProperty("FTP_CONN") Map<String, String> ftpConn,
        @JsonProperty("SAMPLE_DATA") String sampleData
) {
}
