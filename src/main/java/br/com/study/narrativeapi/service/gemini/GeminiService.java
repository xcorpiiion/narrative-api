package br.com.study.narrativeapi.service.gemini;


import br.com.study.narrativeapi.model.SessaoNarrativa;
import br.com.study.narrativeapi.model.dto.NarrativaRequest;

public interface GeminiService {

    /**
     * Gera o inimigo e a narração de abertura da sessão.
     * Retorna JSON com os dados do inimigo + texto narrativo.
     */
    InimigoGerado gerarInimigoEAbertura(String nomePersonagem, String classePersonagem, int nivel);

    /**
     * Narra um turno de batalha com base no resultado mecânico.
     * Usa o histórico da sessão para manter coerência.
     */
    String narrarTurno(SessaoNarrativa sessao, NarrativaRequest.NarrarTurno request);

    // ─── DTO interno ──────────────────────────────────────────────────────────

    record InimigoGerado(
            String nomeInimigo,
            String descricaoInimigo,
            String categoriaInimigo,
            int hpInimigo,
            int ataqueInimigo,
            int defesaInimigo,
            long recompensaAlmas,
            String narracaoAbertura
    ) {
    }
}