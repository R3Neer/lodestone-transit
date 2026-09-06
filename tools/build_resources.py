"""Reproducible, hand-authored 16px artwork and JSON resources. No generated AI art.

Requires Pillow only when rebuilding the checked-in textures. Minecraft textures
are referenced by model identifiers, never bundled or copied from other mods.
"""
from pathlib import Path
import json
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
NS = "lodestone_transit"

def write(path, obj):
    dest = ROOT / path
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

def png(name, im):
    dest = ROOT / f"assets/{NS}/textures/{name}.png"
    dest.parent.mkdir(parents=True, exist_ok=True)
    im.save(dest)

# Each row and color is deliberately authored; no noise or procedural art models.
PALETTE = {".": (0,0,0,0), "#": "#292c35", "s": "#686d76", "S": "#b9bdc5",
           "w": "#e2e3d7", "g": "#264e43", "G": "#438b72", "e": "#86b78c",
           "p": "#623a91", "P": "#9970c4", "a": "#d1b3ef", "r": "#c55359"}
DEVICE = [
"................", "......#aa#......", ".....#aPPp#.....", "....#S#PP#S#....",
"...#SSs##sSS#...", "..#SsgGeegGsS#..", ".#SsgGeeweGgsS#.", ".#ssGee##eeGss#.",
".#ssGe####eGss#.", ".#ssGe#rr#eGss#.", "..#ssGe##eGss#..", "..#SsgGrrGgsS#..",
"...#SsgGGgsS#...", "....#SSssSS#....", ".....######.....", "................"]
def pixels(rows):
    im = Image.new("RGBA", (16,16))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, c in enumerate(row): im.putpixel((x,y), Image.new("RGBA", (1,1), PALETTE[c]).getpixel((0,0)))
    return im
normal = pixels(DEVICE)
dimensional = normal.copy()
for xy in [(3,5),(12,5),(2,7),(13,7),(2,9),(13,9),(4,12),(11,12)]: dimensional.putpixel(xy, (182,146,224,255))
png("item/teleporter", normal); png("item/dimensional_teleporter", dimensional)
CORE = ["................",".......aa.......","......aPPa......","......PppP......","..aa..PppP..aa..","..aPPPPppPPPPa..","...PPpSwwSppP...",".aPPpSwSSwSpPPa.",".aPPpSwSSwSpPPa.","...PPpSwwSppP...","..aPPPPppPPPPa..","..aa..PppP..aa..","......PppP......","......aPPa......",".......aa.......","................"]
png("item/dimensional_core", pixels(CORE))
inlay = Image.new("RGBA", (16,16))
d = ImageDraw.Draw(inlay)
d.polygon([(7,4),(10,7),(8,11),(5,8)], fill="#573479")
d.polygon([(7,4),(9,7),(7,9),(5,8)], fill="#a77bd1")
d.line([(7,5),(6,7)], fill="#dfc9ef", width=1)
png("block/lodestone_inlay", inlay)

for name in ["teleporter", "dimensional_teleporter", "dimensional_core"]:
    write(f"assets/{NS}/models/item/{name}.json", {"parent":"minecraft:item/generated", "textures":{"layer0":f"{NS}:item/{name}"}})
    write(f"assets/{NS}/items/{name}.json", {"model":{"type":"minecraft:model", "model":f"{NS}:item/{name}"}})

def cube(top, side, bottom):
    return {"from":[0,0,0],"to":[16,16,16],"faces":{face:{"texture":tex,"cullface":face} for face,tex in {"up":top,"down":bottom,"north":side,"south":side,"east":side,"west":side}.items()}}

