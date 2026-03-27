package br.com.study.narrativeapi.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Sessão narrativa do GM — persiste o histórico completo da aventura.
 *
 * Uma sessão representa uma run do jogador com um personagem.
 * O histórico é enviado ao Gemini a cada request para manter
 * coerência narrativa entre turnos e sessões.
 *
 * Ciclo de vida:
 * - Criada ao iniciar uma nova aventura
 * - Atualizada a cada turno com a narração do GM
 * - Mantida mesmo após o fim da batalha — memória entre sessões
 * - Encerrada definitivamente em caso de Hollow Digital
 */
@Getter
@Setter
@Document(collection = "sessoes_narrativas")
public class SessaoNarrativa {

    @Id
    private String id;

    @Field("usuario_id")
    private Long usuarioId;

    @Field("personagem_id")
    private Long personagemId;

    @Field("nome_personagem")
    private String nomePersonagem;

    @Field("batalha_id")
    private String batalhaId;

    /** Histórico completo de mensagens da sessão */
    private List<MensagemSessao> historico = new ArrayList<>();

    /** Total de turnos narrados */
    @Field("total_turnos")
    private int totalTurnos = 0;

    /** Se true, sessão encerrada por Hollow Digital — não pode ser retomada */
    @Field("hollow")
    private boolean hollow = false;

    private Instant criadaEm = Instant.now();

    @Field("atualizada_em")
    private Instant atualizadaEm = Instant.now();

    // ─── Utilitários ──────────────────────────────────────────────────────────

    public void adicionarMensagem(MensagemSessao mensagem) {
        this.historico.add(mensagem);
        this.atualizadaEm = Instant.now();
    }

    public void incrementarTurno() {
        this.totalTurnos++;
    }

    /**
     * Retorna as últimas N mensagens do histórico.
     * Usado para limitar tokens enviados ao Gemini.
     */
    public List<MensagemSessao> historicoRecente(int maxMensagens) {
        if (historico.size() <= maxMensagens) return historico;
        return historico.subList(historico.size() - maxMensagens, historico.size());
    }
}