package com.example.gembos;

public class Message {
    private String messageContent;
    private boolean isSent;

    public Message(String messageContent, boolean isSent) {
        this.messageContent = messageContent;
        this.isSent = isSent;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public boolean isSent() {
        return isSent;
    }
}