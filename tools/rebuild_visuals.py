"""Hand-authored pixel accents over referenced vanilla art; no copied third-party assets.

Pillow draws only our tiny inlays. All compass frames, stone and crystal artwork
remain Minecraft resource references so resource packs can replace them normally.
"""
from pathlib import Path
import json
from PIL import Image

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
NS = 'lodestone_transit'

def write(path, value):
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')

def texture(name, pixels):
    image = Image.new('RGBA', (16, 16))
    for (x,y), color in pixels.items():
        image.putpixel((x,y), tuple(bytes.fromhex(color)) + (255,))
    target = ROOT / f'assets/{NS}/textures/{name}.png'
    target.parent.mkdir(parents=True, exist_ok=True)
    image.save(target)

# Crisp, stepped gemstone facets: never paint over the central moving needle.
normal = {(6,2):'6c4396',(7,2):'ceb0f0',(8,2):'a477cf',(9,2):'62418a',
          (7,3):'9970c3',(8,3):'69458e',(3,6):'a477cf',(3,7):'69458e',
          (12,6):'9970c3',(12,7):'62418a',(6,12):'2a6556',(7,12):'6aaa8b',
          (8,12):'418576',(9,12):'285346'}
dimensional = dict(normal)
dimensional.update({(5,2):'b693dc',(6,2):'513d69',(9,2):'513d69',(10,2):'b693dc',
                    (5,3):'6e508b',(10,3):'6e508b',(3,5):'33293f',(12,5):'33293f',
                    (4,11):'d7d2a4',(5,12):'a8a274',(10,12):'a8a274',(11,11):'d7d2a4'})
texture('item/teleporter_inlay', normal)
texture('item/dimensional_teleporter_inlay', dimensional)
texture('block/teleporter_inlay', normal)
texture('block/dimensional_teleporter_inlay', dimensional)
# The block atlas uses an alias of vanilla art, not a redistributed PNG.
write('assets/minecraft/atlases/blocks.json',{'sources':[{'type':'minecraft:single','resource':'minecraft:item/compass_16','sprite':f'{NS}:block/station_dial'}]})
# Small stone-face centers, surrounded by their original carved stone outlines.
side = {(7,6):'9970c3',(8,6):'ceb0f0',(6,7):'9970c3',(7,7):'b592da',(8,7):'9970c3',(9,7):'69458e',
        (6,8):'69458e',(7,8):'9970c3',(8,8):'7e55a8',(9,8):'51366f',(7,9):'69458e',(8,9):'51366f'}
texture('block/calibration_inlay', side)

def model_ref(name): return {'type':'minecraft:model','model':name}
def cube(start, end, textures):
    return {'from':start,'to':end,'faces':{face:{'texture':tex,'uv':[0,0,16,16]} for face,tex in textures.items()}}
def body():
    result = cube([0,0,0],[16,16,16],{f:('#top' if f in ('up','down') else '#side') for f in ('up','down','north','south','west','east')})
    for face, data in result['faces'].items(): data['cullface'] = face
    return result
def side_inlays():
    return [cube([0,0,-.008],[16,16,-.008],{'north':'#inlay'}),
            cube([0,0,16.008],[16,16,16.008],{'south':'#inlay'}),
            cube([-.008,0,0],[-.008,16,16],{'west':'#inlay'}),
            cube([16.008,0,0],[16.008,16,16],{'east':'#inlay'})]

