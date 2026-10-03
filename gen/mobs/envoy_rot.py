from mobs import _envoy
ID = 'envoy_rot'
SPEC = _envoy.spec(ID, 'rot', 'Çürük Elçisi', 'Rot Envoy', (0x4a5a2a, 0xe8f080), "Çürük Divanı'nın bahçıvan elçisi. Dostluk tohum gibidir: ya büyür ya çürür.")


def build():
    return _envoy.make(ID, 'rot')
