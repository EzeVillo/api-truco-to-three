package com.villo.truco.application.dto;

public record CurrentHandDTO(CardDTO cardPlayerOne,  // null si playerOne todavía no jugó
                             CardDTO cardPlayerTwo,  // null si playerTwo todavía no jugó
                             String mano             // quién tiene el rol de mano esta ronda
) {

}