for dimensional_flag in (False,True):
    prefix = 'dimensional_' if dimensional_flag else ''
    for recovery in (False,True):
        name = prefix + ('recovery_' if recovery else '') + 'teleporter'
        base = 'compass' # Recovery changes the default name and target, not the artwork.
        accent = prefix + 'teleporter_inlay'
        entries = []
        bridge_entries = []
        # Vanilla's 32-frame wrap includes frame 16 at both boundaries.
        for i in range(33):
            frame = (i+16)%32
            path = f'{name}_{frame:02}'
            write(f'assets/{NS}/models/item/{path}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'minecraft:item/{base}_{frame:02}','layer1':f'{NS}:item/{accent}'}})
            threshold = max(0, i-.5)
            flat = model_ref(f'{NS}:item/{path}')
            entries.append({'threshold':threshold,'model':flat})
            bridge_base = f'minecraft:item/{base}_{frame:02}_in_hand'
            # Low-priority aliases prevent missing-model errors when the bridge is
            # enabled without p1kl. The external pack overrides these aliases.
            write(f'assets/minecraft/models/item/{base}_{frame:02}_in_hand.json',{'parent':f'minecraft:item/{base}_{frame:02}'})
            gems = [(7,8,9,9)] if not dimensional_flag else [(5,8,6,9),(10,8,11,9)]
            attachment = {'parent':bridge_base,'textures':{'particle':f'minecraft:item/{base}_{frame:02}','gem':'minecraft:block/amethyst_block'},
                          'elements':[cube([x,y,6.94],[xx,yy,9.04],{f:'#gem' for f in ('up','down','north','south','west','east')}) for x,y,xx,yy in gems]}
            write(f'resourcepacks/p1kl_compat/assets/{NS}/models/item/{path}_inlay.json',attachment)
            physical = {'type':'minecraft:composite','models':[model_ref(bridge_base),model_ref(f'{NS}:item/{path}_inlay')]}
            bridge_entries.append({'threshold':threshold,'model':{'type':'minecraft:select','property':'minecraft:display_context','cases':[{'when':['gui','on_shelf','fixed'],'model':flat}],'fallback':physical}})
        dispatch = {'type':'minecraft:range_dispatch','property':'minecraft:compass','target':'spawn','scale':32,'entries':entries,'fallback':entries[0]['model']}
        write(f'assets/{NS}/items/{name}.json',{'model':dispatch})
        bridge = dict(dispatch, entries=bridge_entries, fallback=bridge_entries[0]['model'])
        write(f'resourcepacks/p1kl_compat/assets/{NS}/items/{name}.json',{'model':{'type':'minecraft:condition','property':f'{NS}:p1kl_available','on_true':bridge,'on_false':dispatch}})

textures = {'particle':'minecraft:block/lodestone_side','side':'minecraft:block/lodestone_side','top':'minecraft:block/lodestone_top','inlay':f'{NS}:block/calibration_inlay','bud':'minecraft:block/small_amethyst_bud'}
# Crossed planes retain the vanilla bud's pixel density. Only four visible pixels
# rise above the cube; the upper part of the texture is transparent.
bud = []
for start,end,faces in [([.8,16,8],[15.2,32,8],('north','south')),([8,16,.8],[8,32,15.2],('west','east'))]:
    e = cube(start,end,{f:'#bud' for f in faces})
    e.update(rotation={'origin':[8,24,8],'axis':'y','angle':45,'rescale':True},shade=False)
    bud.append(e)
blocks = {'calibrated_lodestone':{'parent':'minecraft:block/block','textures':textures,'elements':[body(),*side_inlays(),*bud]}}
for prefix in ('','dimensional_'):
    name = prefix+'teleport_station'
    tx = dict(textures, dial=f'{NS}:block/station_dial', accent=f'{NS}:block/{prefix}teleporter_inlay')
    elements = [body(),*side_inlays()]
    # Horizontal instrument, almost flush with the original lodestone top.
    for y,tex in [(16.016,'#dial'),(16.028,'#accent')]:
        elements.append(cube([0,y,0],[16,y,16],{'up':tex}))
    blocks[name] = {'parent':'minecraft:block/block','textures':tx,'elements':elements}
for name, block in blocks.items():
    write(f'assets/{NS}/models/block/{name}.json',block)
    write(f'assets/{NS}/blockstates/{name}.json',{'variants':{'':{'model':f'{NS}:block/{name}'}}})
    write(f'assets/{NS}/items/{name}.json',{'model':model_ref(f'{NS}:block/{name}')})

# The core reuses the nether-star silhouette with hand-placed amethyst sockets.
texture('item/core_inlay',{(7,2):'ceb0f0',(8,2):'9970c3',(3,6):'a477cf',(12,6):'69458e',(7,12):'9970c3',(8,12):'69458e'})
write(f'assets/{NS}/models/item/dimensional_core.json',{'parent':'minecraft:item/generated','textures':{'layer0':'minecraft:item/nether_star','layer1':f'{NS}:item/core_inlay'}})
write(f'assets/{NS}/items/dimensional_core.json',{'model':model_ref(f'{NS}:item/dimensional_core')})

write(f'data/{NS}/recipe/calibrated_lodestone.json',{'type':'minecraft:crafting_shaped','category':'redstone','pattern':['CAC','CIC','CCC'],'key':{'C':'minecraft:chiseled_stone_bricks','A':'minecraft:amethyst_shard','I':'minecraft:iron_ingot'},'result':{'id':f'{NS}:calibrated_lodestone','count':1}})
for name,operation,unlock in [('teleporter','teleporter','minecraft:ender_eye'),('recovery_teleporter','recovery','minecraft:recovery_compass'),('teleport_station','station',f'{NS}:teleporter'),('dimensional_teleport_station','dimensional_station',f'{NS}:dimensional_teleporter'),('dimensional_upgrade','upgrade',f'{NS}:teleporter'),('dimensional_core','core','minecraft:nether_star'),('calibrated_lodestone',None,'minecraft:amethyst_shard')]:
    if operation:
        recipe = {'type':f'{NS}:transit','operation':operation}
        if operation == 'core': recipe['fabric:load_conditions'] = [{'condition':'fabric:not','value':{'condition':'fabric:registry_contains','registry':'minecraft:item','values':['alexsmobs:dimensional_carver']}}]
        write(f'data/{NS}/recipe/{name}.json',recipe)
    write(f'data/{NS}/advancement/recipes/{name}.json',{'parent':'minecraft:recipes/root','criteria':{'has_ingredient':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':unlock}]}},'has_the_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':f'{NS}:{name}'}}},'requirements':[['has_ingredient','has_the_recipe']],'rewards':{'recipes':[f'{NS}:{name}']}})
    if operation == 'core':
        path=f'data/{NS}/advancement/recipes/{name}.json'
        advancement=json.loads((ROOT/path).read_text(encoding='utf-8'))
        advancement['fabric:load_conditions']=recipe['fabric:load_conditions']
        write(path,advancement)
write(f'data/{NS}/loot_table/blocks/calibrated_lodestone.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'{NS}:calibrated_lodestone'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
for tag in ('mineable/pickaxe','needs_stone_tool'):
    write(f'data/minecraft/tags/block/{tag}.json',{'replace':False,'values':[f'{NS}:{n}' for n in blocks]})
write('data/frozenlib/tags/block/has_pushable_block_entity.json',{'replace':False,'values':[f'{NS}:teleport_station',f'{NS}:dimensional_teleport_station']})
write('resourcepacks/p1kl_compat/pack.mcmeta',{'pack':{'description':'Lodestone Transit — p1kl’s 3D Items bridge (enable above p1kl’s pack)','min_format':[88,0],'max_format':[88,0]}})

MESSAGES = {
'warming_up':('Teleporter is still warming up.','El teletransportador aún se está preparando.'),
'no_fuel':('No ender pearls. Load a pearl to teleport.','No quedan perlas de ender. Carga una para teletransportarte.'),
'no_destination':('No destination linked.','No hay ningún destino enlazado.'),
'anchor_unavailable':('Linked lodestone is no longer available.','La magnetita enlazada ya no está disponible.'),
'uncalibrated':('Linked lodestone is not calibrated. Teleportation unavailable.','La magnetita enlazada no está calibrada. Teletransporte no disponible.'),
'anchor_moving':('Linked lodestone is moving. Try again when it stops.','La magnetita enlazada se está moviendo. Inténtalo cuando se detenga.'),
'no_death':('No last death location is available.','No hay una ubicación de última muerte disponible.'),
'dimension_unavailable':('The destination dimension is unavailable.','La dimensión de destino no está disponible.'),
'wrong_dimension':('Destination is in another dimension. A dimensional teleporter is required.','El destino está en otra dimensión. Necesitas un teletransportador dimensional.'),
'invalid_state':('You cannot teleport in your current state.','No puedes teletransportarte en tu estado actual.'),
'sleeping':('Wake up before teleporting.','Despiértate antes de teletransportarte.'),
'no_space':('No safe arrival space near the destination.','No hay un lugar de llegada seguro cerca del destino.'),
'no_mount_space':('Not enough safe arrival space for your mount and passengers.','No hay espacio de llegada seguro para tu montura y sus pasajeros.'),
'group_restricted':('Your mount or a passenger cannot travel to this dimension.','Tu montura o uno de sus pasajeros no puede viajar a esta dimensión.'),
'transfer_failed':('Teleportation could not be completed.','No se ha podido completar el teletransporte.'),
'leash_left_behind':('Some leashed entities could not follow you.','Algunas entidades atadas no han podido acompañarte.'),
'station_unavailable':('This teleport station is unavailable.','Esta estación de teletransporte no está disponible.'),
'fuel_full':('Ender pearl storage is full.','El depósito de perlas de ender está lleno.'),
'linked_uncalibrated':('Linked to uncalibrated lodestone. Teleportation unavailable.','Enlazado a una magnetita sin calibrar. Teletransporte no disponible.'),
'linked_calibrated':('Linked to calibrated lodestone.','Enlazado a una magnetita calibrada.')}
for index,lang in enumerate(('en_us','es_es')):
    path = f'assets/{NS}/lang/{lang}.json'
    values = json.loads((ROOT/path).read_text(encoding='utf-8-sig'))
    values.update({f'transit.message.{key}':pair[index] for key,pair in MESSAGES.items()})
    values.update({'transit.station.pearls':('Ender pearls: %s/16','Perlas de ender: %s/16')[index],
                   'transit.container.empty':('Empty','Vacío')[index],
                   'transit.container.full':('Full','Lleno')[index]})
    values.update({'block.lodestone_transit.calibrated_lodestone':('Calibrated Lodestone','Magnetita calibrada')[index],
                   'item.lodestone_transit.recovery_teleporter':('Recovery Teleporter','Teletransportador de recuperación')[index],
                   'item.lodestone_transit.dimensional_recovery_teleporter':('Dimensional Recovery Teleporter','Teletransportador dimensional de recuperación')[index]})
    write(path,values)
print('Generated vanilla-referenced compass frames, stone models, optional 3D bridge, recipes and messages.')
