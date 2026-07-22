package com.inspien.eai.mapper;

public record OrderRow(
        String orderId,
        String applicantKey,
        String userId,
        String itemId,
        String name,
        String address,
        String itemName,
        String price,
        String status
) {
}
