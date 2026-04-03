package br.com.study.narrativeapi.service;

import br.com.study.genericcrudmongo.controller.exception.DataIntegrityException;
import br.com.study.genericcrudmongo.controller.exception.ObjectNotFoundException;
import br.com.study.narrativeapi.feign.CombatApiFeign;
import br.com.study.narrativeapi.model.SessaoNarrativa;
import br.com.study.narrativeapi.model.dto.NarrativaRequest;
import br.com.study.narrativeapi.model.dto.NarrativaResponse;
import br.com.study.narrativeapi.model.enums.TipoNarracaoType;
import br.com.study.narrativeapi.repository.SessaoNarrativaRepository;
import br.com.study.narrativeapi.service.gemini.GeminiService;
import br.com.study.narrativeapi.service.narrativa.NarrativaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NarrativaServiceImpl")
class NarrativaServiceImplTest {

    @Mock
    SessaoNarrativaRepository repositorio;
    @Mock
    GeminiService geminiService;
    @Mock
    CombatApiFeign combatApiFeign;
    @InjectMocks
    NarrativaServiceImpl service;

    private static final Long USUARIO_ID = 1L;
    private static final Long PERSONAGEM_ID = 1L;
    private static final String SESSAO_ID = "sessao-uuid-123";
    private static final String BATALHA_ID = "batalha-uuid-456";

    private NarrativaRequest.IniciarSessao requestIniciar;
    private GeminiService.InimigoGerado inimigoGerado;
    private SessaoNarrativa sessaoAtiva;

    @BeforeEach
    void setUp() {
        requestIniciar = new NarrativaRequest.IniciarSessao(
                PERSONAGEM_ID, "Nexus-7",
                100, 150, 40, 50, 4,
                18, 12, 8, 5,
                500L, false, true
        );

        inimigoGerado = new GeminiService.InimigoGerado(
                "Vigilante de Dados",
                "Um cavaleiro feito de cabos de fibra ótica",
                "NORMAL",
                80, 12, 5, 100L,
                "O servidor pulsa. O protocolo aguarda."
        );

        sessaoAtiva = new SessaoNarrativa();
        sessaoAtiva.setId(SESSAO_ID);
        sessaoAtiva.setUsuarioId(USUARIO_ID);
        sessaoAtiva.setPersonagemId(PERSONAGEM_ID);
        sessaoAtiva.setNomePersonagem("Nexus-7");
        sessaoAtiva.setBatalhaId(BATALHA_ID);
        sessaoAtiva.setHollow(false);
    }

    // ─────────────────────────────────────────────
    // iniciarSessao
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("iniciarSessao")
    class IniciarSessao {

        @Test
        @DisplayName("inicia sessão com sucesso — gera inimigo e cria batalha")
        void requestValido_iniciaSessao() {
            when(geminiService.gerarInimigoEAbertura(any(), any(), anyInt()))
                    .thenReturn(inimigoGerado);
            when(repositorio.save(any())).thenAnswer(inv -> {
                SessaoNarrativa s = inv.getArgument(0);
                s.setId(SESSAO_ID);
                return s;
            });
            when(combatApiFeign.iniciarBatalha(any())).thenReturn(
                    ResponseEntity.ok(new CombatApiFeign.BatalhaIniciadaResponse(
                            BATALHA_ID, SESSAO_ID,
                            "Vigilante de Dados", "Cavaleiro de fibra ótica",
                            80, 12, 5, 100L
                    ))
            );

            NarrativaResponse.Abertura response = service.iniciarSessao(requestIniciar, USUARIO_ID);

            assertThat(response.sessaoId()).isEqualTo(SESSAO_ID);
            assertThat(response.batalhaId()).isEqualTo(BATALHA_ID);
            assertThat(response.nomeInimigo()).isEqualTo("Vigilante de Dados");
            assertThat(response.narracao()).isEqualTo("O servidor pulsa. O protocolo aguarda.");
            verify(geminiService).gerarInimigoEAbertura(any(), any(), anyInt());
            verify(combatApiFeign).iniciarBatalha(any());
        }

        @Test
        @DisplayName("lança DataIntegrityException quando personagem é Hollow")
        void personagemHollow_lancaExcecao() {
            var requestHollow = new NarrativaRequest.IniciarSessao(
                    PERSONAGEM_ID, "Nexus-7",
                    0, 150, 40, 50, 4,
                    18, 12, 8, 5,
                    0L, true, false
            );

            assertThatThrownBy(() -> service.iniciarSessao(requestHollow, USUARIO_ID))
                    .isInstanceOf(DataIntegrityException.class)
                    .hasMessageContaining("corrompido");

            verify(geminiService, never()).gerarInimigoEAbertura(any(), any(), anyInt());
            verify(combatApiFeign, never()).iniciarBatalha(any());
        }

        @Test
        @DisplayName("lança DataIntegrityException quando combat-api retorna null")
        void combatApiRetornaNull_lancaExcecao() {
            when(geminiService.gerarInimigoEAbertura(any(), any(), anyInt()))
                    .thenReturn(inimigoGerado);
            when(repositorio.save(any())).thenAnswer(inv -> {
                SessaoNarrativa s = inv.getArgument(0);
                s.setId(SESSAO_ID);
                return s;
            });
            when(combatApiFeign.iniciarBatalha(any()))
                    .thenReturn(ResponseEntity.ok(null));

            assertThatThrownBy(() -> service.iniciarSessao(requestIniciar, USUARIO_ID))
                    .isInstanceOf(DataIntegrityException.class)
                    .hasMessageContaining("Falha");
        }
    }

