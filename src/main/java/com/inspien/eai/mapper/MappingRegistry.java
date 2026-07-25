package com.inspien.eai.mapper;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MappingRegistry {

    private final Map<String, Mapper> registry = new ConcurrentHashMap<>();

    public MappingRegistry(OrderXmlToRowMapper orderXmlToRowMapper,
                            OrderToShipmentMapper orderToShipmentMapper) {
        register("IF-ORD-001", orderXmlToRowMapper);
        register("IF-SHP-001", orderToShipmentMapper);
    }

    public void register(String interfaceId, Mapper mapper) {
        registry.put(interfaceId, mapper);
    }

    public Mapper get(String interfaceId) {
        Mapper mapper = registry.get(interfaceId);
        if (mapper == null) {
            throw new IllegalStateException("등록되지 않은 인터페이스 ID: " + interfaceId);
        }
        return mapper;
    }
}
