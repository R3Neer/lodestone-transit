"""Local reference contact sheet, never bundled with the mod."""
from pathlib import Path
from PIL import Image, ImageDraw
import zipfile, io, sys

root = Path(__file__).resolve().parents[1] / 'src/main/resources'
with zipfile.ZipFile(sys.argv[1]) as vanilla:
    def base(name): return Image.open(io.BytesIO(vanilla.read(f'assets/minecraft/textures/{name}.png'))).convert('RGBA')
    def accent(name): return Image.open(root / f'assets/lodestone_transit/textures/{name}.png').convert('RGBA')
    sheet = Image.new('RGB',(960,420),'#242830')
    draw = ImageDraw.Draw(sheet)
    labels = ['Vanilla','Teleporter','Dimensional','Recovery','Dim. recovery','Core']
    for i,label in enumerate(labels):
        texture = base('item/'+('nether_star' if i == 5 else 'recovery_compass_16' if i in (3,4) else 'compass_16'))
        if i: texture.alpha_composite(accent('item/'+('core_inlay' if i==5 else 'dimensional_teleporter_inlay' if i in (2,4) else 'teleporter_inlay')))
        sheet.paste(texture.resize((128,128),Image.Resampling.NEAREST),(i*160+16,30),texture.resize((128,128),Image.Resampling.NEAREST))
        sheet.paste(texture,(i*160+72,172),texture)
        draw.text((i*160+15,10),label,fill='white')
    for i in range(6):
        texture = base('item/compass_'+f'{(i*5)%32:02}')
        texture.alpha_composite(accent('item/teleporter_inlay'))
        sheet.paste(texture.resize((96,96),Image.Resampling.NEAREST),(i*160+32,220),texture.resize((96,96),Image.Resampling.NEAREST))
    draw.text((16,340),'32 vanilla needle frames; only our small rim inlays are new pixels.',fill='white')
    draw.text((16,365),'Bottom row: six orientations to check needle clearance. Native size shown above.',fill='white')
    sheet.save(sys.argv[2])
