package br.com.study.narrativeapi.feign;

import br.com.study.narrativeapi.model.enums.CategoriaInimigoType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Client Feign para a combat-api.
 * Chamado ao iniciar sessão para criar a batalha com o inimigo gerado pelo GM.
 */
@FeignClient(name = "combat-api")
public interface CombatApiFeign {

    @PostMapping(
            value = "/batalhas",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    ResponseEntity<BatalhaIniciadaResponse> iniciarBatalha(@RequestBody IniciarBatalhaRequest request);

    // ─── DTOs internos ────────────────────────────────────────────────────────

    record IniciarBatalhaRequest(
            Long personagemId,
            String sessaoId,
            String nomePersonagem,
            int hpAtual, int hpMaximo,
            int mpAtual, int mpMaximo,
            int apMaximo,
            int ataque, int defesa,
            int velocidade, int sorte,
            long bitsConsciencia,
            boolean hollow,
            boolean temMemoriasDisponiveis,
            String nomeInimigo,
            String descricaoInimigo,
            CategoriaInimigoType categoriaInimigo,
            int hpInimigo,
            int ataqueInimigo,
            int defesaInimigo,
            long recompensaAlmas
    ) {
    }

    record BatalhaIniciadaResponse(
            String batalhaId,
            String sessaoId,
            String nomeInimigo,
            String descricaoInimigo,
            int hpInimigo,
            int ataqueInimigo,
            int defesaInimigo,
            long recompensaAlmas
    ) {
    }
}