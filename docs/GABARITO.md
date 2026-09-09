# Gabarito comentado, Aula 05

Branch `gabarito` do laboratório de POO, SOLID e Design Patterns. Contém as
seis lacunas resolvidas, os testes que elas pedem e o `docs/EVIDENCIAS.md`
preenchido com valores medidos de verdade.

`python3 verificar.py` nesta branch fecha **11 de 11** critérios.

## Como usar

A solução está no próprio código, comentada onde a decisão foi tomada. Todo
comentário de gabarito começa com a palavra `GABARITO`, então dá para ler as
seis lacunas em sequência com:

```bash
git diff main..gabarito                       # o diff inteiro
grep -rn "GABARITO" pedidos/src faturamento/src faturamento/tests
```

O diff contra a `main` é a leitura mais útil, porque o que interessa em várias
lacunas é o que **saiu** do arquivo, não o que entrou.

---

## TODO-1, inverter a dependência do repositório (Java)

**O defeito.** `PedidoService` declarava o campo como `JpaPedidoRepository`.
Regra de negócio conhecendo o ORM que a persiste: trocar de banco, de ORM, ou
testar sem banco, custava mexer na regra de negócio.

**A correção.** Uma interface `PedidoRepository` em `dominio/`, implementada
por `JpaPedidoRepository`, e o serviço passando a depender dela.

**O que observar.** Onde a interface mora é a decisão, e não o fato de existir
uma interface. Ela está em `dominio/`, junto de quem precisa dela, e não em
`infra/`, junto de quem a cumpre. Uma interface criada dentro de `infra/`
deixaria o domínio dependendo da infraestrutura através de um tipo mais bonito,
e a inversão não teria acontecido.

O corpo dos métodos de `JpaPedidoRepository` não mudou uma linha. Inverter
dependência é mudar quem aponta para quem, não reescrever a persistência.

**Onde o ganho aparece.** Em `PedidoServiceTest`. O dublê antes era obrigado a
herdar do repositório JPA e chamar `super(null)`, passando um `PedidoJpa` nulo
que ninguém usava. Agora implementa a interface, e o arquivo de teste não
importa mais nada de `infra`: ele deixou de saber que existe um ORM no projeto.
Esse `super(null)` era o acoplamento cobrando o preço dele.

---

## TODO-2, Factory Method para os conectores (Java)

**O defeito.** O método privado `escolherConector` era uma cadeia de `if` que
crescia a cada contrato novo do comercial, dentro da classe que não deveria ter
motivo nenhum para mudar por causa disso.

**A correção.** `ConectorFaturamentoFactory` recebe `List<ConectorFaturamento>`
no construtor, monta um índice por `tipoClienteAtendido()` e devolve o conector
em `para(tipoCliente)`.

**O que observar.** O construtor é o que faz a fábrica valer a pena. O Spring
entrega nessa lista todo bean que implemente a interface, então nenhum conector
precisa ser registrado em lugar nenhum: existir e ser `@Component` basta.

Uma fábrica escrita com a mesma cadeia de `if`, apenas mudada de endereço,
passaria neste critério e reprovaria no seguinte, porque acrescentar o cliente
CONTRATO exigiria editar a fábrica. Mover código para outro arquivo não inverte
dependência.

O índice é montado uma vez, na construção, e não a cada pedido: a fábrica é um
bean singleton e a lista de conectores não muda em tempo de execução.

A `ConectorNaoEncontradoException` passou a ser lançada dentro da fábrica. Quem
procura é quem sabe que não achou.

---

## TODO-3, o cliente CONTRATO sem tocar no serviço (Java)

**A correção.** Um arquivo novo, `ConectorFaturaMensal`, com `@Component`,
`tipoClienteAtendido()` devolvendo `CONTRATO` e `montar()` produzindo
`FATURA_MENSAL` com valor cheio e 30 dias de prazo.

**O que observar.** Este arquivo é a resposta inteira da lacuna. Não houve
alteração em `PedidoService`, nem em `ConectorFaturamentoFactory`, nem registro
em arquivo de configuração. O sistema passou a atender um tipo de cliente novo
porque uma classe nova apareceu.

O critério de aceitação do passo é um diff vazio, e não um teste verde:

```bash
git diff main..gabarito -- pedidos/src/main/java/br/com/fiap/logitech/pedidos/aplicacao/PedidoService.java
```

Esse diff mostra alterações, mas todas vindas dos TODOs 1 e 2. Nenhuma linha
foi tocada por causa do CONTRATO, e é isso que o Aberto/Fechado promete. Um
teste verde diz que o tipo novo funciona; só o diff diz que ele entrou sem
custo.

Sem desconto, ao contrário do cliente OURO: quem tem contrato mensal já
negociou preço no contrato, e o desconto por operação seria dobrado.

**A medida resumida das três lacunas Java** está na lista de `import` de
`PedidoService`. Sumiram `infra.JpaPedidoRepository`, `ConectorBoleto`,
`ConectorCartaoCorporativo` e `ConectorNaoEncontradoException`. Restou
`ConectorFaturamento`, a abstração, e nenhuma das implementações. Import é
dependência declarada.

