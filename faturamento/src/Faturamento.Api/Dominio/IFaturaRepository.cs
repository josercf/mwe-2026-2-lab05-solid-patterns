namespace Faturamento.Api.Dominio;

/// <summary>
/// GABARITO, TODO-4 (DIP em C#). Porta de saída da persistência de faturas.
/// </summary>
/// <remarks>
/// <para>Mesma decisão do <c>PedidoRepository</c> em Java, e de propósito: a
/// interface mora em <c>Dominio/</c>, junto de quem precisa dela, e não em
/// <c>Infraestrutura/</c>, junto de quem a cumpre. Violação de DIP não é
/// característica de linguagem, é de projeto, e a correção também não muda de
/// forma quando muda a linguagem.</para>
///
/// <para>Repare no que a interface não menciona: EF Core, <c>DbContext</c>,
/// PostgreSQL. Se o nome de qualquer um deles aparecesse aqui, a abstração
/// estaria vazando a implementação e não teria servido para nada.</para>
///
/// <para>Sem o <c>virtual</c> que os métodos do repositório precisavam ter no
/// esqueleto: com uma interface, o dublê de teste implementa em vez de herdar,
/// e nenhum método precisa ser aberto para sobrescrita só por causa do
/// teste.</para>
/// </remarks>
public interface IFaturaRepository
{
    Fatura Salvar(Fatura fatura);

    Fatura? PorPedido(string pedidoId);

    IReadOnlyList<Fatura> Todas();
}
