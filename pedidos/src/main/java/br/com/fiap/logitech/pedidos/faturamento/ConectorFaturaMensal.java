package br.com.fiap.logitech.pedidos.faturamento;

import br.com.fiap.logitech.pedidos.dominio.Pedido;
import br.com.fiap.logitech.pedidos.dominio.SolicitacaoFatura;
import org.springframework.stereotype.Component;

/**
 * GABARITO, TODO-3 (Aberto/Fechado). Cliente CONTRATO: rede de distribuidoras
 * com contrato mensal, faturada por FATURA_MENSAL, valor cheio, 30 dias de
 * prazo.
 *
 * <p>Este arquivo é a resposta inteira do TODO-3. Não houve mais nada: nenhuma
 * linha de {@code PedidoService}, nenhuma linha de
 * {@code ConectorFaturamentoFactory}, nenhum registro em arquivo de
 * configuração. O sistema passou a atender um tipo de cliente novo porque uma
 * classe nova apareceu, e é exatamente esse o enunciado do princípio: aberto
 * para extensão, fechado para modificação.</p>
 *
 * <p>As duas coisas que fazem isso funcionar são o {@code @Component}, que põe
 * a classe na lista que o Spring injeta na fábrica, e o
 * {@code tipoClienteAtendido()}, que é como a classe se declara responsável
 * por CONTRATO sem que ninguém lhe pergunte.</p>
 *
 * <p>Sem desconto, ao contrário do cliente OURO: quem tem contrato mensal já
 * negociou preço no contrato, e o desconto por operação seria dobrado.</p>
 */
@Component
public class ConectorFaturaMensal implements ConectorFaturamento {

    public static final String TIPO_CLIENTE = "CONTRATO";

    private static final int PRAZO_DIAS = 30;

    @Override
    public String tipoClienteAtendido() {
        return TIPO_CLIENTE;
    }

    @Override
    public SolicitacaoFatura montar(Pedido pedido) {
        return new SolicitacaoFatura(
                pedido.getId(),
                pedido.getCliente(),
                pedido.getValor(),
                "FATURA_MENSAL",
                PRAZO_DIAS);
    }
}
