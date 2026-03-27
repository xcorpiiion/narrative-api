package br.com.study.narrativeapi.model.dto;


import br.com.study.narrativeapi.model.enums.TipoNarracaoType;

public class NarrativaResponse {

    /**
     * Retornado ao iniciar a sessão.
     * Contém a narração de abertura + dados da batalha criada.
     */
    public record Abertura(
            String sessaoId,
            String batalhaId,

            // Narração de abertura do System Architect
            String narracao,

            // Inimigo gerado pelo GM
            String nomeInimigo,
            String descricaoInimigo,
            int hpInimigo,
            int ataqueInimigo,
            int defesaInimigo,
            long recompensaAlmas
    ) {}

    /**
     * Retornado após cada narração de turno.
     * Contém só a narração — dados mecânicos o front já tem da combat-api.
     */
    public record Turno(
            String sessaoId,
            TipoNarracaoType tipo,
            String narracao,
            int totalTurnos
    ) {}
}