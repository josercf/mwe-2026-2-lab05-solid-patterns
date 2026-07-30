#!/usr/bin/env python3
"""
Cliente minimo de IA para os laboratorios da disciplina.

Ordem de tentativa:
  1. GitHub Models  - usa o GITHUB_TOKEN, que o Codespaces ja injeta.
                      Nenhuma conta ou cartao adicional e necessario.
  2. Ollama local   - fallback offline, se voce tiver `ollama serve` rodando.

Uso:
    python ai/ask.py "escreva um PRD para o servico de telemetria"
    cat prompt.txt | python ai/ask.py
    MODEL=microsoft/phi-4-mini-instruct python ai/ask.py "..."

Sem dependencias externas: so a biblioteca padrao.
"""
import json
import os
import sys
import urllib.error
import urllib.request

GITHUB_ENDPOINT = "https://models.github.ai/inference/chat/completions"
OLLAMA_ENDPOINT = "http://localhost:11434/api/chat"

# Modelos pequenos, adequados ao uso em sala
DEFAULT_GITHUB_MODEL = os.environ.get("MODEL", "openai/gpt-4o-mini")
DEFAULT_OLLAMA_MODEL = os.environ.get("OLLAMA_MODEL", "qwen2.5:1.5b")

TIMEOUT = int(os.environ.get("AI_TIMEOUT", "120"))


def _post(url, payload, headers, timeout=TIMEOUT):
    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json", **headers},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def via_github_models(prompt):
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if not token:
        raise RuntimeError("GITHUB_TOKEN ausente")

    data = _post(
        GITHUB_ENDPOINT,
        {
            "model": DEFAULT_GITHUB_MODEL,
            "messages": [{"role": "user", "content": prompt}],
            "temperature": 0.7,
        },
        {"Authorization": "Bearer " + token},
    )
    return data["choices"][0]["message"]["content"]


def via_ollama(prompt):
    data = _post(
        OLLAMA_ENDPOINT,
        {
            "model": DEFAULT_OLLAMA_MODEL,
            "messages": [{"role": "user", "content": prompt}],
            "stream": False,
        },
        {},
        timeout=300,
    )
    return data["message"]["content"]


def main():
    prompt = " ".join(sys.argv[1:]).strip()
    if not prompt and not sys.stdin.isatty():
        prompt = sys.stdin.read().strip()
    if not prompt:
        print(__doc__)
        return 1

    tentativas = [("GitHub Models", via_github_models), ("Ollama local", via_ollama)]
    erros = []

    for nome, fn in tentativas:
        try:
            print("[{}] consultando...".format(nome), file=sys.stderr)
            print(fn(prompt))
            return 0
        except urllib.error.HTTPError as e:
            corpo = e.read().decode("utf-8", "replace")[:300]
            erros.append("{}: HTTP {} {}".format(nome, e.code, corpo))
        except Exception as e:  # noqa: BLE001
            erros.append("{}: {}".format(nome, e))

    print("\nNenhum backend de IA respondeu.\n", file=sys.stderr)
    for e in erros:
        print("  - " + e, file=sys.stderr)
    print(
        "\nDicas:\n"
        "  - No Codespaces o GITHUB_TOKEN e injetado automaticamente.\n"
        "  - Localmente: export GITHUB_TOKEN=$(gh auth token)\n"
        "  - Offline: ollama serve && ollama pull qwen2.5:3b\n",
        file=sys.stderr,
    )
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
