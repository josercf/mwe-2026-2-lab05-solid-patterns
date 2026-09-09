using Faturamento.Api.Dominio;
using Microsoft.EntityFrameworkCore;

namespace Faturamento.Api.Infraestrutura;

/// <summary>
/// Persistência de faturas em PostgreSQL, via EF Core.
/// </summary>
/// <remarks>
/// <para><strong>GABARITO, TODO-4.</strong> O corpo dos métodos não mudou: só
/// a declaração da classe e a palavra <c>virtual</c>, que sumiu. Ela existia no
/// esqueleto para o dublê de teste conseguir herdar desta classe. Com a
/// interface no lugar, o teste implementa o contrato e ninguém precisa abrir
/// método de produção para sobrescrita por causa de teste.</para>
/// </remarks>
public class EfFaturaRepository : IFaturaRepository
{
    private readonly FaturamentoDbContext _banco;

    public EfFaturaRepository(FaturamentoDbContext banco)
    {
        _banco = banco;
    }

    public Fatura Salvar(Fatura fatura)
    {
        _banco.Faturas.Add(fatura);
        _banco.SaveChanges();
        return fatura;
    }

    public Fatura? PorPedido(string pedidoId)
    {
        return _banco.Faturas.AsNoTracking().FirstOrDefault(f => f.PedidoId == pedidoId);
    }

    public IReadOnlyList<Fatura> Todas()
    {
        return _banco.Faturas.AsNoTracking().OrderBy(f => f.EmitidaEm).ToList();
    }
}
