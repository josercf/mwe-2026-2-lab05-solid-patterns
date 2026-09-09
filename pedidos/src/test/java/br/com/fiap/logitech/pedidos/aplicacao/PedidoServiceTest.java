package br.com.fiap.logitech.pedidos.aplicacao;

import br.com.fiap.logitech.pedidos.dominio.NovoPedido;
import br.com.fiap.logitech.pedidos.dominio.Pedido;
import br.com.fiap.logitech.pedidos.dominio.PedidoRepository;
import br.com.fiap.logitech.pedidos.dominio.SolicitacaoFatura;
import br.com.fiap.logitech.pedidos.dominio.StatusPedido;
import br.com.fiap.logitech.pedidos.faturamento.ClienteFaturamento;
import br.com.fiap.logitech.pedidos.faturamento.ConectorBoleto;
import br.com.fiap.logitech.pedidos.faturamento.ConectorCartaoCorporativo;
import br.com.fiap.logitech.pedidos.faturamento.ConectorFaturaMensal;
import br.com.fiap.logitech.pedidos.faturamento.ConectorFaturamentoFactory;
import br.com.fiap.logitech.pedidos.faturamento.ConectorNaoEncontradoException;
import br.com.fiap.logitech.pedidos.faturamento.FaturamentoIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes de unidade da regra de negócio de Pedidos: sem Spring, sem PostgreSQL,
 * sem o serviço de Faturamento no ar. Rodam em milissegundos.
 *
 * <p>Já vêm verdes e precisam continuar verdes depois dos três TODOs.</p>
 */
class PedidoServiceTest {

    /**
     * Dublê de teste do repositório.
     *
     * <p><strong>GABARITO, TODO-1.</strong> Compare com o esqueleto: sumiram o
     * {@code extends JpaPedidoRepository}, o construtor com {@code super(null)}
     * e o import de {@code infra}. Este arquivo de teste não sabe mais que
     * existe um ORM no projeto, e é essa a medida do DIP que interessa. O
     * {@code super(null)} do esqueleto era o acoplamento cobrando o preço dele:
     * para testar regra de negócio, o teste era obrigado a construir meio ORM e
     * passar um {@code PedidoJpa} nulo que ninguém usava.</p>
     */
    static class RepositorioEmMemoria implements PedidoRepository {

        private final Map<String, Pedido> dados = new LinkedHashMap<>();

        @Override
        public Pedido salvar(Pedido pedido) {
            dados.put(pedido.getId(), pedido);
            return pedido;
        }

        @Override
        public Optional<Pedido> porId(String id) {
            return Optional.ofNullable(dados.get(id));
        }

        @Override
        public List<Pedido> todos() {
            return new ArrayList<>(dados.values());
        }
    }

    /** Dublê do serviço de Faturamento: guarda a última solicitação recebida. */
    static class FaturamentoFalso implements ClienteFaturamento {

        SolicitacaoFatura ultimaSolicitacao;
        boolean noAr = true;
        String numeroDevolvido = "NF-000001";

        @Override
        public String emitir(SolicitacaoFatura solicitacao) {
            this.ultimaSolicitacao = solicitacao;
            if (!noAr) {
                throw new FaturamentoIndisponivelException("faturamento fora do ar (dublê de teste)");
            }
            return numeroDevolvido;
        }
    }

    private RepositorioEmMemoria repositorio;
    private FaturamentoFalso faturamento;
    private PedidoService servico;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioEmMemoria();
        faturamento = new FaturamentoFalso();
        // GABARITO, TODO-2: aqui o teste faz à mão o que o Spring faz sozinho em
        // produção, montar a lista de conectores. Repare que o conector do
        // CONTRATO entra nesta lista sem que a fábrica ou o serviço mudem.
        ConectorFaturamentoFactory fabrica = new ConectorFaturamentoFactory(List.of(
                new ConectorBoleto(), new ConectorCartaoCorporativo(), new ConectorFaturaMensal()));
        servico = new PedidoService(repositorio, faturamento, fabrica);
    }

    private NovoPedido novoPedido(String tipoCliente) {
        return new NovoPedido("Distribuidora Sul", tipoCliente, "São Paulo/SP", "Curitiba/PR",
                "Rua das Araucárias, 480", new BigDecimal("120.0"), new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("cliente PADRAO: pedido é faturado por boleto e fica FATURADO")
    void clientePadraoFaturaPorBoleto() {
        Pedido pedido = servico.criar(novoPedido("PADRAO"));

        assertEquals("BOLETO", faturamento.ultimaSolicitacao.meioPagamento());
        assertEquals(StatusPedido.FATURADO, pedido.getStatus());
        assertEquals("NF-000001", pedido.getNumeroNotaFiscal());
        assertEquals(1, repositorio.todos().size());
    }

    @Test
    @DisplayName("cliente OURO: pedido é faturado no cartão corporativo com desconto")
    void clienteOuroFaturaNoCartao() {
        servico.criar(novoPedido("OURO"));

        assertEquals("CARTAO_CORPORATIVO", faturamento.ultimaSolicitacao.meioPagamento());
        assertEquals(0, new BigDecimal("950.00").compareTo(faturamento.ultimaSolicitacao.valor()));
    }

    @Test
    @DisplayName("faturamento fora do ar: o pedido é gravado como AGUARDANDO_FATURAMENTO")
    void faturamentoForaDoArNaoPerdeOPedido() {
        faturamento.noAr = false;

        Pedido pedido = servico.criar(novoPedido("PADRAO"));

        assertEquals(StatusPedido.AGUARDANDO_FATURAMENTO, pedido.getStatus());
        assertEquals(1, repositorio.todos().size());
    }

    @Test
    @DisplayName("tipo de cliente sem conector é recusado")
    void tipoDeClienteSemConectorEhRecusado() {
        assertThrows(ConectorNaoEncontradoException.class, () -> servico.criar(novoPedido("PRATA")));
    }

    @Test
    @DisplayName("alterar endereço grava o novo endereço no repositório")
    void alterarEnderecoGrava() {
        Pedido pedido = servico.criar(novoPedido("PADRAO"));

        servico.alterarEndereco(pedido.getId(), "Avenida das Torres, 1200");

        assertEquals("Avenida das Torres, 1200",
                repositorio.porId(pedido.getId()).orElseThrow().getEnderecoEntrega());
    }

    @Test
    @DisplayName("pedido inexistente devolve erro de não encontrado")
    void pedidoInexistente() {
        assertThrows(PedidoNaoEncontradoException.class, () -> servico.porId("nao-existe"));
    }

    /**
     * GABARITO, TODO-3. Este teste é o que prova o Aberto/Fechado do lado do
     * comportamento, mas o critério de verdade do passo é outro, e está fora
     * daqui: {@code git diff} sobre {@code PedidoService.java} não pode mostrar
     * nenhuma linha alterada por causa do CONTRATO. Um teste verde diz que o
     * tipo novo funciona; só o diff vazio diz que ele entrou sem custo.
     */
    @Test
    @DisplayName("cliente CONTRATO: pedido é faturado como FATURA_MENSAL, sem tocar no serviço")
    void clienteContratoFaturaMensalmente() {
        Pedido pedido = servico.criar(novoPedido("CONTRATO"));

        assertEquals("FATURA_MENSAL", faturamento.ultimaSolicitacao.meioPagamento());
        assertEquals(30, faturamento.ultimaSolicitacao.prazoDias());
        assertEquals(0, new BigDecimal("1000.00").compareTo(faturamento.ultimaSolicitacao.valor()));
        assertEquals(StatusPedido.FATURADO, pedido.getStatus());
    }
}
