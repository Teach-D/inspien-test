package com.inspien.eai.mapper;

import com.inspien.eai.message.Message;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrderXmlToRowMapper implements Mapper {

    @Override
    public Message map(Message source) {
        String rawXml = (String) source.getPayload();
        String applicantKey = source.getHeaders().applicantKey();

        try {
            String wrapped = "<ROOT>" + rawXml + "</ROOT>";
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(wrapped)));

            Map<String, HeaderInfo> headerByUserId = new HashMap<>();
            NodeList headerNodes = doc.getElementsByTagName("HEADER");
            for (int i = 0; i < headerNodes.getLength(); i++) {
                Element h = (Element) headerNodes.item(i);
                String userId = text(h, "USER_ID");
                headerByUserId.put(userId, new HeaderInfo(text(h, "NAME"), text(h, "ADDRESS")));
            }

            NodeList itemNodes = doc.getElementsByTagName("ITEM");
            List<OrderRow> rows = new ArrayList<>();
            int skippedOrphanItems = 0;

            for (int i = 0; i < itemNodes.getLength(); i++) {
                Element item = (Element) itemNodes.item(i);
                String userId = text(item, "USER_ID");
                HeaderInfo header = headerByUserId.get(userId);

                if (header == null) {
                    skippedOrphanItems++;
                    continue;
                }

                rows.add(new OrderRow(
                        "", applicantKey, userId,
                        text(item, "ITEM_ID"), header.name(), header.address(),
                        text(item, "ITEM_NAME"), text(item, "PRICE"), "N"));
            }

            if (skippedOrphanItems > 0) {
                org.slf4j.LoggerFactory.getLogger(OrderXmlToRowMapper.class)
                        .warn("[{}] 매칭되는 HEADER가 없어 스킵된 ITEM {}건",
                                source.getHeaders().correlationId(), skippedOrphanItems);
            }

            return source.withPayload(rows);
        } catch (Exception e) {
            throw new IllegalArgumentException("주문 XML 파싱 실패: " + e.getMessage(), e);
        }
    }

    private record HeaderInfo(String name, String address) {
    }

    private String text(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0 || nl.item(0).getTextContent() == null) {
            return "";
        }
        return nl.item(0).getTextContent().trim();
    }
}
