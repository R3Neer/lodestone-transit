"""Assemble UI-drawn 16px Piskel layers into Minecraft resources.

No drawing algorithms: only lossless decoding, compositing, nearest-neighbour
compass rotation and extrusion of the authored pixels into model cuboids.
"""
from pathlib import Path
import copy
import json
import math
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
NS = 'lodestone_transit'
A = f'assets/{NS}'
SOURCE = ROOT / 'art/piskel/manual-16.json'
AUTHORED = json.loads(SOURCE.read_text(encoding='utf-8'))
P = [tuple(c) for c in AUTHORED['colors']]
for source_png in sorted(SOURCE.parent.glob('*.png')):
    with Image.open(source_png) as im:
        assert im.size == (16, 16), f'Expected a native 16px layer: {source_png}'
        for color in im.convert('RGBA').getdata():
            if color[3] and color not in P:
                P.append(color)
assert len(P) <= 64, 'The model palette supports up to 64 opaque colors.'

def layer(name):
    png = SOURCE.parent / (name + '.png')
    if png.exists():
        with Image.open(png) as im:
            return im.convert('RGBA')
    rows = AUTHORED['layers'][name]
    assert len(rows) == 16 and all(len(row) == 16 for row in rows)
    im = Image.new('RGBA', (16, 16))
    im.putdata([P[int(v, 36)] if v != '.' else (0, 0, 0, 0) for row in rows for v in row])
    return im

def crystal(frame):
    # The manually drawn NW-pointing master corresponds to vanilla frame 12.
    return layer('amethyst').rotate(-(frame-12)*360/32,
        resample=Image.Resampling.NEAREST, center=(8, 7.5))

def export_sources():
    import base64
    import io
    directory = SOURCE.parent
    for name in AUTHORED['layers']:
        layer(name).save(directory / (name + '.png'))
    # Package the actual copied layers into editable Piskel documents.
    for case_name in ('iron', 'ivory'):
        layers = []
        for name in (case_name, 'Ender fragments', 'amethyst'):
            strip = Image.new('RGBA', (80,16))
            for n in range(5):
                strip.paste(layer('fragments_'+str(n) if name == 'Ender fragments' else name), (n*16,0))
            data = io.BytesIO(); strip.save(data, format='PNG')
            layers.append(json.dumps({'name':name,'opacity':1,'frameCount':5,
                'chunks':[{'layout':[[n] for n in range(5)],
                           'base64PNG':'data:image/png;base64,'+base64.b64encode(data.getvalue()).decode()}]}))
        document={'modelVersion':2,'piskel':{'name':'Teleporter '+case_name+' 16px',
            'description':'Manually painted in Piskel through computer use. Five charge states.',
            'fps':1,'height':16,'width':16,'layers':layers,'hiddenFrames':['']}}
        (directory/(case_name+'.piskel')).write_text(json.dumps(document),encoding='utf-8')

def write(path, value):
    target = RES / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, separators=(',', ':')) + '\n', encoding='utf-8')

def save(name, im):
    path = RES / A / 'textures' / (name + '.png')
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)

def sprite(dim,charges,frame):
    im = layer('ivory' if dim else 'iron')
    im.alpha_composite(layer('fragments_'+str(charges)))
    im.alpha_composite(crystal(frame))
    return im

