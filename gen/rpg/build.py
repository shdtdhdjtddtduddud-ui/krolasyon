"""Builds every generated RPG asset: Java tables, textures, item models."""
import os, sys
sys.path.insert(0, os.path.dirname(__file__))
import defs, javagen, textures

javagen.main()
textures.main(defs)
if os.path.exists(os.path.join(os.path.dirname(__file__), 'assets.py')):
    import assets
    assets.main(defs)
print('ok', len(defs.MONSTERS), len(defs.BOSSES), len(defs.SPELLS), len(defs.SWORDS))
