#!/usr/bin/env python3
"""src/ klasöründeki Luau dosyalarını Roblox Studio'nun doğrudan açabildiği
bir .rbxlx yer dosyasına paketler (Rojo gerekmez).

Kural (Rojo ile aynı):
  Name.server.lua  -> Script
  Name.client.lua  -> LocalScript
  Name.lua         -> ModuleScript
  klasör           -> Folder  (içinde init.server.lua / init.client.lua / init.lua
                                varsa o klasör o türde bir script olur)

Kullanım:  python3 build.py   ->  ItachiArena.rbxlx
"""
import os
import sys
from xml.sax.saxutils import escape

ROOT = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(ROOT, "src")
OUT = os.path.join(ROOT, "ItachiArena.rbxlx")

_ref = 0


def ref():
    global _ref
    _ref += 1
    return "RBX%08X" % _ref


def script_class(filename):
    if filename.endswith(".server.lua"):
        return "Script", filename[: -len(".server.lua")]
    if filename.endswith(".client.lua"):
        return "LocalScript", filename[: -len(".client.lua")]
    if filename.endswith(".lua"):
        return "ModuleScript", filename[: -len(".lua")]
    return None, None


def read(path):
    with open(path, encoding="utf-8") as f:
        src = f.read()
    if "]]>" in src:
        sys.exit(f"{path}: ']]>' CDATA içinde kullanılamaz")
    return src


def item(cls, name, source=None, children="", extra=""):
    props = f'<string name="Name">{escape(name)}</string>'
    if source is not None:
        props += f'<ProtectedString name="Source"><![CDATA[{source}]]></ProtectedString>'
        if cls == "Script":
            props += '<bool name="Disabled">false</bool>'
    props += extra
    return f'<Item class="{cls}" referent="{ref()}"><Properties>{props}</Properties>{children}</Item>\n'


def build_dir(path, name):
    entries = sorted(os.listdir(path))
    init = None
    for init_name in ("init.server.lua", "init.client.lua", "init.lua"):
        if init_name in entries:
            init = init_name
    children = ""
    for e in entries:
        if e == init:
            continue
        full = os.path.join(path, e)
        if os.path.isdir(full):
            children += build_dir(full, e)
        else:
            cls, n = script_class(e)
            if cls:
                children += item(cls, n, read(full))
    if init:
        cls, _ = script_class(init)
        return item(cls, name, read(os.path.join(path, init)), children)
    return item("Folder", name, None, children)


def build_service(cls, path, extra=""):
    children = ""
    if os.path.isdir(path):
        for e in sorted(os.listdir(path)):
            full = os.path.join(path, e)
            if os.path.isdir(full):
                children += build_dir(full, e)
            else:
                c, n = script_class(e)
                if c:
                    children += item(c, n, read(full))
    return item(cls, cls, None, children, extra)


def main():
    body = ""
    body += build_service(
        "Workspace",
        "",
        '<bool name="StreamingEnabled">false</bool>'
        '<float name="Gravity">196.2</float>',
    )
    body += build_service("ReplicatedStorage", os.path.join(SRC, "ReplicatedStorage"))
    body += build_service("ServerScriptService", os.path.join(SRC, "ServerScriptService"))
    sp = os.path.join(SRC, "StarterPlayer")
    sp_children = build_service("StarterPlayerScripts", os.path.join(sp, "StarterPlayerScripts"))
    sp_children += build_service("StarterCharacterScripts", os.path.join(sp, "StarterCharacterScripts"))
    body += item(
        "StarterPlayer",
        "StarterPlayer",
        None,
        sp_children,
        '<bool name="EnableMouseLockOption">true</bool>'
        '<float name="CharacterWalkSpeed">20</float>'
        '<bool name="CharacterUseJumpPower">true</bool>'
        '<float name="CharacterJumpPower">50</float>',
    )
    body += item("Players", "Players", None, "", '<float name="RespawnTime">3</float>')
    # Future ışıklandırma (betikten ayarlanamadığı için yer dosyasında)
    body += item("Lighting", "Lighting", None, "", '<token name="Technology">4</token>')

    xml = (
        '<roblox xmlns:xmime="http://www.w3.org/2005/05/xmlmime" '
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" '
        'xsi:noNamespaceSchemaLocation="http://www.roblox.com/roblox.xsd" version="4">\n'
        + body
        + "</roblox>\n"
    )
    with open(OUT, "w", encoding="utf-8") as f:
        f.write(xml)
    print("yazıldı:", OUT, f"({len(xml) // 1024} KB)")


if __name__ == "__main__":
    main()
