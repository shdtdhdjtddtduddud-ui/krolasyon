from mobs import _envoy
ID = 'envoy_bone'
SPEC = _envoy.spec(ID, 'bone', 'Kemik Haberci', 'Bone Herald', (0xe8e0c8, 0x30b8e8), "Kemik Krallığı'nın kanun okuyucusu. Her şeyin bir yasası, her yasanın bir bedeli vardır.")


def build():
    return _envoy.make(ID, 'bone')
