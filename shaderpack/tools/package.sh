#!/usr/bin/env bash
# Shader paketini Oculus/Iris'in doğrudan okuyabileceği bir zip dosyasına paketler.
set -euo pipefail
OUT="$(realpath -m "${1:-KrolasyonFantasy.zip}")"
cd "$(dirname "$0")/.."
python3 tools/gen_stubs.py
rm -f "$OUT"
(cd KrolasyonFantasy && zip -qr "$OUT" shaders)
echo "Wrote $OUT"
