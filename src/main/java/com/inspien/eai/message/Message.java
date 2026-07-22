package com.inspien.eai.message;

public class Message {

    private final MessageHeaders headers;
    private Object payload;

    public Message(MessageHeaders headers, Object payload) {
        this.headers = headers;
        this.payload = payload;
    }

    public MessageHeaders getHeaders() {
        return headers;
    }

    public Object getPayload() {
        return payload;
    }

    public Message withPayload(Object newPayload) {
        return new Message(this.headers, newPayload);
    }
}
