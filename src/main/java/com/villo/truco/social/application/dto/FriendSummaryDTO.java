package com.villo.truco.social.application.dto;

public record FriendSummaryDTO(String friendUsername, boolean online,
                               FriendAvailabilityStatus availability, FriendBusyReason busyReason,
                               SpectatableMatchRefDTO spectatableMatch) {

}
