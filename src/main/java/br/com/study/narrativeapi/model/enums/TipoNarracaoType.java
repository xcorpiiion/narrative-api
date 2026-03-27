package br.com.study.narrativeapi.model.enums;

/**
 * Tipo de narração solicitada ao System Architect.
 * Cada tipo usa um prompt diferente pro Gemini.
 */
public enum TipoNarracaoType {

    /** Abertura da sessão — GM descreve o ambiente e apresenta o inimigo */
    ABERTURA,

    /** Narração de um turno de batalha — descreve o que aconteceu */
    TURNO,

    /** Vitória — GM narra a derrota do inimigo com tom épico */
    VITORIA,

    /** Derrota normal — GM narra a queda do personagem */
    DERROTA,

    /**
     * Hollow Digital — narração especial quando o personagem
     * morre sem Bits de Consciência e vira NPC permanentemente.
     * Tom mais pesado e definitivo.
     */
    HOLLOW
}