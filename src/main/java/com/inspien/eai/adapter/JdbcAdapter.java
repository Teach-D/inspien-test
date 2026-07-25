package com.inspien.eai.adapter;

import com.inspien.eai.mapper.OrderRow;
import com.inspien.eai.mapper.ShipmentRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JdbcAdapter {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<OrderRow> insertOrders(List<OrderRow> rows) {
        if (rows.isEmpty()) {
            return rows;
        }
        String applicantKey = rows.get(0).applicantKey();
        String sql = "INSERT INTO RECRUIT.ORDER_TB " +
                "(ORDER_ID, APPLICANT_KEY, USER_ID, ITEM_ID, NAME, ADDRESS, ITEM_NAME, PRICE, STATUS, CREATE_TIME) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)";

        String cursor = fetchCurrentMax("RECRUIT.ORDER_TB", "ORDER_ID", applicantKey);
        List<OrderRow> assigned = new ArrayList<>(rows.size());
        for (OrderRow r : rows) {
            cursor = nextLetterDigitId(cursor);
            OrderRow withId = new OrderRow(cursor, r.applicantKey(), r.userId(), r.itemId(),
                    r.name(), r.address(), r.itemName(), r.price(), r.status());
            jdbcTemplate.update(sql, withId.orderId(), withId.applicantKey(), withId.userId(), withId.itemId(),
                    withId.name(), withId.address(), withId.itemName(), withId.price(), withId.status());
            assigned.add(withId);
        }
        return assigned;
    }

    public List<OrderRow> findUnshippedOrders(String applicantKey, int limit) {
        String sql = "SELECT * FROM (" +
                "  SELECT ORDER_ID, APPLICANT_KEY, USER_ID, ITEM_ID, NAME, ADDRESS, ITEM_NAME, PRICE, STATUS " +
                "  FROM RECRUIT.ORDER_TB " +
                "  WHERE APPLICANT_KEY = ? AND STATUS = 'N'" +
                ") WHERE ROWNUM <= ?";
        return jdbcTemplate.query(sql, (rs, i) -> new OrderRow(
                rs.getString("ORDER_ID"), rs.getString("APPLICANT_KEY"), rs.getString("USER_ID"),
                rs.getString("ITEM_ID"), rs.getString("NAME"), rs.getString("ADDRESS"),
                rs.getString("ITEM_NAME"), rs.getString("PRICE"), rs.getString("STATUS")
        ), applicantKey, limit);
    }

    public void insertShipment(ShipmentRow row) {
        String shipmentId = nextLetterDigitId(
                fetchCurrentMax("RECRUIT.SHIPMENT_TB", "SHIPMENT_ID", row.applicantKey()));
        jdbcTemplate.update(
                "INSERT INTO RECRUIT.SHIPMENT_TB (SHIPMENT_ID, APPLICANT_KEY, ORDER_ID, ITEM_ID, ADDRESS, CREATE_DATE) " +
                        "VALUES (?, ?, ?, ?, ?, SYSTIMESTAMP)",
                shipmentId, row.applicantKey(), row.orderId(), row.itemId(), row.address());
    }

    public int markOrderShipped(String orderId, String applicantKey) {
        return jdbcTemplate.update(
                "UPDATE RECRUIT.ORDER_TB SET STATUS = 'Y' WHERE ORDER_ID = ? AND APPLICANT_KEY = ? AND STATUS = 'N'",
                orderId, applicantKey);
    }

    private String fetchCurrentMax(String table, String idColumn, String applicantKey) {
        List<String> result = jdbcTemplate.query(
                "SELECT * FROM (SELECT " + idColumn + " AS ID FROM " + table +
                        " WHERE APPLICANT_KEY = ? ORDER BY " + idColumn + " DESC) WHERE ROWNUM = 1",
                (rs, i) -> rs.getString("ID"), applicantKey);
        return result.isEmpty() ? null : result.get(0);
    }

    private String nextLetterDigitId(String current) {
        if (current == null) {
            return "A000";
        }
        char letter = current.charAt(0);
        int number = Integer.parseInt(current.substring(1)) + 1;
        if (number > 999) {
            number = 0;
            letter++;
            if (letter > 'Z') {
                throw new IllegalStateException("채번 가능 공간(A000~Z999)이 모두 소진되었습니다: " + table());
            }
        }
        return "" + letter + String.format("%03d", number);
    }

    private String table() {
        return "ORDER_TB/SHIPMENT_TB";
    }
}
