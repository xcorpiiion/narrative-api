package br.com.study.narrativeapi.model;

import br.com.study.narrativeapi.model.enums.TipoMensagemType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Uma mensagem no histórico da sessão.
 * Documento embutido dentro de SessaoNarrativa.
 * <p>
 * O histórico completo é enviado ao Gemini a cada request
 * para manter coerência narrativa entre turnos.
 */
@Getter
@Setter
public class MensagemSessao {

    private TipoMensagemType tipo;

    /**
     * Conteúdo da mensagem — narração do GM, ação do jogador ou resultado mecânico
     */
    private String conteudo;

    private Instant timestamp = Instant.now();

    public static MensagemSessao doGm(String conteudo) {
        MensagemSessao m = new MensagemSessao();
        m.setTipo(TipoMensagemType.GM);
        m.setConteudo(conteudo);
        return m;
    }

    public static MensagemSessao doJogador(String conteudo) {
        MensagemSessao m = new MensagemSessao();
        m.setTipo(TipoMensagemType.GM.JOGADOR);
        m.setConteudo(conteudo);
        return m;
    }

    public static MensagemSessao doSistema(String conteudo) {
        MensagemSessao m = new MensagemSessao();
        m.setTipo(TipoMensagemType.GM.SISTEMA);
        m.setConteudo(conteudo);
        return m;
    }
}