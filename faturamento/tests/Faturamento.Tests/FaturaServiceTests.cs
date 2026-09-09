using System.Collections.Concurrent;
using System.Text.RegularExpressions;
using Faturamento.Api.Aplicacao;
using Faturamento.Api.Dominio;
using Xunit;

namespace Faturamento.Tests;

/// <summary>
/// Testes de unidade da regra de faturamento: sem PostgreSQL, sem servidor web.
/// Já vêm verdes e precisam continuar verdes depois dos TODOs 4, 5 e 6.
/// </summary>
public class FaturaServiceTests
{
    /// <summary>
    /// Dublê de teste do repositório.
    /// </summary>
    /// <remarks>
    /// <para><strong>GABARITO, TODO-4.</strong> Compare com o esqueleto:
    /// sumiram o <c>: EfFaturaRepository</c>, o <c>base(null!)</c>, os três
    /// <c>override</c> e o <c>using</c> de <c>Infraestrutura</c>. O
    /// <c>base(null!)</c> era o acoplamento cobrando o preço no teste: para
    /// exercitar regra de negócio, o teste precisava construir um repositório
    /// de EF Core com um <c>DbContext</c> nulo que ninguém usava.</para>
    ///
    /// <para>O dicionário é concorrente de propósito: o defeito que o TODO-6 vai
    /// expor está no numerador de notas, e não no armazenamento do dublê.</para>
    /// </remarks>
    private class FaturaRepositorioEmMemoria : IFaturaRepository
    {
        private readonly ConcurrentDictionary<string, Fatura> _dados = new();

        public Fatura Salvar(Fatura fatura)
        {
            _dados[fatura.PedidoId] = fatura;
            return fatura;
        }

        public Fatura? PorPedido(string pedidoId)
        {
            return _dados.TryGetValue(pedidoId, out Fatura? fatura) ? fatura : null;
        }

        public IReadOnlyList<Fatura> Todas()
        {
            return _dados.Values.OrderBy(f => f.EmitidaEm).ToList();
        }
    }

    private static SolicitacaoFatura Solicitacao(string pedidoId = "pedido-1",
                                                 string meio = "BOLETO",
                                                 int prazo = 3)
    {
        return new SolicitacaoFatura(pedidoId, "Distribuidora Sul", 1000.00m, meio, prazo);
    }

    private static FaturaService ServicoNovo()
    {
        NumeradorNotaFiscal.Instancia.ReiniciarParaTeste();
        // GABARITO, TODO-4: esta linha é literalmente a mesma do esqueleto. O
        // que mudou foi o tipo que FaturaService declara receber, e é por isso
        // que o dublê pôde deixar de herdar do EF Core.
        return new FaturaService(new FaturaRepositorioEmMemoria());
    }

    [Fact]
    public void EmitirGeraNotaFiscalNoFormatoDoContrato()
    {
        FaturaService servico = ServicoNovo();

        Fatura fatura = servico.Emitir(Solicitacao());

        Assert.Matches(new Regex(@"^NF-\d{6}$"), fatura.NumeroNotaFiscal);
        Assert.Equal("pedido-1", fatura.PedidoId);
        Assert.Equal(1000.00m, fatura.Valor);
    }

    [Fact]
    public void EmitirDuasVezesParaOMesmoPedidoNaoGeraDuasNotas()
    {
        FaturaService servico = ServicoNovo();

        Fatura primeira = servico.Emitir(Solicitacao());
        Fatura segunda = servico.Emitir(Solicitacao());

        Assert.Equal(primeira.NumeroNotaFiscal, segunda.NumeroNotaFiscal);
        Assert.Single(servico.Todas());
    }

    [Fact]
    public void PrazoDoClienteViraVencimentoDaFatura()
    {
        FaturaService servico = ServicoNovo();

        Fatura fatura = servico.Emitir(Solicitacao(prazo: 30, meio: "FATURA_MENSAL"));

        Assert.Equal("FATURA_MENSAL", fatura.MeioPagamento);
        Assert.Equal(fatura.EmitidaEm.AddDays(30), fatura.Vencimento());
    }

    [Fact]
    public void ValorNaoPositivoEhRecusadoPeloDominio()
    {
        FaturaService servico = ServicoNovo();

        Assert.Throws<ArgumentException>(() =>
            servico.Emitir(new SolicitacaoFatura("pedido-2", "Distribuidora Sul", 0m, "BOLETO", 3)));
    }
}
