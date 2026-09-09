using Faturamento.Api.Dominio;

namespace Faturamento.Api.Aplicacao;

/// <summary>
/// Regra de negócio do Bounded Context de Faturamento.
/// </summary>
/// <remarks>
/// <para><strong>GABARITO, TODO-4.</strong> Sumiu o
/// <c>using Faturamento.Api.Infraestrutura;</c> do topo do arquivo, e é essa a
/// prova mais direta de que a inversão aconteceu: a camada de negócio não
/// compila mais nenhuma referência à infraestrutura. Quem injeta o
/// <c>EfFaturaRepository</c> em produção é o contêiner de injeção de
/// dependência, configurado no <c>Program.cs</c>.</para>
///
/// <para>Emitir fatura é idempotente por pedido: o serviço de Pedidos pode
/// repetir a chamada depois de um timeout, e repetir não pode gerar duas
/// notas fiscais para o mesmo pedido.</para>
/// </remarks>
public class FaturaService
{
    private readonly IFaturaRepository _repositorio;

    public FaturaService(IFaturaRepository repositorio)
    {
        _repositorio = repositorio;
    }

    public Fatura Emitir(SolicitacaoFatura solicitacao)
    {
        Fatura? jaEmitida = _repositorio.PorPedido(solicitacao.PedidoId);
        if (jaEmitida is not null)
        {
            return jaEmitida;
        }

        // O número da nota vem do Singleton compartilhado por todas as
        // requisições, agora protegido (TODO-5). Esta linha não mudou: a
        // correção da corrida ficou inteira dentro do numerador, e nenhum
        // chamador precisou saber que existia um problema aqui.
        string numero = NumeradorNotaFiscal.Instancia.Proximo();

        var fatura = new Fatura(
            solicitacao.PedidoId,
            solicitacao.Cliente,
            solicitacao.Valor,
            solicitacao.MeioPagamento,
            solicitacao.PrazoDias,
            numero);

        return _repositorio.Salvar(fatura);
    }

    public Fatura? PorPedido(string pedidoId) => _repositorio.PorPedido(pedidoId);

    public IReadOnlyList<Fatura> Todas() => _repositorio.Todas();
}
