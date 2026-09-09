package br.com.fiap.logitech.pedidos.dominio;

import java.util.List;
import java.util.Optional;

/**
 * GABARITO, TODO-1 (DIP). Porta de saída da persistência de pedidos.
 *
 * <p>Repare em <strong>onde</strong> esta interface mora: no pacote
 * {@code dominio}, junto da regra de negócio, e não no pacote {@code infra}
 * junto da implementação. Isso não é organização de pastas, é a inversão em
 * si. Quem define o contrato é quem precisa dele, não quem o cumpre: o domínio
 * declara "eu preciso salvar pedidos", e a infraestrutura que se vire para
 * atender. Se esta interface morasse em {@code infra}, o domínio continuaria
 * dependendo da infraestrutura, só que através de um tipo mais bonito.</p>
 *
 * <p>Repare também no que ela <strong>não</strong> tem: nada de JPA, nada de
 * {@code Optional} vindo de framework, nenhuma anotação. Só o vocabulário do
 * negócio. É esse silêncio que permite trocar PostgreSQL por qualquer outra
 * coisa sem que {@code PedidoService} tome conhecimento.</p>
 */
public interface PedidoRepository {

    Pedido salvar(Pedido pedido);

    Optional<Pedido> porId(String id);

    List<Pedido> todos();
}
