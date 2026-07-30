#!/usr/bin/env bash
# Preparacao do ambiente do laboratorio. Roda uma vez, na criacao do container.
set -euo pipefail

echo "==> Configurando o laboratorio mwe-2026-2-lab05-solid-patterns"

# --- Dependencias da stack -------------------------------------------------
if [ -f pom.xml ]; then mvn -q -B dependency:go-offline || true; fi

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
