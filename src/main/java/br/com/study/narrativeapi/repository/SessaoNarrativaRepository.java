package br.com.study.narrativeapi.repository;

import br.com.study.narrativeapi.model.SessaoNarrativa;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessaoNarrativaRepository extends MongoRepository<SessaoNarrativa, String> {

    /**
     * Busca sessão ativa pelo personagem — garante que pertence ao usuário
     */
    Optional<SessaoNarrativa> findByIdAndUsuarioId(String id, Long usuarioId);

    /**
     * Histórico de todas as sessões de um personagem — inclusive encerradas
     */
    List<SessaoNarrativa> findByPersonagemIdOrderByCriadaEmDesc(Long personagemId);

    /**
     * Verifica se o personagem já tem uma sessão ativa com batalha em andamento
     */
    boolean existsByPersonagemIdAndBatalhaIdAndHollowFalse(Long personagemId, String batalhaId);
}