package com.villo.truco.infrastructure.http;

import com.villo.truco.application.commands.SendMessageCommand;
import com.villo.truco.application.ports.in.GetChatByParentUseCase;
import com.villo.truco.application.ports.in.GetChatMessagesUseCase;
import com.villo.truco.application.ports.in.SendMessageUseCase;
import com.villo.truco.application.queries.GetChatByParentQuery;
import com.villo.truco.application.queries.GetChatMessagesQuery;
import com.villo.truco.infrastructure.http.dto.request.SendMessageRequest;
import com.villo.truco.infrastructure.http.dto.response.ChatMessagesResponse;
import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats")
@Tag(name = "Chat", description = "Endpoints para el chat en tiempo real")
public class ChatController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);

    private final SendMessageUseCase sendMessage;
    private final GetChatMessagesUseCase getChatMessages;
    private final GetChatByParentUseCase getChatByParent;

    public ChatController(final SendMessageUseCase sendMessage,
        final GetChatMessagesUseCase getChatMessages,
        final GetChatByParentUseCase getChatByParent) {

        this.sendMessage = Objects.requireNonNull(sendMessage);
        this.getChatMessages = Objects.requireNonNull(getChatMessages);
        this.getChatByParent = Objects.requireNonNull(getChatByParent);
    }

    @PostMapping("/{chatId}/messages")
    @Operation(summary = "Enviar mensaje", description = "Envía un mensaje en el chat", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Mensaje enviado"),
        @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Chat no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "No se pudo enviar el mensaje", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public ResponseEntity<Void> sendMessage(
        @Parameter(description = "ID del chat") @PathVariable final String chatId,
        @Valid @RequestBody final SendMessageRequest request,
        @AuthenticationPrincipal final Jwt jwt) {

        LOGGER.info("HTTP sendMessage requested: chatId={}", chatId);
        this.sendMessage.handle(
            new SendMessageCommand(chatId, jwt.getSubject(), request.content()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{chatId}/messages")
    @Operation(summary = "Obtener mensajes", description = "Devuelve los mensajes del chat (máximo 50)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mensajes del chat", content = @Content(schema = @Schema(implementation = ChatMessagesResponse.class))),
        @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Chat no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public ResponseEntity<ChatMessagesResponse> getChatMessages(
        @Parameter(description = "ID del chat") @PathVariable final String chatId,
        @AuthenticationPrincipal final Jwt jwt) {

        final var dto = this.getChatMessages.handle(
            new GetChatMessagesQuery(chatId, jwt.getSubject()));
        return ResponseEntity.ok(ChatMessagesResponse.from(dto));
    }

    @GetMapping("/by-parent/{parentType}/{parentId}")
    @Operation(summary = "Buscar chat por recurso padre", description = "Busca el chat asociado a un match, league o cup", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Chat encontrado", content = @Content(schema = @Schema(implementation = ChatMessagesResponse.class))),
        @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Chat no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public ResponseEntity<ChatMessagesResponse> getChatByParent(
        @Parameter(description = "Tipo: MATCH, LEAGUE o CUP") @PathVariable final String parentType,
        @Parameter(description = "ID del recurso padre") @PathVariable final String parentId,
        @AuthenticationPrincipal final Jwt jwt) {

        final var dto = this.getChatByParent.handle(
            new GetChatByParentQuery(parentType, parentId, jwt.getSubject()));
        return ResponseEntity.ok(ChatMessagesResponse.from(dto));
    }

}