---

## TODO-4, Repository e injeção por construtor (C#)

**O defeito.** O mesmo do TODO-1, em outra linguagem: `FaturaService` dependia
de `EfFaturaRepository`.

**A correção.** `IFaturaRepository` em `Dominio/`, implementada por
`EfFaturaRepository`, recebida por construtor em `FaturaService` e registrada
em `Program.cs`:

```csharp
builder.Services.AddScoped<IFaturaRepository, EfFaturaRepository>();
```

**O que observar.** A prova mais direta de que a inversão aconteceu é o
desaparecimento do `using Faturamento.Api.Infraestrutura;` no topo de
`FaturaService.cs`. A camada de negócio não compila mais nenhuma referência à
infraestrutura.

O `virtual` dos métodos do repositório também sumiu. Ele existia para o dublê
de teste conseguir herdar da classe concreta. Com a interface no lugar, o teste
implementa o contrato, e nenhum método de produção precisa ficar aberto para
sobrescrita por causa de teste.

`Program.cs` mudou em uma linha. É ali, e só ali, que o sistema decide qual
implementação atende a abstração.

Que o defeito seja idêntico em Java e em C# é o argumento da aula: violação de
DIP não é característica de linguagem, é de projeto, e a correção também não
muda de forma quando muda a linguagem.

---

## TODO-6, o teste que faz a corrida aparecer (C#)

Esta lacuna vem **antes** do TODO-5, e a ordem é o exercício.

**A correção.** Um teste que reinicia o numerador, dispara 100 chamadas a
`Proximo()` com `Task.Run` mais `Task.WhenAll`, conta as repetições com
`Distinct()` e exige zero duplicatas.

**O que observar.** `Task.Run` com `Task.WhenAll`, e não `Parallel.For`: as
tarefas são despachadas todas para o pool antes de qualquer espera, o que
concentra as chamadas na mesma janela de tempo e faz a corrida aparecer também
em máquina de poucos núcleos.

A asserção é sobre zero duplicatas, e não sobre uma quantidade esperada delas,
porque a quantidade muda a cada execução.

**A medição.** Três execuções seguidas deste teste com o Singleton ainda
quebrado, no ambiente descrito em `EVIDENCIAS.md`, deram **6, 8 e 4** notas
duplicadas. A variação entre execuções é a assinatura de uma corrida: o
resultado depende de quando cada thread foi escalonada, não da entrada.

**Por que a ordem importa.** Escrito depois da correção, este mesmo teste
passaria de primeira e não teria provado nada: mostraria que o código atual
funciona, sem nunca mostrar que o anterior estava quebrado. Um teste de
concorrência que nunca falhou não tem evidência de que consegue falhar.

---

## TODO-5, o Singleton thread-safe (C#)

**Os dois defeitos**, que precisavam de correção separada:

| Defeito | Por que falhava | Correção aplicada |
|---|---|---|
| Criação da instância com `??=` | Ler, testar e atribuir em passos separados. Duas threads na primeira chamada criavam dois numeradores, cada um com seu contador | `Lazy<NumeradorNotaFiscal>`, cujo modo padrão garante que a fábrica roda uma vez só |
| Incremento em três passos | Entre a leitura de `_ultimo` e a gravação, outra thread lia o mesmo valor e produzia o mesmo número | `Interlocked.Increment`, que torna ler, somar e gravar uma operação atômica |

**O que observar.** Corrigir só um dos dois não resolveria. Instância única com
incremento desprotegido continua duplicando número, e incremento atômico em
duas instâncias diferentes duplica igual, porque são dois contadores.

O `Thread.Yield()` do esqueleto sumiu junto com o incremento em três passos.
Ele nunca foi o defeito: apenas alargava uma janela que já existia, para a
corrida aparecer em 100 execuções em vez de uma vez a cada tantos milhares.

`IniciarEm` usa `Interlocked.Exchange` pelo mesmo motivo do incremento: o campo
passou a ser tocado por várias threads, e toda escrita nele precisa ser atômica
para o contador continuar coerente.

Um `lock` em torno do bloco de leitura e escrita seria igualmente aceito.
`Interlocked.Increment` foi preferido por ser mais barato para o caso de um
único contador, já que não envolve entrar e sair de uma região crítica.

A chamada em `FaturaService` não mudou. A correção da corrida ficou inteira
dentro do numerador, e nenhum chamador precisou saber que existia um problema.

---

## O que a máquina não verifica

O `verificar.py` lê a estrutura do código: a abstração existe, a implementação
a declara, a regra de negócio não cita mais o tipo concreto, o teste tem a
forma pedida. Ele não julga se os nomes ficaram bons nem se a modelagem é a
melhor possível, e não consegue voltar no tempo para conferir se o teste do
TODO-6 foi mesmo escrito antes da correção do TODO-5.

Um `NOTAS_DUPLICADAS_ANTES` maior que zero é a única evidência de que a corrida
foi observada em vez de apenas afirmada.
