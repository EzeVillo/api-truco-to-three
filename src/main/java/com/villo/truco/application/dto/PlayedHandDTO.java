package com.villo.truco.application.dto;

public record PlayedHandDTO(CardDTO cardPlayerOne, CardDTO cardPlayerTwo, String winner
                            // null si fue parda
) {

}
