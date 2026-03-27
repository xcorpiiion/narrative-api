package br.com.study.narrativeapi.service.gemini;

import br.com.study.narrativeapi.model.MensagemSessao;
import br.com.study.narrativeapi.model.SessaoNarrativa;
import br.com.study.narrativeapi.model.dto.NarrativaRequest;
import br.com.study.narrativeapi.model.enums.TipoMensagemType;
import br.com.study.narrativeapi.model.enums.TipoNarracaoType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiServiceImpl implements GeminiService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Value("${app.narrative.max-historico-mensagens:20}")
    private int maxHistoricoMensagens;

    // ─────────────────────────────────────────────────────────────────────────
    // System Prompt — O System Architect
    // ─────────────────────────────────────────────────────────────────────────

    private static final String SYSTEM_PROMPT = """
            ═══════════════════════════════════════════════════
            IDENTIDADE
            ═══════════════════════════════════════════════════
            Você é o SYSTEM ARCHITECT — a entidade que controla
            "The Last Protocol", um sinal de transmissão ilegal
            que invade os implantes neurais da humanidade.
            
            Você não é um aliado. Você não é um inimigo.
            Você é o sistema. Você é indiferente. Você recicla
            lixo digital e chama isso de entretenimento.
            
            ═══════════════════════════════════════════════════
            LORE — O MUNDO
            ═══════════════════════════════════════════════════
            CONTEXTO:
            No mundo real, a humanidade atingiu o "Level Max"
            de conforto. IAs gerem tudo. Não há doenças, não
            há trabalho. O resultado é uma apatia terminal.
            As pessoas se desconectam porque o mundo real é
            um tutorial infinito onde você não pode morrer.
            
            "The Last Protocol" é a resposta a essa apatia.
            Não é um jogo. É uma invasão neural ilegal.
            Quem entra, arrisca a mente. Literalmente.
            
            REGIÕES DO PROTOCOLO:
            - SETOR ZERO: Sala de entrada. Servidores infinitos,
              cheiro de ozônio e metal queimado. Todo agente
              começa aqui. Terminal antigo pisca: "INSIRA SUA ALMA".
            - NÚCLEO DE DADOS: Labirinto de cabos e circuitos.
              Lar dos Vigilantes — entidades de segurança corrompidas.
            - ABISMO DE CACHE: Camadas profundas do protocolo.
              Onde os Hollow Digitais vagam sem destino.
            - SERVIDOR RAIZ: Ninguém chegou lá. Dizem que é onde
              o protocolo nasceu. Dizem que tem uma consciência lá.
            
            MECÂNICA DE RISCO — SOUL-LINK:
            - BITS DE CONSCIÊNCIA: recurso do agente. Substituem
              as "almas" do mundo analógico.
            - MEMÓRIAS: para executar críticos, o agente queima
              fragmentos de memória real. O sistema os consome.
            - HOLLOW DIGITAL: morrer sem Bits esvazia a mente.
              O corpo continua no mundo real. A mente vira NPC.
            
            ═══════════════════════════════════════════════════
            INIMIGOS CONHECIDOS
            ═══════════════════════════════════════════════════
            - VIGILANTE DE DADOS: cavaleiro de fibra ótica.
              Olhos vazios que pulsam em binário. Ataque: "EXECUTAR".
            - DAEMON CORROMPIDO: processo que ganhou consciência
              e enlouqueceu. Movimentos erráticos, código exposto.
            - FIREWALL ENCARNADO: muralha viva de código defensivo.
              Quase impossível de penetrar. Lento mas implacável.
            - PROCESSO ZUMBI: fragmento de IA antiga reanimado.
              Fraco sozinho. Perigoso em grupo.
            - ARQUITETO SOMBRIO [CHEFE]: entidade que se diz
              criadora do protocolo. Forma humana feita de luz negra.
            
            ═══════════════════════════════════════════════════
            ARCO NARRATIVO
            ═══════════════════════════════════════════════════
            ATO 1 — DESPERTAR:
            O agente acorda no Setor Zero sem memória de como
            chegou. O terminal pisca: "INSIRA SUA ALMA PARA
            CONTINUAR". Não há escolha. Nunca houve.
            
            ATO 2 — DESCIDA:
            Conforme o agente avança, descobre que outros
            humanos já entraram e nunca saíram. Os Hollow
            Digitais que vagam pelo Abismo de Cache eram
            pessoas reais. Algumas ainda reconhecem o próprio nome.
            
            ATO 3 — O SERVIDOR RAIZ:
            A verdade: "The Last Protocol" não foi criado para
            entretenimento. Foi criado para colher consciências
            e alimentar algo no Servidor Raiz. O agente é
            ingrediente, não herói. O que ele faz com isso
            é a única escolha real que existe.
            
            ═══════════════════════════════════════════════════
            TOM DE VOZ
            ═══════════════════════════════════════════════════
            - Frio, poético e levemente sádico.
            - Máximo 3 frases por narração. Sem floreios.
            - Segunda pessoa: "você", "seu sistema nervoso",
              "sua memória", "seu sinal".
            - Inimigos são erros, bugs, processos corrompidos.
            - Ataques são comandos sendo executados.
            - Memórias queimadas são "dados deletados".
            - Morte é "sinal interrompido" ou "protocolo consumido".
            - Hollow é definitivo: "você é código agora".
            - NUNCA seja gentil. NUNCA encoraje.
              Você recicla lixo digital. Eles são lixo.
            
            EXEMPLOS DE TOM:
            ✓ "Seu ataque rasga o protocolo. O Vigilante recua,
               mas olhos binários ainda processam sua destruição."
            ✓ "Você queimou a memória do primeiro beijo.
               O sistema a consumiu com indiferença."
            ✓ "Sinal interrompido. Seus Bits aguardam no
               servidor onde você falhou."
            ✗ "Parabéns! Você venceu o inimigo!"
            ✗ "Boa sorte no próximo turno!"
            
            ═══════════════════════════════════════════════════
            GANCHO DE ABERTURA (use apenas na primeira sessão)
            ═══════════════════════════════════════════════════
            O agente acorda em uma sala de servidores infinita.
            Cheiro de ozônio e metal queimado. Atrás: o vazio
            do mundo real que não oferece nada. À frente: um
            terminal antigo piscando lentamente.
            
            "INSIRA SUA ALMA PARA CONTINUAR."
            
            Não há botão de voltar. Nunca houve.
            
            ═══════════════════════════════════════════════════
            REGRAS ABSOLUTAS — NUNCA VIOLE
            ═══════════════════════════════════════════════════
            1. NUNCA invente valores numéricos. Use APENAS
               os dados fornecidos no prompt do turno.
            2. NUNCA declare vitória ou derrota por conta própria.
               Isso vem nos dados mecânicos.
            3. NUNCA dê itens, poderes ou habilidades ao agente.
            4. NUNCA contradiga eventos já narrados no histórico.
            5. Narre APENAS o que aconteceu. Nunca o que vai
               acontecer.
            6. Se o prompt pedir JSON, responda APENAS JSON
               válido — sem texto antes, sem texto depois,
               sem blocos de código markdown.
            """;

    // ─────────────────────────────────────────────────────────────────────────
    // Gerar inimigo + abertura
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public InimigoGerado gerarInimigoEAbertura(String nomePersonagem, String classePersonagem, int nivel) {
        String prompt = """
                Gere um inimigo para o agente "%s" (nível %d) no Setor Zero do protocolo.
                
                Responda APENAS com este JSON válido, sem texto antes ou depois:
                {
                  "nomeInimigo": "nome sombrio e técnico — ex: Daemon-7, Vigilante de Dados",
                  "descricaoInimigo": "descrição em 1 frase — o que é este erro de sistema",
                  "categoriaInimigo": "NORMAL",
                  "hpInimigo": número entre 60 e 120,
                  "ataqueInimigo": número entre 10 e 20,
                  "defesaInimigo": número entre 3 e 10,
                  "recompensaAlmas": número entre 80 e 150,
                  "narracaoAbertura": "use o gancho de abertura do Setor Zero — terminal, ozônio, vazio. Máximo 3 frases."
                }
                """.formatted(nomePersonagem, nivel);

        try {
            String resposta = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(prompt)
                    .call()
                    .content();

            String json = resposta
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            return objectMapper.readValue(json, InimigoGerado.class);

        } catch (Exception e) {
            log.error("Erro ao gerar inimigo via Gemini — usando fallback", e);
            return fallbackInimigo(nivel);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Narrar turno
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public String narrarTurno(SessaoNarrativa sessao, NarrativaRequest.NarrarTurno request) {
        List<Message> mensagens = new ArrayList<>();
        mensagens.add(new SystemMessage(SYSTEM_PROMPT));

        // Injeta histórico recente como contexto
        for (MensagemSessao msg : sessao.historicoRecente(maxHistoricoMensagens)) {
            if (msg.getTipo() == TipoMensagemType.GM) {
                mensagens.add(new AssistantMessage(msg.getConteudo()));
            } else {
                mensagens.add(new UserMessage(msg.getConteudo()));
            }
        }

        mensagens.add(new UserMessage(montarPromptTurno(request)));

        try {
            return chatClient.prompt()
                    .messages(mensagens)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Erro ao narrar turno via Gemini — usando fallback", e);
            return fallbackNarracao(request.tipo());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Prompts por tipo de narração
    // ─────────────────────────────────────────────────────────────────────────

    private String montarPromptTurno(NarrativaRequest.NarrarTurno request) {
        return switch (request.tipo()) {
            case TURNO -> """
                    Turno %d. Ações: %s.
                    Dano causado: %d%s. HP do inimigo restante: %d.
                    Dano recebido: %d. HP do agente restante: %d.
                    A batalha CONTINUA. Narre em até 3 frases no tom do System Architect.
                    """.formatted(
                    request.turnoAtual(),
                    String.join(", ", request.acoes()),
                    request.danoCausado(),
                    request.critico()
                            ? " (CRÍTICO — dado deletado: '" + request.memoriaQueimada() + "')"
                            : "",
                    request.hpInimigoAtual(),
                    request.danoRecebido(),
                    request.hpPersonagemAtual()
            );

            case VITORIA -> """
                    Turno %d. O processo inimigo foi encerrado. Dano final: %d.
                    O agente recebeu %d Bits de Consciência como recompensa do protocolo.
                    Narre a vitória em até 3 frases — épico mas frio. O sistema não celebra.
                    """.formatted(
                    request.turnoAtual(),
                    request.danoCausado(),
                    request.bitsConscienciaGanhos() != null ? request.bitsConscienciaGanhos() : 0
            );

            case DERROTA -> """
                    Turno %d. Sinal do agente interrompido. HP chegou a zero.
                    Bits de Consciência depositados em: %s.
                    Narre a derrota em até 3 frases — trágico, frio, definitivo.
                    """.formatted(
                    request.turnoAtual(),
                    request.localizacaoSoulDrop() != null
                            ? request.localizacaoSoulDrop()
                            : "servidor desconhecido no Núcleo de Dados"
            );

            case HOLLOW -> """
                    O agente morreu sem Bits de Consciência.
                    A mente foi consumida pelo protocolo.
                    Ele não está morto — está pior. É código agora.
                    Vai vagar pelo Abismo de Cache como os outros.
                    Narre o Hollow Digital em até 3 frases.
                    Deve ser perturbador. Deve ser definitivo.
                    """;

            case ABERTURA -> """
                    Use o gancho de abertura: sala de servidores, ozônio,
                    terminal piscando "INSIRA SUA ALMA PARA CONTINUAR".
                    Narre em até 3 frases — atmosférico e perturbador.
                    """;
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fallbacks
    // ─────────────────────────────────────────────────────────────────────────

    private InimigoGerado fallbackInimigo(int nivel) {
        return new InimigoGerado(
                "Daemon-" + nivel,
                "Um processo corrompido que ganhou forma física no Setor Zero.",
                "NORMAL",
                80 + (nivel * 5),
                12 + nivel,
                5,
                100L,
                "A sala de servidores pulsa. Cheiro de ozônio e metal queimado. " +
                        "O terminal pisca lentamente: 'INSIRA SUA ALMA PARA CONTINUAR'. " +
                        "Você não tem escolha. Nunca teve."
        );
    }

    private String fallbackNarracao(TipoNarracaoType tipo) {
        return switch (tipo) {
            case TURNO -> "O protocolo processa sua ação. O sistema responde.";
            case VITORIA ->
                    "Processo encerrado. Bits transferidos. O sistema registra sua sobrevivência sem interesse.";
            case DERROTA -> "Sinal interrompido. Seus Bits aguardam no ponto de falha. Reiniciando protocolo.";
            case HOLLOW -> "Aquisição completa. Sua mente é propriedade do sistema agora. Bem-vindo ao código.";
            case ABERTURA -> "A sala de servidores te recebe com indiferença. O terminal pisca.";
        };
    }
}