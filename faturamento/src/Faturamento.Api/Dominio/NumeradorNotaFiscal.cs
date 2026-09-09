namespace Faturamento.Api.Dominio;

/// <summary>
/// GABARITO, TODO-5 (Singleton thread-safe): gerador do número sequencial da
/// nota fiscal.
/// </summary>
/// <remarks>
/// <para>Número de nota fiscal é sequencial e único por natureza: dois pedidos
/// com a mesma nota é problema fiscal, não bug de tela. Por isso o contador
/// mora em uma instância única compartilhada por todas as requisições, e é
/// exatamente aí que mora o perigo.</para>
///
/// <para>O esqueleto tinha dois defeitos, e os dois precisavam de correção
/// separada:</para>
///
/// <list type="number">
///   <item><strong>A criação da instância.</strong> O <c>??=</c> do esqueleto
///         é ler, testar e atribuir em passos separados: duas threads chegando
///         juntas na primeira chamada criavam dois numeradores, cada um com o
///         seu contador. O <c>Lazy&lt;T&gt;</c> resolve isso por construção. O
///         modo padrão é <c>ExecutionAndPublication</c>, que garante que a
///         fábrica roda uma vez só, mesmo com dezenas de threads chegando ao
///         mesmo tempo. Um campo <c>static readonly</c> inicializado na
///         declaração também serviria: o CLR garante a inicialização de tipo
///         uma única vez.</item>
///   <item><strong>O incremento do contador.</strong>
///         <c>Interlocked.Increment</c> faz ler, somar e gravar virarem uma
///         operação atômica no processador, indivisível por definição. Não há
///         mais janela entre a leitura e a gravação porque não há mais leitura
///         e gravação separadas.</item>
/// </list>
///
/// <para>Corrigir só um dos dois não resolveria. Instância única com
/// incremento desprotegido continua duplicando número; incremento atômico em
/// duas instâncias diferentes duplica igual, porque são dois contadores.</para>
///
/// <para>O <c>Thread.Yield()</c> do esqueleto sumiu junto com o incremento em
/// três passos. Ele nunca foi o defeito: apenas alargava uma janela que já
/// existia, para a corrida aparecer em 100 execuções em vez de uma vez a cada
/// tantos milhares. Com <c>Interlocked</c> não há janela para alargar.</para>
///
/// <para>Alternativa igualmente aceita: um <c>lock</c> em torno do bloco de
/// leitura e escrita. <c>Interlocked.Increment</c> foi preferido por ser mais
/// barato para o caso de um único contador, já que não envolve o custo de
/// entrar e sair de uma região crítica.</para>
///
/// <para>O formato do número devolvido não mudou: <c>NF-000001</c>, com seis
/// dígitos, é lido pelo serviço de Pedidos e pelo verificador.</para>
/// </remarks>
public sealed class NumeradorNotaFiscal
{
    // GABARITO, TODO-5, defeito 1: instância criada uma única vez, com a
    // sincronização garantida pelo próprio Lazy<T>.
    private static readonly Lazy<NumeradorNotaFiscal> _instancia =
        new(() => new NumeradorNotaFiscal());

    private int _ultimo;

    private NumeradorNotaFiscal()
    {
    }

    public static NumeradorNotaFiscal Instancia => _instancia.Value;

    /// <summary>Devolve o próximo número de nota fiscal, no formato NF-000001.</summary>
    public string Proximo()
    {
        // GABARITO, TODO-5, defeito 2: uma operação atômica no lugar dos três
        // passos. Interlocked.Increment devolve o valor já incrementado, então
        // nenhuma outra thread pode ter usado este mesmo número.
        int atual = Interlocked.Increment(ref _ultimo);

        return $"NF-{atual:D6}";
    }

    /// <summary>
    /// Continua a numeração a partir do último número já gravado no banco.
    /// </summary>
    /// <remarks>
    /// Chamado uma única vez, na subida do serviço. Sem isto, cada reinício
    /// recomeçaria em NF-000001 e esbarraria no índice único da tabela de
    /// faturas: estado em memória não sobrevive ao processo, e o Singleton
    /// precisa ser reconstruído a partir de onde a verdade mora, que é o banco.
    ///
    /// <para>A escrita usa Interlocked.Exchange pelo mesmo motivo do
    /// incremento: o campo passou a ser tocado por várias threads, e toda
    /// escrita nele precisa ser atômica para o contador continuar coerente.</para>
    /// </remarks>
    public void IniciarEm(int ultimoEmitido)
    {
        Interlocked.Exchange(ref _ultimo, ultimoEmitido);
    }

    /// <summary>
    /// Zera o contador. Existe só para os testes conseguirem partir sempre do
    /// mesmo ponto: nenhum código de produção chama este método.
    /// </summary>
    public void ReiniciarParaTeste()
    {
        IniciarEm(0);
    }
}
