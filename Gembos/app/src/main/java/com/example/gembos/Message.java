package com.example.gembos;

public class Message {
    private int id;
    private String sender;
    private String body;
    private String date;

    public Message(int id, String sender, String body, String date) {
        this.id = id;
        this.sender = sender;
        this.body = body;
        this.date = date;
    }

    public Message(String sender, String body, String date) {
        this.sender = sender;
        this.body = body;
        this.date = date;
    }

    public int getId() { return id; }
    public String getSender() { return sender; }
    public String getBody() { return body; }
    public String getDate() { return date; }

    public void setId(int id) { this.id = id; }
}

