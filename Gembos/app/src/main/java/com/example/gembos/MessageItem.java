package com.example.gembos;

public class MessageItem {
    private String messageContent;
    private boolean isSent;

    public MessageItem(String messageContent, boolean isSent) {
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