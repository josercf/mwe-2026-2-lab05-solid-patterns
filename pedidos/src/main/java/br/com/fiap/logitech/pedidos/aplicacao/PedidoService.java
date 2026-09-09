package br.com.fiap.logitech.pedidos.aplicacao;

import br.com.fiap.logitech.pedidos.dominio.NovoPedido;
import br.com.fiap.logitech.pedidos.dominio.Pedido;
import br.com.fiap.logitech.pedidos.dominio.PedidoRepository;
import br.com.fiap.logitech.pedidos.dominio.SolicitacaoFatura;
import br.com.fiap.logitech.pedidos.faturamento.ClienteFaturamento;
import br.com.fiap.logitech.pedidos.faturamento.ConectorFaturamento;
import br.com.fiap.logitech.pedidos.faturamento.ConectorFaturamentoFactory;
import br.com.fiap.logitech.pedidos.faturamento.FaturamentoIndisponivelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regra de negócio do contexto de Pedidos.
 *
 * <p><strong>GABARITO das lacunas 1, 2 e 3.</strong> Compare a lista de
 * {@code import} deste arquivo com a do esqueleto. Ela é o resumo mais honesto
 * do que a aula fez, porque import é dependência declarada:</p>
 *
 * <ul>
 *   <li>sumiu {@code infra.JpaPedidoRepository}: a regra de negócio não conhece
 *       mais o ORM que a persiste (TODO-1);</li>
 *   <li>sumiram {@code ConectorBoleto} e {@code ConectorCartaoCorporativo}:
 *       a decisão de qual conector usar mudou de lugar (TODO-2);</li>
 *   <li>sumiu {@code ConectorNaoEncontradoException}: quem sabe que um tipo de
 *       cliente não tem conector é a fábrica, que é quem procura;</li>
 *   <li>restou {@code ConectorFaturamento}, a abstração, e nenhuma das suas
 *       implementações.</li>
 * </ul>
 *
 * <p>O ganho do TODO-3 se mede aqui: o conector do cliente CONTRATO foi
 * acrescentado ao projeto sem que uma única linha deste arquivo mudasse por
 * causa dele. É isso que o Aberto/Fechado promete, e é por isso que o critério
 * de aceitação do passo 4 é um {@code git diff} vazio, e não um teste verde.</p>
 */
@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    // GABARITO, TODO-1: o campo é da abstração declarada no domínio. Quem chega
    // aqui em tempo de execução continua sendo o JpaPedidoRepository, injetado
    // pelo Spring; a diferença é que esta classe não sabe disso, e o teste de
    // unidade pode passar outra coisa.
    private final PedidoRepository repositorio;
    private final ClienteFaturamento clienteFaturamento;

    // GABARITO, TODO-2: a fábrica entra por construtor, como toda dependência.
    private final ConectorFaturamentoFactory conectores;

    public PedidoService(PedidoRepository repositorio,
                         ClienteFaturamento clienteFaturamento,
                         ConectorFaturamentoFactory conectores) {
        this.repositorio = repositorio;
        this.clienteFaturamento = clienteFaturamento;
        this.conectores = conectores;
    }

    /**
     * Abre um pedido e tenta emitir a fatura correspondente.
     *
     * <p>Se o serviço de Faturamento estiver fora do ar, o pedido é gravado
     * assim mesmo, com status {@code AGUARDANDO_FATURAMENTO}: a
     * indisponibilidade de um Bounded Context não pode derrubar o outro.</p>
     */
    @Transactional
    public Pedido criar(NovoPedido novo) {
        Pedido pedido = new Pedido(novo.cliente(), novo.tipoCliente(), novo.origem(),
                novo.destino(), novo.enderecoEntrega(), novo.pesoKg(), novo.valor());

        // GABARITO, TODO-2: uma linha, e ela não sabe quantos tipos de cliente
        // existem. A cadeia de if virou uma pergunta para quem tem a resposta.
        ConectorFaturamento conector = conectores.para(pedido.getTipoCliente());
        SolicitacaoFatura solicitacao = conector.montar(pedido);

        try {
            String numeroNotaFiscal = clienteFaturamento.emitir(solicitacao);
            pedido.faturar(numeroNotaFiscal);
        } catch (FaturamentoIndisponivelException erro) {
            log.warn("faturamento indisponível para o pedido {}: {}", pedido.getId(), erro.getMessage());
            pedido.aguardarFaturamento();
        }

        return repositorio.salvar(pedido);
    }

    @Transactional(readOnly = true)
    public List<Pedido> listar() {
        return repositorio.todos();
    }

    @Transactional(readOnly = true)
    public Pedido porId(String id) {
        return repositorio.porId(id).orElseThrow(() -> new PedidoNaoEncontradoException(id));
    }

    /** Rota consumida pelo agente de IA da Aula 08. */
    @Transactional
    public Pedido alterarEndereco(String id, String novoEndereco) {
        Pedido pedido = porId(id);
        pedido.alterarEndereco(novoEndereco);
        return repositorio.salvar(pedido);
    }
}
