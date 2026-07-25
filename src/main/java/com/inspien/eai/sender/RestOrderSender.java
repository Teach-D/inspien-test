package com.inspien.eai.sender;

import com.inspien.eai.engine.EngineResult;
import com.inspien.eai.engine.OrderRealtimeEngine;
import com.inspien.eai.message.Message;
import com.inspien.eai.message.MessageHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RestOrderSender {

    private final OrderRealtimeEngine orderRealtimeEngine;

    public RestOrderSender(OrderRealtimeEngine orderRealtimeEngine) {
        this.orderRealtimeEngine = orderRealtimeEngine;
    }

    @PostMapping(value = "/orders", consumes = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<Map<String, Object>> receiveOrder(
            @RequestBody String orderXml,
            @RequestHeader("X-Applicant-Key") String applicantKey) {

        MessageHeaders headers = MessageHeaders.of("IF-ORD-001", applicantKey);
        Message message = new Message(headers, orderXml);

        EngineResult result = orderRealtimeEngine.process(message);

        if (result.success()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "correlationId", result.correlationId(),
                    "message", "OK"
            ));
        }
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "correlationId", result.correlationId(),
                "message", result.message()
        ));
    }
}
