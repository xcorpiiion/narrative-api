package br.com.study.narrativeapi.controller;

import br.com.study.genericauthorization.annotation.CurrentUser;
import br.com.study.genericauthorization.model.UserPrincipal;
import br.com.study.narrativeapi.model.dto.NarrativaRequest;
import br.com.study.narrativeapi.model.dto.NarrativaResponse;
import br.com.study.narrativeapi.service.narrativa.NarrativaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequiredArgsConstructor
@RequestMapping("/narrativa")
@Tag(name = "Narrativa", description = "Game Master com IA — The Last Protocol")
public class NarrativaController {

    private final NarrativaService service;

    @Operation(summary = "Inicia uma sessão — GM gera o inimigo e narra a abertura")
    @PostMapping(value = "/sessao",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<NarrativaResponse.Abertura> iniciarSessao(
            @Valid @RequestBody NarrativaRequest.IniciarSessao request,
            @CurrentUser UserPrincipal usuario
    ) {
        return ResponseEntity.status(CREATED)
                .body(service.iniciarSessao(request, usuario.getId()));
    }

    @Operation(summary = "Narra um turno — recebe resultado da combat-api e retorna narração do GM")
    @PostMapping(value = "/turno",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<NarrativaResponse.Turno> narrarTurno(
            @Valid @RequestBody NarrativaRequest.NarrarTurno request,
            @CurrentUser UserPrincipal usuario
    ) {
        return ResponseEntity.ok(service.narrarTurno(request, usuario.getId()));
    }
}