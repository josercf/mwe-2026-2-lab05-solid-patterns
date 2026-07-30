# Lab 05 - POO, SOLID e Design Patterns em Java e C#

**Microservice and Web Engineering & IT Services**
Prof. José Romualdo da Costa Filho | FIAP Sistemas de Informacao | 1o semestre de 2026-2

> Aula 05 - POO, Principios SOLID e Design Patterns | 01/09/2026

---

## Missao

Implementar o calculo de frete da LogiTech com Strategy e injecao de dependencia, em Java (Spring Boot) e C# (.NET), aplicando SOLID.

Todos os laboratorios da disciplina evoluem o mesmo case: a **LogiTech Enterprise AI Platform**, uma
transportadora ficticia. O que voce entrega aqui e reaproveitado nas aulas
seguintes e desemboca na Global Solution.

---

## Como comecar

### Opcao 1: GitHub Codespaces (recomendado)

Clique em **Code > Codespaces > Create codespace on main**. O ambiente sobe
pronto, com todas as dependencias e o cliente de IA ja configurado. Nada para
instalar na sua maquina.

### Opcao 2: Local com Dev Container

Requer Docker e a extensao **Dev Containers** no VS Code.

```bash
git clone https://github.com/josercf/mwe-2026-2-lab05-solid-patterns.git
cd mwe-2026-2-lab05-solid-patterns
code .
# VS Code vai sugerir: "Reopen in Container"
```

Localmente, exporte o token para habilitar o assistente de IA:

```bash
export GITHUB_TOKEN=$(gh auth token)
```

---

## Assistente de IA incluso

O laboratorio traz um cliente minimo que fala com **GitHub Models** usando o
token que o Codespaces ja injeta. Voce nao precisa criar conta, gerar chave nem
cadastrar cartao.

```bash
python ai/ask.py "explique a diferenca entre TCP e UDP em duas frases"

# escolher outro modelo pequeno
MODEL=microsoft/phi-4-mini-instruct python ai/ask.py "..."

# usar um arquivo como prompt
cat prompts/prd.md | python ai/ask.py
```

Se o GitHub Models estiver indisponivel ou a cota da sua conta tiver acabado, o
script cai automaticamente para o **Ollama que ja vem instalado neste
devcontainer**, com o modelo `qwen2.5:1.5b` baixado na criacao do ambiente.

```bash
ollama list                      # o modelo ja deve aparecer aqui
OLLAMA_MODEL=qwen2.5:1.5b python ai/ask.py "..."   # forcar o modelo local
ollama pull qwen2.5:3b           # modelo maior, se a maquina aguentar
```

> A cota gratuita do GitHub Models e limitada por dia. Se a turma inteira
> disparar requisicoes ao mesmo tempo, o fallback local resolve sem depender
> de rede.

---

## Instalando uma skill da nossa biblioteca

Uma **skill** e um arquivo `SKILL.md` que ensina ao assistente de IA um
procedimento: como escrever um PRD, como padronizar commits, como estruturar
um SDD. Em vez de repetir o mesmo prompt longo toda vez, voce instala a skill
uma vez e passa a invoca-la.

Nossa biblioteca compartilhada fica em
<https://github.com/josercf/skill-library>:

```
skills/
  prd/SKILL.md               como escrever um PRD
  sdd/SKILL.md               Spec Driven Development
  semantic-commits/SKILL.md  Conventional Commits e Git Hooks
  fiap-course-design/SKILL.md
```

### Instalar no seu ambiente

```bash
# 1. Baixe a biblioteca
git clone https://github.com/josercf/skill-library.git /tmp/skill-library

# 2. Copie a skill desejada para o diretorio de skills do projeto
mkdir -p .claude/skills
cp -r /tmp/skill-library/skills/prd .claude/skills/

# 3. Confira
ls .claude/skills/prd/SKILL.md
```

Assistentes que leem `.claude/skills/` (como o Claude Code) passam a
enxergar a skill automaticamente. Para usar com o `ai/ask.py`, basta anexar
o conteudo da skill ao prompt:

```bash
python ai/ask.py "$(cat .claude/skills/prd/SKILL.md)

Agora escreva o PRD do servico de telemetria da LogiTech."
```

---

## Entregaveis

- `java/`
- `csharp/`

Portas expostas pelo ambiente: 8080

---

## Regras de entrega

1. Trabalho em **dupla**. Um repositorio por dupla, gerado a partir deste
   (use **Fork** ou **Use this template**).
2. Commits seguindo [Conventional Commits](https://www.conventionalcommits.org/pt-br/v1.0.0/):

   ```bash
   git commit -m "feat(telemetria): adiciona listener UDP na porta 8081"
   ```

3. Submeta a URL do repositorio no formulario da disciplina ate o fim da aula.

---

## Estrutura

```
mwe-2026-2-lab05-solid-patterns/
├── .devcontainer/
│   ├── devcontainer.json   # ambiente reproduzivel (Codespaces e local)
│   └── post-create.sh      # instalacao de dependencias
├── ai/
│   └── ask.py              # cliente de IA (GitHub Models -> Ollama)
├── docs/                   # artefatos de especificacao
└── README.md
```

---

## Material da aula

Este laboratorio faz parte do acervo da disciplina:

| | |
|---|---|
| Slides desta aula | <https://josercf.github.io/FIAP-2026-2-3SI/aulas-1sem/aulas/aula05.html> |
| Portal da disciplina | <https://josercf.github.io/FIAP-2026-2-3SI/> |
| Repositorio do acervo | <https://github.com/josercf/FIAP-2026-2-3SI> |
| Biblioteca de skills | <https://github.com/josercf/skill-library> |
