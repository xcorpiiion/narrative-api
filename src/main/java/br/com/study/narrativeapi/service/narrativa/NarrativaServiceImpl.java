package br.com.study.narrativeapi.service.narrativa;

import br.com.study.genericcrudmongo.controller.exception.DataIntegrityException;
import br.com.study.genericcrudmongo.controller.exception.ObjectNotFoundException;
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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NarrativaServiceImpl implements NarrativaService {

    private final SessaoNarrativaRepository repositorio;
    private final GeminiService geminiService;
    private final CombatApiFeign combatApiFeign;

    @Override
    public NarrativaResponse.Abertura iniciarSessao(NarrativaRequest.IniciarSessao request, Long usuarioId) {
        if (request.hollow()) {
            throw new DataIntegrityException(
                    "Agente corrompido. Sua mente pertence ao sistema. Acesso negado."
            );
        }

        log.info("Gerando inimigo via Gemini para {} nível...", request.nomePersonagem());
        GeminiService.InimigoGerado inimigo = geminiService.gerarInimigoEAbertura(
                request.nomePersonagem(), "AGENTE", 1
        );

        SessaoNarrativa sessao = new SessaoNarrativa();
        sessao.setUsuarioId(usuarioId);
        sessao.setPersonagemId(request.personagemId());
        sessao.setNomePersonagem(request.nomePersonagem());
        sessao.adicionarMensagem(MensagemSessao.doGm(inimigo.narracaoAbertura()));
        sessao = repositorio.save(sessao);

        log.info("Iniciando batalha na combat-api — inimigo: {}", inimigo.nomeInimigo());
        var batalha = combatApiFeign.iniciarBatalha(
                new CombatApiFeign.IniciarBatalhaRequest(
                        request.personagemId(), sessao.getId(),
                        request.nomePersonagem(),
                        request.hpAtual(), request.hpMaximo(),
                        request.mpAtual(), request.mpMaximo(),
                        request.apMaximo(),
                        request.ataque(), request.defesa(),
                        request.velocidade(), request.sorte(),
                        request.bitsConsciencia(), request.hollow(),
                        request.temMemoriasDisponiveis(),
                        inimigo.nomeInimigo(), inimigo.descricaoInimigo(),
                        CategoriaInimigoType.valueOf(inimigo.categoriaInimigo()),
                        inimigo.hpInimigo(), inimigo.ataqueInimigo(),
                        inimigo.defesaInimigo(), inimigo.recompensaAlmas()
                )
        ).getBody();

        if (batalha == null) {
            throw new DataIntegrityException("Falha ao iniciar batalha no servidor de combate.");
        }

        sessao.setBatalhaId(batalha.batalhaId());
        repositorio.save(sessao);

        log.info("Sessão iniciada: {} | batalha: {}", sessao.getId(), batalha.batalhaId());

        return new NarrativaResponse.Abertura(
                sessao.getId(), batalha.batalhaId(),
                inimigo.narracaoAbertura(), inimigo.nomeInimigo(),
                inimigo.descricaoInimigo(), inimigo.hpInimigo(),
                inimigo.ataqueInimigo(), inimigo.defesaInimigo(),
                inimigo.recompensaAlmas()
        );
    }

    @Override
    public NarrativaResponse.Turno narrarTurno(NarrativaRequest.NarrarTurno request, Long usuarioId) {
        SessaoNarrativa sessao = repositorio.findByIdAndUsuarioId(request.sessaoId(), usuarioId)
                .orElseThrow(() -> new ObjectNotFoundException("Sessão não encontrada: " + request.sessaoId()));

        if (sessao.isHollow()) {
            throw new DataIntegrityException("Esta sessão foi encerrada pelo protocolo. Sem retorno.");
        }

        String acoes = request.acoes() != null ? String.join(", ", request.acoes()) : "ação desconhecida";
        sessao.adicionarMensagem(MensagemSessao.doJogador(acoes));
        sessao.adicionarMensagem(MensagemSessao.doSistema(montarResultadoMecanico(request)));

        String narracao = geminiService.narrarTurno(sessao, request);
        sessao.adicionarMensagem(MensagemSessao.doGm(narracao));
        sessao.incrementarTurno();

        if (request.tipo().name().equals("HOLLOW")) {
            sessao.setHollow(true);
            log.info("Sessão {} encerrada por Hollow Digital", sessao.getId());
        }

        repositorio.save(sessao);

        return new NarrativaResponse.Turno(
                sessao.getId(), request.tipo(), narracao, sessao.getTotalTurnos()
        );
    }

    @Override
    public NarrativaResponse.Sessao buscarSessao(String sessaoId, Long usuarioId) {
        SessaoNarrativa sessao = repositorio.findByIdAndUsuarioId(sessaoId, usuarioId)
                .orElseThrow(() -> new ObjectNotFoundException("Sessão não encontrada: " + sessaoId));

        return toSessao(sessao);
    }

    @Override
    public List<NarrativaResponse.SessaoResumo> listarSessoesPorPersonagem(Long personagemId, Long usuarioId) {
        return repositorio.findByPersonagemIdOrderByCriadaEmDesc(personagemId)
                .stream()
                .filter(s -> s.getUsuarioId().equals(usuarioId))
                .map(this::toSessaoResumo)
                .toList();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private NarrativaResponse.Sessao toSessao(SessaoNarrativa s) {
        return new NarrativaResponse.Sessao(
                s.getId(), s.getPersonagemId(), s.getNomePersonagem(),
                s.getBatalhaId(), s.getHistorico(), s.getTotalTurnos(),
                s.isHollow(), s.getCriadaEm(), s.getAtualizadaEm()
        );
    }

    private NarrativaResponse.SessaoResumo toSessaoResumo(SessaoNarrativa s) {
        return new NarrativaResponse.SessaoResumo(
                s.getId(), s.getPersonagemId(), s.getNomePersonagem(),
                s.getBatalhaId(), s.getTotalTurnos(),
                s.isHollow(), s.getCriadaEm(), s.getAtualizadaEm()
        );
    }

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