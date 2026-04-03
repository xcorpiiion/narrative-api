package br.com.study.narrativeapi.model.dto;

import br.com.study.narrativeapi.model.MensagemSessao;
import br.com.study.narrativeapi.model.enums.TipoNarracaoType;

import java.time.Instant;
import java.util.List;

public class NarrativaResponse {

    public record Abertura(
            String sessaoId,
            String batalhaId,
            String narracao,
            String nomeInimigo,
            String descricaoInimigo,
            int hpInimigo,
            int ataqueInimigo,
            int defesaInimigo,
            long recompensaAlmas
    ) {
    }

    public record Turno(
            String sessaoId,
            TipoNarracaoType tipo,
            String narracao,
            int totalTurnos
    ) {
    }

    /**
     * Sessão completa com histórico de mensagens
     */
    public record Sessao(
            String sessaoId,
            Long personagemId,
            String nomePersonagem,
            String batalhaId,
            List<MensagemSessao> historico,
            int totalTurnos,
            boolean hollow,
            Instant criadaEm,
            Instant atualizadaEm
    ) {
    }

    /**
     * Resumo da sessão — para listagem sem histórico completo
     */
    public record SessaoResumo(
            String sessaoId,
            Long personagemId,
            String nomePersonagem,
            String batalhaId,
            int totalTurnos,
            boolean hollow,
            Instant criadaEm,
            Instant atualizadaEm
    ) {
    }
}