from mobs import _envoy
ID = 'envoy_ember'
SPEC = _envoy.spec(ID, 'ember', 'Kor Elçisi', 'Ember Envoy', (0x2a0e08, 0xff8a20), "Kor Hanedanı'nın sözcüsü. Kızıl sancağıyla yabancıları sınar; onurlu olana kapılar açılır.")


def build():
    return _envoy.make(ID, 'ember')
