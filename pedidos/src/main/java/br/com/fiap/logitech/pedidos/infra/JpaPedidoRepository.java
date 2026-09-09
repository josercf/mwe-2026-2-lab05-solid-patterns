package br.com.fiap.logitech.pedidos.infra;

import br.com.fiap.logitech.pedidos.dominio.Pedido;
import br.com.fiap.logitech.pedidos.dominio.PedidoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Persistência de pedidos em PostgreSQL, via JPA/Hibernate.
 *
 * <p><strong>GABARITO, TODO-1 (DIP).</strong> O corpo dos métodos não mudou
 * uma linha: a inversão de dependência não é reescrever a persistência, é
 * mudar quem aponta para quem. Antes, {@code PedidoService} apontava para cá.
 * Agora as duas classes apontam para a interface do domínio, e a seta que ia
 * do negócio para a infraestrutura foi invertida.</p>
 *
 * <p>O {@code @Repository} continua: é assim que o Spring descobre esta classe
 * e a injeta onde alguém pediu um {@code PedidoRepository}. Como existe uma
 * única implementação, não é preciso qualificar nada.</p>
 */
@Repository
public class JpaPedidoRepository implements PedidoRepository {

    private final PedidoJpa jpa;

    public JpaPedidoRepository(PedidoJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Pedido salvar(Pedido pedido) {
        return jpa.save(pedido);
    }

    @Override
    public Optional<Pedido> porId(String id) {
        return jpa.findById(id);
    }

    @Override
    public List<Pedido> todos() {
        return jpa.findAll();
    }
}
