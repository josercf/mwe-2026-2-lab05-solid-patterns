package br.com.fiap.logitech.pedidos.faturamento;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GABARITO, TODO-2 (Factory Method): quem decide qual conector de faturamento
 * usar.
 *
 * <p>O que faz esta fábrica valer a pena não é ter tirado o {@code if} de
 * lugar, é o construtor. Ele recebe {@code List<ConectorFaturamento>}, e o
 * Spring entrega nessa lista todo bean que implemente a interface. Ninguém
 * registra conector em lugar nenhum: existir e ser {@code @Component} basta.
 * Por isso o passo seguinte, o TODO-3, não precisa editar este arquivo
 * também.</p>
 *
 * <p>Se a fábrica tivesse sido escrita com a cadeia de {@code if} apenas
 * mudada de endereço, o {@code switch} continuaria existindo, só que aqui, e o
 * Aberto/Fechado do TODO-3 falharia: acrescentar CONTRATO exigiria editar a
 * fábrica. Mover código não é inverter dependência.</p>
 *
 * <p>O índice é montado uma vez, na construção, e não a cada pedido: a fábrica
 * é um bean singleton do Spring e a lista de conectores não muda em tempo de
 * execução.</p>
 */
@Component
public class ConectorFaturamentoFactory {

    private final Map<String, ConectorFaturamento> porTipoCliente;

    public ConectorFaturamentoFactory(List<ConectorFaturamento> conectores) {
        this.porTipoCliente = conectores.stream()
                .collect(Collectors.toMap(ConectorFaturamento::tipoClienteAtendido, c -> c));
    }

    /**
     * Devolve o conector que atende este tipo de cliente.
     *
     * @throws ConectorNaoEncontradoException quando nenhum conector se declara
     *         responsável pelo tipo pedido. Repare que a exceção é lançada aqui,
     *         e não em quem chama: quem procura é quem sabe que não achou.
     */
    public ConectorFaturamento para(String tipoCliente) {
        ConectorFaturamento conector = porTipoCliente.get(tipoCliente);
        if (conector == null) {
            throw new ConectorNaoEncontradoException(tipoCliente);
        }
        return conector;
    }
}
