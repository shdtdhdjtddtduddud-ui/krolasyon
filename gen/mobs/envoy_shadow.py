from mobs import _envoy
ID = 'envoy_shadow'
SPEC = _envoy.spec(ID, 'shadow', 'Gölge Elçisi', 'Shadow Envoy', (0x14082a, 0xc8a0ff), "Gölge Tarikatı'nın fısıltıcısı. Söylediği her şey bir başka gerçeği saklar.")


def build():
    return _envoy.make(ID, 'shadow')
