package br.com.study.narrativeapi.model.enums;

/**
 * Tipo de cada mensagem no histórico da sessão.
 * Enviado ao Gemini para montar o contexto da conversa.
 */
public enum TipoMensagemType {

    /** Mensagem do System Architect (GM) — narração, descrição de cena */
    GM,

    /** Ação do jogador — combo executado, item usado */
    JOGADOR,

    /** Resultado mecânico do turno — dano, HP, memória queimada */
    SISTEMA
}