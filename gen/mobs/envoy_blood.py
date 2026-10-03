from mobs import _envoy
ID = 'envoy_blood'
SPEC = _envoy.spec(ID, 'blood', 'Kan Elçisi', 'Blood Envoy', (0xb01830, 0xf2eae0), "Kan Konseyi'nin nazik diplomatı. Gülümsemesi bir pazarlığın ilk şartıdır.")


def build():
    return _envoy.make(ID, 'blood')
