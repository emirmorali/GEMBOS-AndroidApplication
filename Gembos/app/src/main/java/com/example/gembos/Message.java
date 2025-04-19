package com.example.gembos;

public class Message {
    private String sender;
    private String body;
    private String date;

    public Message(String sender, String body, String date) {
        this.sender = sender;
        this.body = body;
        this.date = date;
    }

    public String getSender() { return sender; }
    public String getBody() { return body; }
    public String getDate() { return date; }
}
