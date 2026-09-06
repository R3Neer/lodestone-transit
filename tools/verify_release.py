"""Validate the distributable using only Python's standard library."""
from pathlib import Path
import hashlib
import json
import struct
import zipfile

ROOT = Path(__file__).resolve().parents[1]
version = next(line.split('=', 1)[1].strip() for line in
               (ROOT / 'gradle.properties').read_text().splitlines() if line.startswith('version='))
jar = ROOT / 'build/libs' / f'lodestone-transit-{version}.jar'
sources = jar.with_name(f'lodestone-transit-{version}-sources.jar')
assert sources.is_file(), 'Missing corresponding sources JAR'
with zipfile.ZipFile(jar) as archive:
    names = archive.namelist()
    assert len(names) == len(set(names)), 'Duplicate archive entries'
    metadata = json.loads(archive.read('fabric.mod.json'))
    assert metadata['id'] == 'lodestone_transit' and metadata['version'] == version
    assert metadata['license'] == 'GPL-3.0-only' and metadata['environment'] == '*'
    assert metadata['depends']['minecraft'] == '26.2'
    assert metadata['depends']['fabric-api'] == '>=0.159.0+26.2'
    assert metadata['icon'] in names
    assert any(n.startswith('LICENSE') for n in names)
    assert not any(n.startswith(('net/minecraft/', 'data/lodestone_transit_test/'))
                   or n.endswith(('.piskel', '.zip', '.jar'))
                   or 'gametest' in n.lower() for n in names), 'Unexpected bundled content'
    # Our fallback aliases and atlas references contain no copied upstream artwork.
    allowed_minecraft = {'assets/minecraft/atlases/blocks.json'}
    for base in ('compass', 'recovery_compass'):
        for frame in range(32):
            name = f'assets/minecraft/models/item/{base}_{frame:02}_in_hand.json'
            allowed_minecraft.add(name)
            assert json.loads(archive.read(name)) == {'parent': f'minecraft:item/{base}_{frame:02}'}
    assert {n for n in names if n.startswith('assets/minecraft/') and not n.endswith('/')} == allowed_minecraft
    assert json.loads(archive.read('assets/minecraft/atlases/blocks.json')) == {'sources': [{
        'type': 'minecraft:single', 'resource': 'minecraft:item/compass_16',
        'sprite': 'lodestone_transit:block/station_dial'}]}
    for entry in metadata['entrypoints'].values():
        for name in entry:
            assert name.replace('.', '/') + '.class' in names, name
    pngs = [name for name in names if name.endswith('.png')]
    for name in pngs:
        data = archive.read(name)
        assert data[:8] == b'\x89PNG\r\n\x1a\n' and struct.unpack('>II', data[16:24]) == (16,16), name
    for name in names:
        if name.endswith(('.json', '.mcmeta')):
            json.loads(archive.read(name))
    prefix = 'assets/lodestone_transit/'
    english = json.loads(archive.read(prefix + 'lang/en_us.json'))
    spanish = json.loads(archive.read(prefix + 'lang/es_es.json'))
    assert english.keys() == spanish.keys(), 'Translation keys differ'
    for key in english:
        assert english[key].count('%s') == spanish[key].count('%s'), key
    for station in ('teleport_station', 'dimensional_teleport_station'):
        states = json.loads(archive.read(prefix + f'blockstates/{station}.json'))['variants']
        assert set(states) == {f'pearls={i}' for i in range(17)}, station
        for state in states.values():
            model = state['model'].split(':', 1)[1]
            assert prefix + 'models/' + model + '.json' in names
    core = json.loads(archive.read(prefix + 'models/item/dimensional_core.json'))
    assert core['textures'] == {'layer0':'lodestone_transit:item/dimensional_core'}
out = ROOT / 'build/release'
out.mkdir(parents=True, exist_ok=True)
(out / 'SHA256SUMS').write_text(''.join(f'{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.name}\n'
                                      for p in (jar, sources)), encoding='ascii')
print(f'Verified {jar.name}: metadata, {len(pngs)} native textures, JSON, translations, station states and archive boundaries.')
print(f'Checksums: {out / "SHA256SUMS"}')
