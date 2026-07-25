package com.inspien.eai.mapper;

import com.inspien.eai.message.Message;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderToShipmentMapper implements Mapper {

    @Override
    @SuppressWarnings("unchecked")
    public Message map(Message source) {
        List<OrderRow> orders = (List<OrderRow>) source.getPayload();

        List<ShipmentRow> shipments = orders.stream()
                .map(o -> new ShipmentRow(
                        "",
                        o.applicantKey(),
                        o.orderId(),
                        o.itemId(),
                        o.address()))
                .toList();

        return source.withPayload(shipments);
    }
}
