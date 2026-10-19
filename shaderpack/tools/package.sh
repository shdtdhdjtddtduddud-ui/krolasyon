#!/usr/bin/env bash
# Shader paketini Oculus/Iris'in doğrudan okuyabileceği bir zip dosyasına paketler.
set -euo pipefail
cd "$(dirname "$0")/.."
python3 tools/gen_stubs.py
OUT="$(realpath -m "${1:-KrolasyonFantasy.zip}")"
rm -f "$OUT"
(cd KrolasyonFantasy && zip -qr "$OUT" shaders)
echo "Wrote $OUT"
