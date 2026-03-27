package br.com.study.narrativeapi.service.narrativa;

import br.com.study.genericcrudmongo.service.exception.DataIntegrityException;
import br.com.study.genericcrudmongo.service.exception.ObjectNotFoundException;
import br.com.study.narrativeapi.feign.CombatApiFeign;
import br.com.study.narrativeapi.model.MensagemSessao;
import br.com.study.narrativeapi.model.SessaoNarrativa;
import br.com.study.narrativeapi.model.dto.NarrativaRequest;
import br.com.study.narrativeapi.model.dto.NarrativaResponse;
import br.com.study.narrativeapi.model.enums.CategoriaInimigoType;
import br.com.study.narrativeapi.repository.SessaoNarrativaRepository;
import br.com.study.narrativeapi.service.gemini.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NarrativaServiceImpl implements NarrativaService {

    private final SessaoNarrativaRepository repositorio;
    private final GeminiService geminiService;
    private final CombatApiFeign combatApiFeign;

    // ─────────────────────────────────────────────
    // Iniciar sessão
    // ─────────────────────────────────────────────

    @Override
    public NarrativaResponse.Abertura iniciarSessao(NarrativaRequest.IniciarSessao request, Long usuarioId) {
        if (request.hollow()) {
            throw new DataIntegrityException(
                    "Agente corrompido. Sua mente pertence ao sistema. Acesso negado."
            );
        }

        // 1. Gemini gera o inimigo e a narração de abertura
        log.info("Gerando inimigo via Gemini para {} nível...", request.nomePersonagem());
        GeminiService.InimigoGerado inimigo = geminiService.gerarInimigoEAbertura(
                request.nomePersonagem(),
                "AGENTE", // classe genérica — o GM não sabe a classe técnica
                1
        );

        // 2. Cria a sessão narrativa no MongoDB
        SessaoNarrativa sessao = new SessaoNarrativa();
        sessao.setUsuarioId(usuarioId);
        sessao.setPersonagemId(request.personagemId());
        sessao.setNomePersonagem(request.nomePersonagem());
        sessao.adicionarMensagem(MensagemSessao.doGm(inimigo.narracaoAbertura()));
        sessao = repositorio.save(sessao);

        // 3. Chama a combat-api via Feign para iniciar a batalha
        log.info("Iniciando batalha na combat-api — inimigo: {}", inimigo.nomeInimigo());
        var batalha = combatApiFeign.iniciarBatalha(
                new CombatApiFeign.IniciarBatalhaRequest(
                        request.personagemId(),
                        sessao.getId(),
                        request.nomePersonagem(),
                        request.hpAtual(), request.hpMaximo(),
                        request.mpAtual(), request.mpMaximo(),
                        request.apMaximo(),
                        request.ataque(), request.defesa(),
                        request.velocidade(), request.sorte(),
                        request.bitsConsciencia(),
                        request.hollow(),
                        request.temMemoriasDisponiveis(),
                        inimigo.nomeInimigo(),
                        inimigo.descricaoInimigo(),
                        CategoriaInimigoType.valueOf(inimigo.categoriaInimigo()),
                        inimigo.hpInimigo(),
                        inimigo.ataqueInimigo(),
                        inimigo.defesaInimigo(),
                        inimigo.recompensaAlmas()
                )
        ).getBody();

        if (batalha == null) {
            throw new DataIntegrityException("Falha ao iniciar batalha no servidor de combate.");
        }

        // 4. Atualiza a sessão com o batalhaId
        sessao.setBatalhaId(batalha.batalhaId());
        repositorio.save(sessao);

        log.info("Sessão iniciada: {} | batalha: {}", sessao.getId(), batalha.batalhaId());

        return new NarrativaResponse.Abertura(
                sessao.getId(),
                batalha.batalhaId(),
                inimigo.narracaoAbertura(),
                inimigo.nomeInimigo(),
                inimigo.descricaoInimigo(),
                inimigo.hpInimigo(),
                inimigo.ataqueInimigo(),
                inimigo.defesaInimigo(),
                inimigo.recompensaAlmas()
        );
    }

    // ─────────────────────────────────────────────
    // Narrar turno
    // ─────────────────────────────────────────────

    @Override
    public NarrativaResponse.Turno narrarTurno(NarrativaRequest.NarrarTurno request, Long usuarioId) {
        SessaoNarrativa sessao = repositorio.findByIdAndUsuarioId(request.sessaoId(), usuarioId)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Sessão não encontrada: " + request.sessaoId()
                ));

        if (sessao.isHollow()) {
            throw new DataIntegrityException("Esta sessão foi encerrada pelo protocolo. Sem retorno.");
        }

        // Registra a ação do jogador no histórico
        String acoes = request.acoes() != null ? String.join(", ", request.acoes()) : "ação desconhecida";
        sessao.adicionarMensagem(MensagemSessao.doJogador(acoes));

        // Registra o resultado mecânico no histórico
        String resultadoMecanico = montarResultadoMecanico(request);
        sessao.adicionarMensagem(MensagemSessao.doSistema(resultadoMecanico));

        // Gemini narra o turno
        String narracao = geminiService.narrarTurno(sessao, request);

        // Salva narração e atualiza sessão
        sessao.adicionarMensagem(MensagemSessao.doGm(narracao));
        sessao.incrementarTurno();

        // Marca hollow se necessário
        if (request.tipo().name().equals("HOLLOW")) {
            sessao.setHollow(true);
            log.info("Sessão {} encerrada por Hollow Digital", sessao.getId());
        }

        repositorio.save(sessao);

        return new NarrativaResponse.Turno(
                sessao.getId(),
                request.tipo(),
                narracao,
                sessao.getTotalTurnos()
        );
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private String montarResultadoMecanico(NarrativaRequest.NarrarTurno request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Turno ").append(request.turnoAtual()).append(": ");
        sb.append("dano causado=").append(request.danoCausado());
        sb.append(", dano recebido=").append(request.danoRecebido());
        sb.append(", HP agente=").append(request.hpPersonagemAtual());
        sb.append(", HP inimigo=").append(request.hpInimigoAtual());

        if (request.critico()) {
            sb.append(", CRÍTICO executado");
            if (request.memoriaQueimada() != null) {
                sb.append(" — memória perdida: ").append(request.memoriaQueimada());
            }
        }

        return sb.toString();
    }
}