package com.villo.truco.social.application.dto;

public record ResourceInvitationDTO(String invitationId, String senderUsername,
                                    String recipientUsername, String targetType, String targetId,
                                    String status, long expiresAt) {

}