inlay_elements = [{"from":[x,16.005,y],"to":[x+1,16.005,y+1],"faces":{"up":{"texture":"#inlay","uv":[x,y,x+1,y+1]}}} for y in range(16) for x in range(16) if inlay.getpixel((x,y))[3]]
write("assets/minecraft/models/block/lodestone.json", {"parent":"minecraft:block/block", "textures":{"particle":"minecraft:block/lodestone_side", "side":"minecraft:block/lodestone_side","top":"minecraft:block/lodestone_top","inlay":f"{NS}:block/lodestone_inlay"}, "elements":[cube("#top","#side","#side"),*inlay_elements]})
for name, art in [("teleport_station",normal),("dimensional_teleport_station",dimensional)]:
    # Compact dark stone frame, with the incorporated apparatus visible on the top.
    top = Image.new("RGBA", (16,16), "#555962"); draw = ImageDraw.Draw(top)
    draw.rectangle((0,0,15,15), outline="#b0b1b2"); draw.rectangle((1,1,14,14), outline="#777d87")
    top.alpha_composite(art.resize((12,12), Image.Resampling.NEAREST), (2,2))
    png(f"block/{name}_top",top)
    write(f"assets/{NS}/models/block/{name}.json", {"parent":"minecraft:block/cube", "textures":{"particle":"minecraft:block/chiseled_stone_bricks", "side":"minecraft:block/chiseled_stone_bricks", "top":f"{NS}:block/{name}_top"}, "elements":[cube("#top","#side","#side")]})
    write(f"assets/{NS}/blockstates/{name}.json", {"variants":{"":{"model":f"{NS}:block/{name}"}}})
    write(f"assets/{NS}/models/item/{name}.json", {"parent":f"{NS}:block/{name}"})
    write(f"assets/{NS}/items/{name}.json", {"model":{"type":"minecraft:model","model":f"{NS}:item/{name}"}})

write("data/minecraft/recipe/lodestone.json", {"type":"minecraft:crafting_shaped","category":"redstone","pattern":["CAC","CIC","CCC"],"key":{"C":"minecraft:chiseled_stone_bricks","A":"minecraft:amethyst_shard","I":"minecraft:iron_ingot"},"result":{"id":"minecraft:lodestone","count":1}})
for name, operation in [("teleporter","teleporter"),("dimensional_upgrade","upgrade"),("teleport_station","station"),("dimensional_core","core")]:
    recipe = {"type":f"{NS}:transit","operation":operation}
    if operation == "core": recipe["fabric:load_conditions"] = [{"condition":"fabric:not", "value":{"condition":"fabric:registry_contains", "registry":"minecraft:item", "values":["alexsmobs:dimensional_carver"]}}]
    write(f"data/{NS}/recipe/{name}.json", recipe)
for path in ["data/minecraft/tags/block/mineable/pickaxe.json","data/frozenlib/tags/block/has_pushable_block_entity.json"]:
    write(path,{"replace":False,"values":[f"{NS}:teleport_station",f"{NS}:dimensional_teleport_station"]})

EN = {
"item.lodestone_transit.teleporter":"Teleporter", "item.lodestone_transit.dimensional_teleporter":"Dimensional Teleporter", "item.lodestone_transit.dimensional_core":"Dimensional Core",
"block.lodestone_transit.teleport_station":"Teleport Station", "block.lodestone_transit.dimensional_teleport_station":"Dimensional Teleport Station",
"transit.destination":"Destination: %s", "transit.charges":"Ender Pearls: %s / %s", "transit.dimensional":"Dimensional", "transit.unavailable":"Status: Unavailable",
"transit.destination.unavailable":"Unavailable", "transit.destination.world_spawn":"World Spawn", "transit.destination.last_death":"Last Death",
"transit.name.compass":"%s Compass", "transit.name.teleporter":"%s Teleporter", "transit.name.dimensional_teleporter":"%s Dimensional Teleporter", "transit.name.station":"%s Teleport Station", "transit.name.dimensional_station":"%s Dimensional Teleport Station"}
ES = {
"item.lodestone_transit.teleporter":"Teletransportador", "item.lodestone_transit.dimensional_teleporter":"Teletransportador dimensional", "item.lodestone_transit.dimensional_core":"Núcleo dimensional",
"block.lodestone_transit.teleport_station":"Estación de teletransporte", "block.lodestone_transit.dimensional_teleport_station":"Estación de teletransporte dimensional",
"transit.destination":"Destino: %s", "transit.charges":"Perlas de ender: %s / %s", "transit.dimensional":"Dimensional", "transit.unavailable":"Estado: No disponible",
"transit.destination.unavailable":"No disponible", "transit.destination.world_spawn":"Aparición del mundo", "transit.destination.last_death":"Última muerte",
"transit.name.compass":"Brújula de %s", "transit.name.teleporter":"Teletransportador de %s", "transit.name.dimensional_teleporter":"Teletransportador dimensional de %s", "transit.name.station":"Estación de teletransporte de %s", "transit.name.dimensional_station":"Estación de teletransporte dimensional de %s"}
assert EN.keys() == ES.keys()
for lang, values in [("en_us",EN),("es_es",ES)]: write(f"assets/{NS}/lang/{lang}.json", values)
print("Wrote hand-authored textures, models, recipes, tags and both localizations.")
