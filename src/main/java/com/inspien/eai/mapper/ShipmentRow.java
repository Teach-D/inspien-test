package com.inspien.eai.mapper;

public record ShipmentRow(
        String shipmentId,
        String applicantKey,
        String orderId,
        String itemId,
        String address
) {
}
