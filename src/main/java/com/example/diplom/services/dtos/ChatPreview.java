package com.example.diplom.services.dtos;

public class ChatPreview {
    private String conversationId;
    private String partnerId;
    private String partnerName;
    private String lastMessage;
    private String timestamp;

    public ChatPreview() {}

    public ChatPreview(String conversationId, String partnerId, String partnerName, String lastMessage, String timestamp) {
        this.conversationId = conversationId;
        this.partnerId = partnerId;
        this.partnerName = partnerName;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    // геттеры/сеттеры
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
