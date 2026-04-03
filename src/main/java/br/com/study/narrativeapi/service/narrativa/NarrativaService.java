package br.com.study.narrativeapi.service.narrativa;

import br.com.study.narrativeapi.model.dto.NarrativaRequest;
import br.com.study.narrativeapi.model.dto.NarrativaResponse;

import java.util.List;

public interface NarrativaService {

    NarrativaResponse.Abertura iniciarSessao(NarrativaRequest.IniciarSessao request, Long usuarioId);

    NarrativaResponse.Turno narrarTurno(NarrativaRequest.NarrarTurno request, Long usuarioId);

    NarrativaResponse.Sessao buscarSessao(String sessaoId, Long usuarioId);

    List<NarrativaResponse.SessaoResumo> listarSessoesPorPersonagem(Long personagemId, Long usuarioId);
}