    // ─────────────────────────────────────────────
    // narrarTurno
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("narrarTurno")
    class NarrarTurno {

        @Test
        @DisplayName("narra turno em andamento com sucesso")
        void turnoEmAndamento_narraComSucesso() {
            var request = new NarrativaRequest.NarrarTurno(
                    SESSAO_ID, TipoNarracaoType.TURNO,
                    20, 8, false, null,
                    92, 60, 2,
                    List.of("ATACAR", "ESQUIVAR"),
                    null, null
            );

            when(repositorio.findByIdAndUsuarioId(SESSAO_ID, USUARIO_ID))
                    .thenReturn(Optional.of(sessaoAtiva));
            when(geminiService.narrarTurno(any(), any()))
                    .thenReturn("Sua lâmina de fragmento rasga o protocolo.");
            when(repositorio.save(any())).thenReturn(sessaoAtiva);

            NarrativaResponse.Turno response = service.narrarTurno(request, USUARIO_ID);

            assertThat(response.narracao()).isEqualTo("Sua lâmina de fragmento rasga o protocolo.");
            assertThat(response.tipo()).isEqualTo(TipoNarracaoType.TURNO);
            verify(geminiService).narrarTurno(any(), any());
            verify(repositorio).save(any());
        }

        @Test
        @DisplayName("narra crítico com memória queimada")
        void turnoComCritico_incluiMemoriaQueimada() {
            var request = new NarrativaRequest.NarrarTurno(
                    SESSAO_ID, TipoNarracaoType.TURNO,
                    45, 0, true, "O cheiro de pão da padaria da esquina",
                    100, 35, 3,
                    List.of("ATAQUE_FORTE"),
                    null, null
            );

            when(repositorio.findByIdAndUsuarioId(SESSAO_ID, USUARIO_ID))
                    .thenReturn(Optional.of(sessaoAtiva));
            when(geminiService.narrarTurno(any(), any()))
                    .thenReturn("Você sente um choque. O cheiro de pão desaparece para sempre.");
            when(repositorio.save(any())).thenReturn(sessaoAtiva);

            NarrativaResponse.Turno response = service.narrarTurno(request, USUARIO_ID);

            assertThat(response.narracao()).contains("cheiro de pão");
            // Verifica que o histórico contém a memória queimada
            boolean historicoCritico = sessaoAtiva.getHistorico().stream()
                    .anyMatch(m -> m.getConteudo().contains("CRÍTICO"));
            assertThat(historicoCritico).isTrue();
        }

        @Test
        @DisplayName("narra hollow digital e marca sessão como encerrada")
        void hollow_marcaSessaoEncerrada() {
            var request = new NarrativaRequest.NarrarTurno(
                    SESSAO_ID, TipoNarracaoType.HOLLOW,
                    0, 0, false, null,
                    0, 0, 5,
                    List.of(),
                    null, null
            );

            when(repositorio.findByIdAndUsuarioId(SESSAO_ID, USUARIO_ID))
                    .thenReturn(Optional.of(sessaoAtiva));
            when(geminiService.narrarTurno(any(), any()))
                    .thenReturn("Sua mente é propriedade do sistema agora.");
            when(repositorio.save(any())).thenReturn(sessaoAtiva);

            service.narrarTurno(request, USUARIO_ID);

            assertThat(sessaoAtiva.isHollow()).isTrue();
        }

        @Test
        @DisplayName("lança DataIntegrityException quando sessão já é hollow")
        void sessaoHollow_lancaExcecao() {
            sessaoAtiva.setHollow(true);
            var request = new NarrativaRequest.NarrarTurno(
                    SESSAO_ID, TipoNarracaoType.TURNO,
                    20, 8, false, null,
                    92, 60, 2, List.of("ATACAR"), null, null
            );

            when(repositorio.findByIdAndUsuarioId(SESSAO_ID, USUARIO_ID))
                    .thenReturn(Optional.of(sessaoAtiva));

            assertThatThrownBy(() -> service.narrarTurno(request, USUARIO_ID))
                    .isInstanceOf(DataIntegrityException.class)
                    .hasMessageContaining("protocolo");

            verify(geminiService, never()).narrarTurno(any(), any());
        }

        @Test
        @DisplayName("lança ObjectNotFoundException quando sessão não existe")
        void sessaoInexistente_lancaExcecao() {
            var request = new NarrativaRequest.NarrarTurno(
                    "id-invalido", TipoNarracaoType.TURNO,
                    20, 8, false, null,
                    92, 60, 2, List.of("ATACAR"), null, null
            );

            when(repositorio.findByIdAndUsuarioId("id-invalido", USUARIO_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.narrarTurno(request, USUARIO_ID))
                    .isInstanceOf(ObjectNotFoundException.class);
        }

        @Test
        @DisplayName("salva três mensagens no histórico por turno")
        void turnoValido_salvaHistoricoCompleto() {
            var request = new NarrativaRequest.NarrarTurno(
                    SESSAO_ID, TipoNarracaoType.TURNO,
                    20, 8, false, null,
                    92, 60, 2,
                    List.of("ATACAR"),
                    null, null
            );

            when(repositorio.findByIdAndUsuarioId(SESSAO_ID, USUARIO_ID))
                    .thenReturn(Optional.of(sessaoAtiva));
            when(geminiService.narrarTurno(any(), any()))
                    .thenReturn("O protocolo processa sua ação.");
            when(repositorio.save(any())).thenReturn(sessaoAtiva);

            service.narrarTurno(request, USUARIO_ID);

            // Ação do jogador + resultado mecânico + narração do GM = 3 mensagens
            assertThat(sessaoAtiva.getHistorico()).hasSize(3);
        }
    }
}