def ref(name): return {'type':'minecraft:model','model':f'{NS}:{name}'}
def composite(*models): return {'type':'minecraft:composite','models':list(models)}
def faces(tex): return {f:{'texture':tex,'uv':[0,0,16,16]} for f in ('up','down','north','south','east','west')}
def box(start,end,color):
    uv=[(color%8)*2,(color//8)*2,(color%8)*2+2,(color//8)*2+2]
    return {'from':start,'to':end,'faces':{f:{'texture':'#palette','uv':uv} for f in faces('')}}

def case(dim):
    side,top,edge=(16,15,14) if dim else (13,12,10)
    es=[box([3.5,0,3.5],[12.5,1.25,12.5],side),box([4.5,1.25,4.5],[11.5,1.26,11.5],11)]
    for a,b in [([3.5,1.25,3.5],[12.5,2,4.5]),([3.5,1.25,11.5],[12.5,2,12.5]),
                ([3.5,1.25,4.5],[4.5,2,11.5]),([11.5,1.25,4.5],[12.5,2,11.5])]:
        e=box(a,b,edge)
        e['faces']['up']=box(a,b,top)['faces']['up']
        es.append(e)
    # One quiet horizontal seam, not a noisy checkerboard.
    for a,b in [([3.5,.43,3.49],[12.5,.57,3.5]),([3.5,.43,12.5],[12.5,.57,12.51]),
                ([3.49,.43,3.5],[3.5,.57,12.5]),([12.5,.43,3.5],[12.51,.57,12.5])]:
        es.append(box(a,b,9))
    return es

def pixels_geometry(im, height=.25):
    # Extrude the exact Piskel pixels; adjacent equal-color cells share a cuboid.
    es=[]
    for y in range(16):
        x=0
        while x<16:
            color=im.getpixel((x,y))
            if color[3]==0: x+=1; continue
            end=x+1
            while end<16 and im.getpixel((end,y))==color: end+=1
            es.append(box([3.5+x*.5625,1.265,3.5+y*.5625],
                [3.5+end*.5625,1.265+height,3.5+(y+1)*.5625],P.index(color)))
            x=end
    return es

DISPLAY={'firstperson_righthand':{'rotation':[0,135,0],'translation':[.5,5.5,0],'scale':[.65]*3},
         'firstperson_lefthand':{'rotation':[0,135,0],'translation':[.5,5.5,0],'scale':[.65]*3},
         'thirdperson_righthand':{'rotation':[0,165,10],'translation':[.5,5,.75],'scale':[.7]*3},
         'thirdperson_lefthand':{'rotation':[0,165,10],'translation':[.5,5,.75],'scale':[.7]*3},
         'ground':{'rotation':[45,0,0],'translation':[0,4.5,2],'scale':[.65]*3},
         'fixed':{'translation':[0,6,-2],'scale':[2]*3}}

def upright(es):
    out=copy.deepcopy(es)
    mapping={'up':'north','down':'south','north':'up','south':'down','east':'east','west':'west'}
    for e in out:
        a,b=e['from'],e['to']
        e['from']=[a[0],12.5-b[2],9-b[1]]
        e['to']=[b[0],12.5-a[2],9-a[1]]
        e['faces']={mapping[f]:v for f,v in e['faces'].items()}
    return out

def physical(es):
    return {'textures':{'palette':f'{NS}:block/device_palette','particle':f'{NS}:block/device_palette'},'display':DISPLAY,'elements':upright(es)}

def charge_dispatch(models):
    return {'type':'minecraft:range_dispatch','property':f'{NS}:charges','entries':[{'threshold':i,'model':m} for i,m in enumerate(models)],'fallback':models[0]}

def compass_dispatch(models):
    entries=[{'threshold':max(0,i-.5),'model':models[(i+16)%32]} for i in range(33)]
    return {'type':'minecraft:range_dispatch','property':'minecraft:compass','target':'spawn','scale':32,'entries':entries,'fallback':entries[0]['model']}

def station_overlay(filled):
    im=layer('fragments_'+str(filled))
    im.alpha_composite(layer('station_amethyst'))
    return im

def ivory_stone(top=False):
    return layer('ivory_side')

def export_core():
    """Copy the manually painted core and package its editable Piskel document."""
    import base64
    core = SOURCE.parent / 'dimensional_core.png'
    with Image.open(core) as im:
        assert im.size == (16, 16), 'The core must stay at native Minecraft resolution.'
        save('item/dimensional_core', im.convert('RGBA'))
    write(f'{A}/models/item/dimensional_core.json', {
        'parent': 'minecraft:item/generated',
        'textures': {'layer0': f'{NS}:item/dimensional_core'}})
    document = {'modelVersion': 2, 'piskel': {
        'name': 'Dimensional Core - Fractured Eye',
        'description': 'Concept B, hand-painted at 16x16 in Piskel using computer use.',
        'fps': 1, 'height': 16, 'width': 16,
        'layers': [json.dumps({'name': 'Fractured Eye', 'opacity': 1, 'frameCount': 1,
            'chunks': [{'layout': [[0]], 'base64PNG': 'data:image/png;base64,' +
                       base64.b64encode(core.read_bytes()).decode()}]})],
        'hiddenFrames': ['']}}
    (SOURCE.parent / 'dimensional_core.piskel').write_text(json.dumps(document), encoding='utf-8')


def generate():
    export_core()
    export_sources()
    atlas=Image.new('RGBA',(16,16))
    for i,c in enumerate(P): ImageDraw.Draw(atlas).rectangle(((i%8)*2,(i//8)*2,(i%8)*2+1,(i//8)*2+1),fill=c)
    save('block/device_palette',atlas)
    save('block/ivory_lodestone_side',ivory_stone())
    save('block/ivory_lodestone_top',ivory_stone(True))
    for n in range(5): save(f'block/station_sockets_{n}',station_overlay(n))
    for frame in range(32):
        write(f'{A}/models/item/crafted/needle_{frame:02}.json',physical(pixels_geometry(crystal(frame), .4375)))
    for dim in (False,True):
        name=('dimensional_' if dim else '')+'teleporter'
        by_charge=[]; by_charge_3d=[]
        for charge in range(5):
            hull=f'item/crafted/{name}_body_{charge}'
            write(f'{A}/models/{hull}.json',physical(case(dim)+pixels_geometry(layer('fragments_'+str(charge)))))
            flat=[]; solid=[]
            for frame in range(32):
                stem=f'crafted/{name}_{charge}_{frame:02}'
                save('item/'+stem,sprite(dim,charge,frame))
                write(f'{A}/models/item/{stem}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{NS}:item/{stem}'}})
                flat.append(ref('item/'+stem))
                solid.append(composite(ref(hull),ref(f'item/crafted/needle_{frame:02}')))
            by_charge.append(compass_dispatch(flat)); by_charge_3d.append(compass_dispatch(solid))
        flat_dispatch=charge_dispatch(by_charge)
        physical_dispatch=charge_dispatch(by_charge_3d)
        write(f'{A}/items/{name}.json',{'model':flat_dispatch})
        context={'type':'minecraft:select','property':'minecraft:display_context','cases':[{'when':['gui','on_shelf','fixed'],'model':flat_dispatch}],'fallback':physical_dispatch}
        write(f'resourcepacks/p1kl_compat/{A}/items/{name}.json',{'model':{'type':'minecraft:condition','property':f'{NS}:p1kl_available','on_true':context,'on_false':flat_dispatch}})
        # Legacy item-model names keep recovery artwork identical.
        for prefix in ('', 'resourcepacks/p1kl_compat/'):
            data=json.loads((RES/(prefix+A+'/items/'+name+'.json')).read_text())
            alias=('dimensional_' if dim else '')+'recovery_teleporter'
            write(prefix+A+'/items/'+alias+'.json',data)
        save('item/'+name,sprite(dim,4,12))
        station=('dimensional_' if dim else '')+'teleport_station'
        # The approved dimensional station has ivory stone; vanilla pattern remains replaceable.
        tx={'side':'minecraft:block/lodestone_side','top':'minecraft:block/lodestone_top','particle':'minecraft:block/lodestone_side','palette':f'{NS}:block/device_palette'}
        if dim:
            tx.update(side=f'{NS}:block/ivory_lodestone_side',top=f'{NS}:block/ivory_lodestone_top',particle=f'{NS}:block/ivory_lodestone_side')
        # Four faces are a real sixteen-socket reservoir, not four coarse charge bands.
        for count in range(17):
            es=[{'from':[0,0,0],'to':[16,16,16],'faces':{f:{'texture':'#top' if f in ('up','down') else '#side','uv':[0,0,16,16],'cullface':f} for f in faces('')}}]
            textures=dict(tx)
            for index,face in enumerate(('north','east','south','west')):
                filled=max(0,min(4,count-index*4))
                textures[face]=f'{NS}:block/station_sockets_{filled}'
                start,end={'north':([0,0,-.01],[16,16,-.01]),'east':([16.01,0,0],[16.01,16,16]),'south':([0,0,16.01],[16,16,16.01]),'west':([-.01,0,0],[-.01,16,16])}[face]
                es.append({'from':start,'to':end,'faces':{face:{'texture':'#'+face,'uv':[0,0,16,16]}}})
            for element in case(dim)+pixels_geometry(crystal(12), .4375):
                e=copy.deepcopy(element)
                e['from'][1]+=16; e['to'][1]+=16
                e['rotation']={'origin':[8,16,8],'axis':'y','angle':45}
                es.append(e)
            write(f'{A}/models/block/{station}_{count}.json',{'parent':'minecraft:block/block','textures':textures,'elements':es})
        write(f'{A}/blockstates/{station}.json',{'variants':{f'pearls={i}':{'model':f'{NS}:block/{station}_{i}'} for i in range(17)}})
        write(f'{A}/models/block/{station}.json',{'parent':f'{NS}:block/{station}_0'})
        write(f'{A}/items/{station}.json',{'model':ref(f'block/{station}_0')})
    print('Assembled UI-painted 16px masters: 320 sprites, 32 crystal rotations, 10 hulls, 34 station states.')

def preview():
    target=ROOT/'build/visual-review'; target.mkdir(parents=True,exist_ok=True)
    sheet=Image.new('RGB',(1100,640),'#282b31'); d=ImageDraw.Draw(sheet)
    for row,dim in enumerate((False,True)):
        name=('dimensional_' if dim else '')+'teleporter'
        for charge in range(5):
            im=sprite(dim,charge,12)
            sheet.paste(im.resize((160,160),Image.Resampling.NEAREST),(charge*215+20,row*240+35),im.resize((160,160),Image.Resampling.NEAREST))
            sheet.paste(im,(charge*215+75,row*240+205),im)
            d.text((charge*215+25,row*240+12),f'{name}: {charge}/4',fill='white')
    for i in range(8):
        im=sprite(False,4,i*4)
        sheet.paste(im.resize((96,96),Image.Resampling.NEAREST),(i*135+10,530),im.resize((96,96),Image.Resampling.NEAREST))
    sheet.save(target/'sprites.png')
    sockets=Image.new('RGB',(800,180),'#505050')
    for i in range(5):
        im=station_overlay(i).resize((144,144),Image.Resampling.NEAREST)
        sockets.paste(im,(i*160+8,20),im)
    sockets.save(target/'station-sockets.png')

if __name__=='__main__':
    generate()
    preview()
