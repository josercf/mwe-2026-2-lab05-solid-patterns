#!/usr/bin/env bash
# Preparacao do ambiente do laboratorio. Roda uma vez, na criacao do container.
set -euo pipefail

echo "==> Configurando o laboratorio mwe-2026-2-lab05-solid-patterns"

# --- Dependencias da stack -------------------------------------------------
if [ -f pom.xml ]; then mvn -q -B dependency:go-offline || true; fi

# --- Ollama: SLM rodando dentro do proprio container -----------------------
# Backend de IA offline, usado quando o GitHub Models nao esta disponivel
# ou quando a cota da conta do aluno acabou.
if ! command -v ollama >/dev/null 2>&1; then
  echo "==> Instalando o Ollama"
  curl -fsSL https://ollama.com/install.sh | sh
fi

echo "==> Subindo o servidor Ollama"
(ollama serve >/tmp/ollama.log 2>&1 &)

# Espera o servidor aceitar conexao (ate 30s)
for _ in $(seq 1 30); do
  if curl -sf http://localhost:11434/api/tags >/dev/null 2>&1; then break; fi
  sleep 1
done

echo "==> Baixando o modelo qwen2.5:1.5b (uso unico, fica em cache)"
ollama pull qwen2.5:1.5b || echo "    AVISO: falha ao baixar o modelo. Rode 'ollama pull qwen2.5:1.5b' manualmente."

# --- Verificacao do backend de IA -----------------------------------------
if [ -n "${GITHUB_TOKEN:-}" ]; then
  echo "==> GITHUB_TOKEN presente: GitHub Models disponivel."
  echo "    Teste com: python ai/ask.py \"diga ola\""
else
  echo "==> AVISO: GITHUB_TOKEN ausente."
  echo "    No Codespaces ele e injetado automaticamente."
  echo "    Localmente, rode: export GITHUB_TOKEN=\$(gh auth token)"
  echo "    Ou use Ollama offline: ollama serve && ollama pull qwen2.5:3b"
fi

echo ""
echo "Ambiente pronto. Comece pelo README.md."
