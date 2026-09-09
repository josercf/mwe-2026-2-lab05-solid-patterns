using System.Collections.Concurrent;
using Faturamento.Api.Dominio;
using Xunit;

namespace Faturamento.Tests;

/// <summary>
/// O Singleton que numera as notas fiscais da LogiTech.
/// </summary>
/// <remarks>
/// <para>O teste que já está aqui é sequencial: uma chamada depois da outra,
/// uma thread só. Ele passa mesmo com o Singleton quebrado, e é essa a
/// armadilha que esta lacuna quer mostrar. Defeito de concorrência não aparece
/// em teste sequencial: ele espera a produção.</para>
///
/// <para><strong>TODO-6:</strong> escreva o teste que dispara 100 emissões
/// concorrentes e prova que não existe número repetido. Faça isso
/// <strong>antes</strong> de corrigir o TODO-5: o teste precisa falhar de
/// verdade primeiro, com número de nota duplicado, e o número de duplicatas
/// vai para <c>docs/EVIDENCIAS.md</c>, no campo
/// <c>NOTAS_DUPLICADAS_ANTES</c>.</para>
///
/// <para>Forma esperada do teste:</para>
/// <list type="number">
///   <item>pegue <c>NumeradorNotaFiscal.Instancia</c> e chame
///         <c>ReiniciarParaTeste()</c>, para partir do zero;</item>
///   <item>guarde os números em uma coleção concorrente
///         (<c>ConcurrentBag&lt;string&gt;</c>);</item>
///   <item>dispare 100 chamadas concorrentes a <c>Proximo()</c>. Com
///         <c>Task.Run</c> mais <c>Task.WhenAll</c> a concorrência é maior e o
///         defeito aparece com mais facilidade do que com <c>Parallel.For</c>,
///         principalmente em máquina de poucos núcleos;</item>
///   <item>conte as duplicatas com
///         <c>numeros.Count - numeros.Distinct().Count()</c>;</item>
///   <item>termine com <c>Assert.Equal(0, duplicadas)</c>. A mensagem de falha do
///         xUnit mostra quantas foram: é dela que sai o valor de
///         <c>NOTAS_DUPLICADAS_ANTES</c>.</item>
/// </list>
///
/// <para>Depois de corrigir o TODO-5, rode de novo, confirme que passa e
/// registre <c>NOTAS_DUPLICADAS_DEPOIS: 0</c>.</para>
/// </remarks>
public class NumeradorNotaFiscalTests
{
    [Fact]
    public void ChamadasSequenciaisDevolvemNumerosCrescentes()
    {
        NumeradorNotaFiscal numerador = NumeradorNotaFiscal.Instancia;
        numerador.ReiniciarParaTeste();

        string primeiro = numerador.Proximo();
        string segundo = numerador.Proximo();

        Assert.Equal("NF-000001", primeiro);
        Assert.Equal("NF-000002", segundo);
    }

    [Fact]
    public void FormatoDoNumeroSegueOContratoDaPlataforma()
    {
        NumeradorNotaFiscal numerador = NumeradorNotaFiscal.Instancia;
        numerador.ReiniciarParaTeste();

        string numero = numerador.Proximo();

        Assert.StartsWith("NF-", numero);
        Assert.Equal(9, numero.Length);
    }

    /// <summary>
    /// GABARITO, TODO-6. O teste que faz a corrida aparecer.
    /// </summary>
    /// <remarks>
    /// <para>Este teste foi escrito <strong>antes</strong> da correção do
    /// TODO-5, e falhou: com o numerador quebrado, cem emissões concorrentes
    /// produziram dezenas de números repetidos. O valor medido está em
    /// <c>docs/EVIDENCIAS.md</c>, no campo <c>NOTAS_DUPLICADAS_ANTES</c>.</para>
    ///
    /// <para>A ordem importa e é o ponto pedagógico da lacuna. Escrito depois da
    /// correção, este mesmo teste passaria de primeira e não teria provado
    /// nada: ele mostraria que o código atual funciona, sem nunca mostrar que o
    /// anterior estava quebrado. Um teste de concorrência que nunca falhou não
    /// tem evidência de que consegue falhar.</para>
    ///
    /// <para><c>Task.Run</c> com <c>Task.WhenAll</c> em vez de
    /// <c>Parallel.For</c>: as tarefas são despachadas todas para o pool antes
    /// de qualquer espera, o que concentra as chamadas na mesma janela de tempo
    /// e faz a corrida aparecer também em máquina de poucos núcleos.</para>
    ///
    /// <para>O número de duplicatas varia a cada execução, e é isso que
    /// caracteriza uma corrida: o resultado depende de quando cada thread foi
    /// escalonada, não da entrada. Por isso a asserção é sobre zero duplicatas,
    /// e não sobre uma quantidade esperada delas.</para>
    /// </remarks>
    [Fact]
    public async Task CemEmissoesConcorrentesNaoGeramNumeroRepetido()
    {
        NumeradorNotaFiscal numerador = NumeradorNotaFiscal.Instancia;
        numerador.ReiniciarParaTeste();

        var numeros = new ConcurrentBag<string>();

        IEnumerable<Task> emissoes = Enumerable.Range(0, 100)
            .Select(_ => Task.Run(() => numeros.Add(numerador.Proximo())));

        await Task.WhenAll(emissoes);

        int duplicadas = numeros.Count - numeros.Distinct().Count();

        Assert.Equal(100, numeros.Count);
        Assert.Equal(0, duplicadas);
    }
}
