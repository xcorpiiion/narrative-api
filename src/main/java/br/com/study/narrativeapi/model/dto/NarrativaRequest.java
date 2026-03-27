package br.com.study.narrativeapi.model.dto;

import br.com.study.narrativeapi.model.enums.TipoNarracaoType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class NarrativaRequest {

    /**
     * Inicia uma nova sessão narrativa.
     * Chamado pelo front ao começar uma aventura.
     * A narrative-api gera o inimigo, chama a combat-api e retorna a abertura.
     */
    public record IniciarSessao(

            @NotNull(message = "ID do personagem é obrigatório")
            Long personagemId,

            @NotBlank(message = "Nome do personagem é obrigatório")
            String nomePersonagem,

            // Snapshot do personagem pra combat-api
            @NotNull int hpAtual,
            @NotNull int hpMaximo,
            @NotNull int mpAtual,
            @NotNull int mpMaximo,
            @NotNull int apMaximo,
            @NotNull int ataque,
            @NotNull int defesa,
            @NotNull int velocidade,
            @NotNull int sorte,
            long bitsConsciencia,
            boolean hollow,
            boolean temMemoriasDisponiveis
    ) {}

    /**
     * Solicita narração de um turno já processado pela combat-api.
     * O front chama após receber o resultado do turno.
     */
    public record NarrarTurno(

            @NotBlank(message = "ID da sessão é obrigatório")
            String sessaoId,

            @NotNull(message = "Tipo de narração é obrigatório")
            TipoNarracaoType tipo,

            // Resultado do turno vindo da combat-api
            int danoCausado,
            int danoRecebido,
            boolean critico,
            String memoriaQueimada,

            int hpPersonagemAtual,
            int hpInimigoAtual,
            int turnoAtual,

            // Ação do jogador — pra GM narrar o que aconteceu
            List<String> acoes,

            // Preenchido em vitória
            Long bitsConscienciaGanhos,

            // Preenchido em derrota
            String localizacaoSoulDrop
    ) {}
}