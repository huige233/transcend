"""Author the tech pixel-art set. Python 3 + Pillow; no network or image API.

Run from any directory. Originals are archived once before replacement. The
manifest is the explicit scope: artifact equipment and HUD/game logic are untouched.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import argparse
import hashlib
import json
import math
import zipfile

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/transcend'
ART = ROOT / 'art/tech'
OUTPUTS = {}
SPRITES = {}
RANKS = ['e', 'd', 'c', 'b', 'a', 's', 'ex']
METALS = ['crude', 'steel', 'alloy', 'phase', 'exo', 'void', 'singularity']
COLORS = ['#bea78b', '#9db3c2', '#59c9ed', '#aa8aef', '#62e3bf', '#8864d4', '#ffc56b']
INK, DEEP, DARK, MID, EDGE, LIGHT = '#0c1422', '#172536', '#293c50', '#466078', '#7995ac', '#c2d8e5'


def shade(color, factor):
    rgb = tuple(int(color[i:i+2], 16) for i in (1, 3, 5))
    return tuple(max(0, min(255, round(v*factor))) for v in rgb)


class Pixel:
    def __init__(self, size=16, solid=False):
        self.im = Image.new('RGBA', (size, size), INK if solid else (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.im)

    def rect(self, box, color, outline=None):
        self.d.rectangle(box, fill=color, outline=outline)

    def poly(self, points, color, outline=None):
        self.d.polygon(points, fill=color, outline=outline)

    def line(self, points, color, width=1):
        self.d.line(points, fill=color, width=width)

    def dot(self, x, y, color):
        self.d.point((x, y), fill=color)

    def ring(self, box, color, width=1, fill=None):
        self.d.ellipse(box, fill=fill, outline=color, width=width)

    def bevel(self, box, face=DARK):
        x1,y1,x2,y2 = box
        self.rect(box, INK)
        self.rect((x1+1,y1+1,x2-1,y2-1),face)
        self.line([(x1+1,y2-1),(x1+1,y1+1),(x2-1,y1+1)],EDGE)
        self.line([(x2-1,y1+2),(x2-1,y2-1),(x1+2,y2-1)],DEEP)


def save(p, name, folder='item', animate=False):
    path = ASSETS / 'textures' / folder / (name+'.png')
    OUTPUTS[path] = p.im
    SPRITES[folder+'/'+name] = p.im.crop((0,0,p.im.width,p.im.width))
    if animate:
        OUTPUTS[path.with_suffix('.png.mcmeta')] = {'animation': {'frametime': 5, 'interpolate': False}}


def model(name, texture=None, parent='minecraft:item/generated', folder='item', **extra):
    data = {'parent': parent}
    if texture is not None: data['textures'] = {'layer0': 'transcend:item/'+texture}
    data.update(extra)
    OUTPUTS[ASSETS / 'models' / folder / (name+'.json')] = data


def icon(p, kind, c, x=8, y=8):
    """Readable 5x5 symbols: category is encoded by shape as well as color."""
    patterns = {
        'melee': ['....#','...#.','#.#..','.##..','#..#.'],
        'projectile': ['..#..','.###.','.###.','.###.','.#.#.'],
        'explosion': ['#.#.#','.###.','#####','.###.','#.#.#'],
        'fire': ['..#..','..##.','.#.#.','##.##','.###.'],
        'magic': ['..#..','.#.#.','#.#.#','.#.#.','..#..'],
        'environment': ['..#..','.#.#.','#...#','#...#','.###.'],
        'basic': ['.###.','#...#','#.#.#','.#.#.','..#..'],
        'reinforced': ['#####','##.##','##.##','.###.','..#..'],
        'efficient': ['...#.','..#..','.###.','..#..','.#...'],
        't5_mechanical': ['.###.','#.#.#','##.##','#.#.#','.###.'],
        'omni': ['.###.','#.#.#','##.##','#.#.#','.###.'],
    }
    for dy,row in enumerate(patterns[kind]):
        for dx,v in enumerate(row):
            if v == '#': p.dot(x-2+dx,y-2+dy,c)


def weapon(rank, shield=False):
    c = COLORS[rank]; p = Pixel(32)
    if shield:
        p.poly([(7,5),(16,2),(25,5),(25,18),(22,25),(16,30),(10,25),(7,18)],INK)
        p.poly([(8,6),(16,3),(24,6),(24,18),(21,24),(16,28),(11,24),(8,18)],MID)
        p.poly([(10,7),(16,5),(22,7),(22,18),(19,23),(16,25),(13,23),(10,18)],DEEP)
        p.line([(8,7),(8,17),(12,24)],LIGHT)
        p.line([(23,8),(23,18),(20,24),(16,28)],shade(c,.65))
        for r in range(1+rank//2):
            top=8+r*3
            p.line([(12,top+1),(16,top-1),(20,top+1)],c)
        p.poly([(16,11),(20,15),(16,21),(12,15)],shade(c,.5))
        p.poly([(16,12),(18,15),(16,19),(14,15)],c)
        p.dot(15,14,'#f0ffff')
        p.rect((14,26,18,27),c)
        if rank>=3:
            p.rect((4,10,5,18),INK);p.rect((5,11,6,17),c)
            p.rect((26,10,27,18),INK);p.rect((25,11,26,17),c)
        if rank>=5:
            p.line([(3,7),(3,20),(7,25)],c);p.line([(28,7),(28,20),(24,25)],c)
    else:
        # Receiver, separate stock, vented barrel and an under-slung energy cell.
        p.poly([(2,11),(6,11),(8,8),(20,8),(22,10),(29,10),(30,12),(29,16),(21,16),(18,19),(13,19),(11,23),(7,23),(8,17),(2,16)],INK)
        p.rect((3,12,8,15),MID);p.line([(3,12),(6,12)],LIGHT)
        p.poly([(9,9),(19,9),(21,11),(21,16),(18,18),(10,17)],MID)
        p.line([(10,9),(18,9),(20,11)],LIGHT)
        p.rect((12,11,18,14),DEEP);p.rect((13,11,17,12),c)
        p.rect((20,11,28,14),DARK);p.line([(21,11),(28,11)],EDGE)
        for x in range(22,28,2): p.rect((x,12,x,14),INK)
        p.rect((28,10,30,15),INK);p.rect((29,11,29,14),c)
        p.poly([(9,17),(12,18),(10,22),(8,22)],DARK);p.dot(9,19,EDGE)
        p.poly([(14,18),(18,18),(18,22),(14,23)],INK)
        p.rect((15,19,17,21),shade(c,.7));p.rect((15,19,16,20),c)
        p.line([(6,10),(7,8),(10,8)],EDGE)
        if rank>=2:
            p.rect((12,5,19,7),INK);p.rect((13,5,18,6),MID);p.dot(18,5,c)
        if rank>=3:
            p.rect((19,7,25,8),INK);p.rect((20,7,24,7),c)
        if rank>=4:
            p.line([(21,17),(26,17),(28,16)],c)
            p.rect((23,9,26,9),c)
        if rank>=5:
            p.poly([(10,9),(8,6),(10,6),(13,9)],DARK)
            p.line([(10,6),(12,8)],c)
        if rank==6:
            p.line([(19,6),(25,6),(27,8)],c)
            p.rect((22,19,26,20),INK);p.rect((23,19,25,19),c)
    return p


def material(form, tier):
    c=COLORS[tier];p=Pixel()
    if form=='metal':
        p.poly([(2,7),(6,3),(12,3),(14,7),(12,12),(3,12),(1,10)],INK)
        p.poly([(3,7),(6,4),(11,4),(13,7),(11,9),(3,9)],c)
        p.poly([(3,10),(11,10),(13,8),(11,11),(3,11)],shade(c,.55))
        p.line([(4,6),(7,4),(10,4)],shade(c,1.3))
        if tier>=2: p.line([(7,7),(10,7),(11,6)],shade(c,.6))
        if tier>=4: p.dot(12,3,'#eefbff');p.dot(13,2,c)
    elif form=='plate':
        p.poly([(2,5),(10,2),(14,5),(14,11),(6,14),(2,11)],INK)
        p.poly([(3,5),(10,3),(13,5),(6,8)],shade(c,1.05))
        p.poly([(3,6),(6,9),(6,12),(3,10)],shade(c,.6))
        p.poly([(7,9),(13,6),(13,10),(7,13)],shade(c,.8))
        p.line([(4,5),(10,3),(12,4)],shade(c,1.4))
        p.line([(8,10),(11,9)],shade(c,.5))
    elif form=='dust':
        p.poly([(1,12),(3,10),(4,7),(6,8),(8,4),(10,7),(12,8),(14,12),(12,14),(4,14)],INK)
        p.poly([(2,12),(5,9),(6,10),(8,6),(10,9),(12,10),(13,12),(11,13),(4,13)],shade(c,.8))
        for x,y in [(4,11),(7,10),(8,7),(10,11),(6,12),(11,10)]: p.dot(x,y,c)
        p.dot(8,8,shade(c,1.4));p.dot(3,6,c);p.dot(12,5,shade(c,.7))
    else:
        p.poly([(2,4),(7,3),(8,5),(5,9),(1,8)],INK)
        p.poly([(3,5),(6,4),(6,5),(4,7),(2,7)],c)
        p.poly([(9,7),(13,6),(14,11),(10,14),(7,12)],INK)
        p.poly([(10,8),(12,7),(13,10),(10,12),(8,11)],shade(c,.75))
        p.line([(10,8),(11,10),(10,11)],shade(c,1.3))
        p.rect((5,11,6,12),c)
    return p


def gear(kind):
    p=Pixel();c={'blank':'#b0b4ba','normal':'#c7a270','refined':'#a8dae9'}[kind]
    for box in [(6,1,9,14),(1,6,14,9),(3,3,12,12)]:p.rect(box,INK)
    for box in [(6,2,9,13),(2,6,13,9),(4,4,11,11)]:p.rect(box,shade(c,.65))
    p.ring((3,3,12,12),c,2,shade(c,.7))
    p.ring((6,6,9,9),INK,1,DEEP)
    p.line([(4,6),(6,4),(9,4)],shade(c,1.3))
    if kind=='refined':
        for x,y in [(7,2),(12,7),(7,12),(2,7)]:p.dot(x,y,'#57eef5')
    return p


def circuit(stage):
    p=Pixel();p.bevel((2,2,13,13),'#184344')
    for x in [4,7,10]:
        p.rect((x,1,x+1,2),'#d5b579');p.rect((x,13,x+1,14),'#997844')
    for y in [4,7,10]:
        p.dot(1,y,'#d5b579');p.dot(14,y,'#997844')
    if stage!='wafer':
        p.line([(4,4),(4,8),(7,8),(7,11)],'#54bf9d')
        p.line([(6,4),(10,4),(10,7),(11,7)],'#54bf9d')
        p.line([(9,11),(11,11),(11,9)],'#c6b47b')
    if stage=='circuit':
        p.rect((6,6,9,9),INK);p.rect((7,6,8,8),MID);p.dot(7,6,'#b5e5ea')
    if stage=='wafer':
        for x in [4,7,10]:
            for y in [4,7,10]:p.rect((x,y,x+1,y+1),'#7ca5b8')
    return p


def capacitor(level):
    p=Pixel();c=COLORS[min(level+1,6)]
    p.rect((5,1,10,2),INK);p.rect((6,1,9,2),LIGHT)
    p.bevel((3,3,12,13),DARK)
    p.rect((5,5,10,11),DEEP)
    for y in [6,8,10]: p.rect((6,y,9,y),c)
    p.line([(4,4),(4,11)],LIGHT)
    if level>=2:p.rect((1,5,2,11),INK);p.line([(2,6),(2,10)],c);p.rect((13,5,14,11),INK);p.line([(13,6),(13,10)],c)
    if level>=3:p.dot(7,4,'#ffffff');p.dot(8,12,c)
    return p


def core(kind):
    p=Pixel();c={'cell':'#5de5bf','core':'#69cfe7','matrix':'#ffc56b','pilot':'#ae93f2','control':'#77e1cc'}[kind]
    p.poly([(5,1),(10,1),(14,5),(14,10),(10,14),(5,14),(1,10),(1,5)],INK)
    p.poly([(5,2),(10,2),(13,5),(13,10),(10,13),(5,13),(2,10),(2,5)],MID)
    p.poly([(6,4),(9,4),(11,6),(11,9),(9,11),(6,11),(4,9),(4,6)],DEEP)
    p.ring((5,5,10,10),shade(c,.8),1,c);p.rect((6,6,7,7),'#ebffff')
    p.line([(3,5),(5,3),(9,3)],LIGHT)
    if kind in ('core','matrix','pilot'):
        for x,y in [(7,2),(12,7),(7,12),(2,7)]:p.dot(x,y,c)
    if kind=='matrix':
        p.line([(1,1),(4,1),(4,0)],c);p.line([(11,14),(14,14),(14,11)],c)
        for x,y in [(3,3),(12,3),(3,12),(12,12)]:p.dot(x,y,c)
    if kind=='control':
        p.rect((5,5,10,10),INK);p.rect((6,6,9,9),'#449e93');p.line([(6,7),(7,7),(7,8),(9,8)],LIGHT)
    return p


def optical(kind):
    p=Pixel();c='#78e5ed'
    if kind=='glass':
        p.poly([(2,4),(11,2),(14,5),(13,12),(4,14),(1,11)],INK)
        p.poly([(3,5),(11,3),(13,5),(12,11),(4,13),(2,10)],'#366c89')
        p.line([(4,6),(9,4),(10,4)],'#c4ffff');p.line([(4,8),(8,6)],c);p.line([(5,12),(11,10),(12,6)],'#529bb8')
    else:
        p.ring((1,1,14,14),INK,2,DARK);p.ring((3,3,12,12),EDGE,1,'#307b9a')
        p.ring((5,4,10,10),c,1,'#4cadc2');p.line([(5,5),(7,4),(9,4)],'#e6ffff');p.dot(10,10,'#245673')
        if kind=='focusing':
            for x,y in [(7,0),(0,7),(14,7),(7,14)]:p.rect((x,y,x+1,y+1),'#e8c88a')
            p.dot(7,7,'#ffffff')
    return p


def module(kind, c):
    p=Pixel();p.bevel((2,1,13,14),DARK)
    p.rect((4,3,11,11),DEEP)
    p.line([(3,3),(3,10)],shade(c,.65));p.rect((11,3,11,4),c)
    icon(p,kind,c,8,7)
    for x in [4,6,8,10]:p.rect((x,13,x,14),'#c4a46d')
    return p


def gun_part(kind):
    p=Pixel();c='#64d5ef'
    if kind.startswith('ammo'):
        subtype=kind[5:];c={'std':'#65d3ea','scatter':'#a0dc8c','piercing':'#ffcc75','explosive':'#ef826a'}[subtype]
        for x,y in ([(4,4),(9,6)] if subtype=='scatter' else [(6,2)]):
            p.poly([(x,y+3),(x+2,y),(x+4,y+3),(x+4,y+10),(x,y+10)],INK)
            p.poly([(x+1,y+3),(x+2,y+1),(x+3,y+3)],c)
            p.rect((x+1,y+4,x+3,y+8),MID);p.line([(x+1,y+4),(x+1,y+7)],LIGHT)
            p.rect((x+1,y+8,x+3,y+9),c)
            if subtype=='explosive':p.dot(x+2,y+5,'#f8e4b2');p.dot(x+2,y+6,c)
            if subtype=='piercing':p.line([(x+2,y+3),(x+2,y+7)],c)
    elif kind.startswith('barrel') or kind=='muzzle_silencer':
        long=kind!='barrel_std';x1=1 if long else 3
        p.poly([(x1,7),(x1+2,4),(12,4),(14,6),(14,9),(12,11),(x1,11)],INK)
        p.rect((x1+1,6,12,9),MID);p.line([(x1+2,5),(11,5)],LIGHT)
        p.rect((12,5,13,10),DEEP);p.dot(13,7,c)
        for x in range(x1+3,11,3):p.line([(x,6),(x,9)],INK)
        if kind=='barrel_accel':
            for x in [4,7,10]:p.rect((x,4,x,10),'#a391ef');p.dot(x,5,'#ebe5ff')
        if kind=='barrel_long':p.line([(2,10),(10,10)],c)
        if kind=='muzzle_silencer':p.rect((3,6,10,9),DARK);p.line([(4,6),(9,6)],EDGE)
    elif kind=='muzzle_scope':
        p.rect((4,10,10,12),INK);p.rect((6,9,8,10),EDGE)
        p.rect((2,4,12,8),INK);p.rect((3,5,11,7),MID);p.line([(4,5),(8,5)],LIGHT)
        p.ring((9,3,14,9),INK,1,'#397795');p.dot(11,5,'#c3fbff');p.dot(12,6,c)
    else:
        p=core('pilot');p.line([(7,2),(7,12)],'#d5c6ff');p.line([(2,7),(12,7)],'#d5c6ff');p.dot(7,7,'#ffffff')
    return p


def cosmic(kind, loaded=False):
    p=Pixel(32);c='#b497f4'
    if kind=='container':
        p.poly([(10,2),(21,2),(26,7),(26,25),(21,29),(10,29),(5,25),(5,7)],INK)
        p.poly([(11,3),(20,3),(24,7),(24,24),(20,27),(11,27),(7,24),(7,7)],MID)
        p.rect((10,7,21,23),DEEP);p.line([(8,8),(8,23)],LIGHT)
        p.ring((11,10,20,19),'#618ca5',1,'#182c41')
        if loaded:
            p.ring((10,10,21,21),c,2);p.ring((13,13,18,18),INK,1,'#030610');p.dot(16,12,'#f5deff')
        for y in [6,24]:p.rect((10,y,21,y+1),'#85d8e5')
        for x in [6,24]:p.rect((x,11,x+1,20),'#b5e6ee')
        return p
    if kind=='seed':
        p.ring((6,6,25,25),INK,2,'#22283b')
        p.ring((8,8,23,23),'#737b9d',1,'#222143');p.ring((11,11,20,20),'#a591d3',1,'#050818')
        p.line([(9,14),(12,10),(17,9),(21,12)],'#d5c2f6')
        p.line([(12,21),(18,22),(22,18)],'#6556a1')
        p.dot(16,5,c);p.dot(25,14,c)
        return p
    # Accretion disk and dark centre; these are hand-drawn, not blurred glows.
    p.ring((5,5,26,26),'#4e427b',1)
    p.ring((8,8,23,23),'#c1a8fa',2,INK)
    p.poly([(1,20),(9,14),(22,11),(30,11),(24,15),(10,19)],'#51457e')
    p.line([(2,20),(10,16),(23,12),(29,12)],'#edcb94',2)
    p.line([(5,20),(15,17),(25,14)],'#a084e7')
    p.ring((11,11,20,20),'#060b17',2,'#040712')
    p.line([(11,11),(14,9),(18,9)],'#f8e9ff')
    p.dot(3,7,'#b8dcf9');p.dot(27,23,'#b8dcf9')
    return p


def manual(tier):
    p=Pixel(32);c=COLORS[tier]
    p.poly([(5,3),(25,3),(28,6),(28,28),(7,29),(3,26),(3,6)],INK)
    p.rect((6,5,25,26),DARK);p.rect((7,27,26,28),'#bbcbd2')
    p.rect((4,6,6,25),shade(c,.65));p.line([(7,5),(24,5),(26,7)],EDGE)
    p.rect((9,7,23,23),DEEP);p.line([(10,8),(21,8)],c)
    p.ring((12,11,21,20),c,2);p.rect((15,14,18,17),INK)
    for x,y in [(16,9),(16,21),(10,15),(22,15)]:p.rect((x,y,x+1,y+1),c)
    for i in range(tier+1):p.rect((9+i*2,24,9+i*2,25),c)
    p.line([(8,10),(8,22)],MID)
    if tier>=4:p.dot(24,9,c);p.dot(24,21,c)
    return p


def research(assist=False):
    p=Pixel();p.bevel((2,1,13,14),DARK)
    for x in [4,6,8,10]:p.dot(x,14,'#d4b57e')
    if assist:
        p.ring((4,3,11,10),'#7ad5dd',1,DEEP)
        p.line([(7,3),(7,10)],'#5b9aaf');p.line([(4,6),(11,6)],'#5b9aaf')
        p.rect((6,5,9,8),'#91ebe5');p.dot(6,5,'#edffff')
    else:
        p.rect((4,3,11,11),'#274761');p.line([(5,4),(9,4),(9,6),(6,6),(6,9),(10,9)],'#81b7cb')
        p.dot(5,4,'#e0f4fc')
    return p


def machine(name, surface='front'):
    p=Pixel(solid=True)
    c=('#eea167' if name=='fire_generator' else '#8ed5e7' if name=='wind_generator'
       else '#85debc' if name=='assembly_table' else '#b59aed' if 'universe' in name or 'cosmic' in name
       else '#e9bf7b' if 'breeder' in name else '#d08cdb' if 'phantom' in name else '#75cde5')
    # One-pixel seams, inset panels and specular edge lighting; no random noise.
    p.rect((1,1,14,14),DARK);p.line([(1,14),(1,1),(14,1)],EDGE)
    p.line([(14,2),(14,14),(2,14)],DEEP)
    p.rect((3,3,12,12),DEEP)
    for x,y in [(2,2),(13,2),(2,13),(13,13)]:p.dot(x,y,LIGHT)
    if surface=='bottom':
        p.ring((4,4,11,11),MID,2,INK);p.rect((6,6,9,9),c)
        for x in [4,10]:p.rect((x,1,x+1,2),MID);p.rect((x,13,x+1,14),MID)
    elif surface=='side':
        p.rect((3,3,12,4),MID)
        for y in [6,8,10]:p.line([(4,y),(11,y)],INK);p.line([(4,y+1),(10,y+1)],MID)
        p.dot(11,3,c);p.rect((3,13,6,13),shade(c,.7))
    elif surface=='top':
        if name=='assembly_table':
            p.rect((3,3,12,12),'#315353')
            for x in [5,8,11]:p.line([(x,4),(x,11)],'#427373')
            for y in [5,8,11]:p.line([(4,y),(11,y)],'#427373')
            p.line([(5,9),(9,5),(10,6),(6,10)],LIGHT);p.rect((9,4,11,5),MID)
        else:
            p.ring((3,3,12,12),MID,2,INK);p.ring((5,5,10,10),c,1,DEEP)
            p.rect((7,6,8,9),shade(c,.7))
    elif 'storage' in name:
        level=['basic','advanced','quantum','phantom'].index(name.split('_')[-1])
        c=COLORS[[1,2,4,5][level]]
        p.rect((4,3,11,12),INK);p.rect((5,4,10,11),DARK)
        for y in [5,7,9]:p.rect((6,y,9,y),c)
        p.line([(3,5),(3,10)],EDGE);p.line([(12,5),(12,10)],EDGE)
        for i in range(level+1):p.dot(5+i*2,13,c)
    elif name=='assembly_table':
        p.rect((3,4,8,9),INK);p.rect((4,5,7,7),'#438975');p.dot(5,5,'#b5f3dc')
        for y in [4,6,8]:p.rect((10,y,12,y),MID)
        p.rect((3,11,12,12),INK);p.rect((4,11,9,11),EDGE);p.dot(11,11,c)
    elif name=='fire_generator':
        p.rect((3,4,12,11),INK);p.rect((4,5,11,10),'#542d2d')
        p.poly([(5,10),(5,7),(7,8),(8,5),(10,8),(10,10)],'#e87c42')
        p.rect((7,8,8,10),'#ffdc7f')
        for x in [5,8,11]:p.line([(x,4),(x,11)],MID)
        p.rect((5,13,10,13),'#c39163')
    elif name=='wind_generator':
        p.ring((3,3,12,12),EDGE,1,INK)
        p.poly([(7,7),(5,4),(7,3),(9,6)],'#c3e4ec');p.poly([(8,7),(11,5),(12,7),(9,9)],'#84baca')
        p.poly([(8,8),(10,11),(8,12),(6,9)],'#c3e4ec');p.poly([(7,8),(4,10),(3,8),(6,6)],'#84baca')
        p.rect((7,7,8,8),c)
    elif name=='creative_generator':
        p.ring((3,5,8,10),'#c1a4f2',1,INK);p.ring((7,5,12,10),'#70e8db',1,INK)
        p.line([(5,6),(10,9)],'#f1d1ff');p.dot(11,4,'#f9e5b2')
    elif 'universe' in name:
        active=name.endswith('active') and not name.endswith('inactive')
        p.ring((3,3,12,12),MID,2,INK);p.ring((5,5,10,10),c if active else '#695b8c',1,INK)
        if active:
            p.line([(2,10),(6,8),(10,6),(13,5)],'#f4cf88');p.rect((6,6,9,9),INK);p.dot(6,5,'#f0e1ff')
        p.dot(7,13,c if active else MID)
    elif 'breeder' in name:
        p.rect((5,3,10,12),INK);p.rect((6,4,9,11),'#394551')
        for y in [4,7,10]:p.rect((4,y,11,y),'#c0a675')
        p.rect((7,6,8,9),INK);p.dot(8,6,'#c1a4f2');p.dot(12,11,'#90e5c7')
    elif name=='research_station':
        p.rect((3,3,12,9),INK);p.rect((4,4,11,8),'#24596d')
        p.line([(5,7),(6,6),(7,7),(9,5),(10,5)],'#8fe1e9')
        p.rect((4,11,11,12),MID)
        for x in [5,7,9]:p.dot(x,11,LIGHT)
    elif name=='research_processor':
        for y in [4,7,10]:
            p.rect((3,y,12,y+1),MID);p.rect((4,y,8,y),EDGE);p.dot(11,y,'#7de6c8')
    elif name=='research_quantum_computer':
        p.ring((3,3,12,12),'#6eb5ca',1,INK);p.line([(5,5),(10,10)],'#70a5b7');p.line([(10,5),(5,10)],'#70a5b7')
        for x,y in [(5,5),(10,5),(5,10),(10,10)]:p.rect((x,y,x+1,y+1),'#a7f4e6')
        p.rect((7,7,8,8),'#f2ffff')
    elif name=='research_cosmic_simulator':
        p.ring((3,5,12,10),'#bfa2ea',1,INK);p.ring((5,3,10,12),'#6e93c6',1)
        p.rect((7,7,8,8),'#f5dfb5');p.dot(4,6,'#f1d9ff');p.dot(10,11,'#88e1ed')
    return p


def assemble():
    for i,rank in enumerate(RANKS):
        for base,shield in [('particle_gun',False),('phase_shield',True)]:
            save(weapon(i,shield),base+'_'+rank)
            model(base+'/'+rank,base+'_'+rank,parent='minecraft:item/handheld' if not shield else 'minecraft:item/generated')
    for base,shield in [('particle_gun',False),('phase_shield',True)]:
        save(weapon(2,shield),base)
        model(base,base,parent='minecraft:item/handheld' if not shield else 'minecraft:item/generated',overrides=[
            {'predicate':{'transcend:tech_rank':i+1},'model':'transcend:item/'+base+'/'+rank} for i,rank in enumerate(RANKS)])
    for i in range(7):
        for form in ['metal','plate','dust','scrap']:
            name='tech_'+form+'_'+METALS[i];save(material(form,i),name);model(name,name)
        name='mechanical_knowledge_t'+str(i+1);save(manual(i),name);model(name,name)
    for name,kind in [('tech_part_gear_blank','blank'),('tech_part_gear','normal'),('tech_part_gear_refined','refined')]:save(gear(kind),name)
    for stage in ['circuit','circuit_printed','circuit_wafer']:save(circuit(stage.split('_')[-1]),'tech_part_'+stage)
    for name,kind in [('tech_part_glass_phase','glass'),('tech_part_lens_phase','lens'),('tech_part_focusing','focusing')]:save(optical(kind),name)
    for i,name in enumerate(['standard_capacitor','portable_capacitor','portable_capacitor_advanced','portable_capacitor_ghost']):save(capacitor(i),name)
    save(capacitor(1),'tech_part_capacitor');save(capacitor(2),'tech_part_capacitor_core')
    for kind in ['cell','core','matrix']:save(core(kind),'tech_energy_'+kind)
    save(core('pilot'),'tech_core_pilot');save(core('control'),'tech_control_core');model('tech_control_core','tech_control_core')
    save(material('dust',2),'tech_dust_conductive')
    for i,name in enumerate(['tech_capacitor_fragment','tech_core_fragment','tech_cell_shard']):
        p=material('scrap',[2,6,4][i]);p.line([(8,8),(10,9),(10,11)],COLORS[[2,6,4][i]]);save(p,name);model(name,name)
    for name in ['blank_research_component','research_assist_unit']:save(research(name=='research_assist_unit'),name)
    for name in ['ammo_std','ammo_scatter','ammo_piercing','ammo_explosive','barrel_std','barrel_long','barrel_accel','muzzle_silencer','muzzle_scope','sirius']:
        save(gun_part(name),name)
    save(gun_part('sirius'),'sirius_module');save(research(True),'gun_module')
    modules={'basic':('basic','#6fc9e5'),'reinforced':('reinforced','#f0b774'),'efficient':('efficient','#9be0ac'),
        't5_mechanical':('t5_mechanical','#d4c0f5'),'b_melee':('melee','#e1cbb4'),'b_projectile':('projectile','#8fcedf'),
        'b_explosion':('explosion','#f2b362'),'b_fire':('fire','#f18667'),'b_magic':('magic','#c29af0'),
        'b_environment':('environment','#83dcb0'),'a_omni':('omni','#86e3d0'),'s_omni':('omni','#c19af0'),'ex_omni':('omni','#ffd282')}
    for i,(name,(kind,c)) in enumerate(modules.items()):
        p=module(kind,c)
        if name.endswith('omni'):
            for n in range(['a_omni','s_omni','ex_omni'].index(name)+1):p.dot(5+2*n,11,c)
        save(p,'shield_module_'+name);model('shield_module/'+name,'shield_module_'+name)
    save(module('basic','#99c3da'),'shield_module')
    save(cosmic('seed'),'black_hole_seed');save(cosmic('container'),'magnetic_confinement_container')
    save(cosmic('container',True),'magnetic_confinement_container_loaded')
    model('magnetic_confinement_container_loaded','magnetic_confinement_container_loaded')
    model('magnetic_confinement_container','magnetic_confinement_container',overrides=[
        {'predicate':{'transcend:loaded':1},'model':'transcend:item/magnetic_confinement_container_loaded'}])
    p=cosmic('singularity');save(p,'mini_singularity')
    p=core('pilot');p.rect((5,5,10,10),DEEP);p.line([(5,7),(10,7)],'#d0b3f4');save(p,'phantom_energy_block')
    machines=['assembly_table','black_hole_seed_breeder','mini_universe_generator','mini_universe_generator_inactive','mini_universe_generator_active',
        'fire_generator','wind_generator','creative_generator','research_station','research_processor','research_quantum_computer','research_cosmic_simulator',
        'long_storage_basic','long_storage_advanced','long_storage_quantum','long_storage_phantom']
    for name in machines:
        for surface in ['front','side','top','bottom']:
            save(machine(name,surface),name+('' if surface=='front' else '_'+surface),'block')
        tex='transcend:block/'+name
        model(name,folder='block',parent='minecraft:block/cube',textures={'north':tex,'south':tex+'_side','east':tex+'_side','west':tex+'_side','up':tex+'_top','down':tex+'_bottom','particle':tex+'_side'})
        if name not in ('mini_universe_generator_inactive','mini_universe_generator_active'):model(name,parent='transcend:block/'+name)
    # Existing flat generator sprites are kept in sync for tools/resource consumers.
    for name in ['fire_generator','wind_generator','creative_generator']:save(machine(name),name)
    # The tech oxygen bar gets its own small atlas instead of tinting vanilla bubbles.
    p=Pixel(32)
    for offset,filled in [(0,True),(9,False)]:
        p.poly([(offset+2,0),(offset+6,0),(offset+8,2),(offset+8,6),(offset+6,8),(offset+2,8),(offset,6),(offset,2)],INK)
        p.poly([(offset+2,1),(offset+6,1),(offset+7,2),(offset+7,6),(offset+6,7),(offset+2,7),(offset+1,6),(offset+1,2)],'#83dfe6' if filled else MID)
        p.rect((offset+2,2,offset+6,6),DEEP)
        if filled:
            p.rect((offset+3,3,offset+5,5),'#b6f6f4');p.dot(offset+2,2,'#e8ffff')
    p.im=p.im.crop((0,0,32,16));save(p,'tech_oxygen','gui')


def contact_sheet():
    names=list(SPRITES)
    cols=10;cellw=124;cellh=103
    im=Image.new('RGB',(cols*cellw,70+math.ceil(len(names)/cols)*cellh),'#101a28');d=ImageDraw.Draw(im)
    d.text((18,14),'TRANSCEND / TECHNOLOGY MATERIAL LIBRARY',fill='#d0e7ee',font=ImageFont.truetype('C:/Windows/Fonts/consola.ttf',22))
    d.text((18,44),'Pixel masters / nearest-neighbour preview / E - EX',fill='#789bb0')
    for i,name in enumerate(names):
        x=(i%cols)*cellw;y=70+(i//cols)*cellh
        d.rectangle((x+3,y+2,x+cellw-4,y+cellh-3),fill='#172536')
        sprite=SPRITES[name].resize((64,64),Image.Resampling.NEAREST);im.paste(sprite,(x+30,y+5),sprite)
        label=name.split('/')[-1]
        d.text((x+6,y+73),label[:19],fill='#d4e3e8');d.text((x+6,y+86),label[19:38],fill='#91aebe')
    im.save(ART/'overview.png')
    # Short review sheet: actual-size comparison and the full equipment rank progression.
    im=Image.new('RGB',(1040,610),'#101a28');d=ImageDraw.Draw(im)
    titlefont=ImageFont.truetype('C:/Windows/Fonts/consola.ttf',23)
    d.text((24,16),'TRANSCEND | TECHNOLOGY REDRAW',fill='#e5f1f4',font=titlefont)
    d.text((24,50),'Graphite housings / machined edges / distinct function silhouettes',fill='#8daebb')
    rows=[['item/particle_gun_'+r for r in RANKS],['item/phase_shield_'+r for r in RANKS],
          ['item/tech_'+f+'_singularity' for f in ['metal','plate','dust','scrap']]+['item/black_hole_seed','item/magnetic_confinement_container_loaded','item/mini_singularity'],
          ['block/'+n for n in ['assembly_table','research_station','research_quantum_computer','fire_generator','wind_generator','black_hole_seed_breeder','mini_universe_generator_active']]]
    for row,names in enumerate(rows):
        for i,name in enumerate(names):
            x=24+i*144;y=85+row*125
            sprite=SPRITES[name].resize((80,80),Image.Resampling.NEAREST);im.paste(sprite,(x+20,y),sprite)
            label=RANKS[i].upper() if row<2 else name.split('/')[-1]
            d.text((x,y+86),label[:21],fill='#c5e1e9');d.text((x,y+99),label[21:42],fill='#8daebb')
    # Show modules small so legibility can be reviewed at inventory scale.
    for i,name in enumerate(['b_melee','b_projectile','b_explosion','b_fire','b_magic','b_environment','a_omni','s_omni','ex_omni']):
        sprite=SPRITES['item/shield_module_'+name].resize((32,32),Image.Resampling.NEAREST);im.paste(sprite,(24+i*110,571),sprite);d.text((62+i*110,582),name,fill='#9cb7c7')
    im.save(ART/'preview.png')
    # Orthographic inventory-style preview of the actual front/side/top sprites.
    names=['assembly_table','research_station','research_processor','research_quantum_computer',
           'research_cosmic_simulator','fire_generator','wind_generator','creative_generator',
           'black_hole_seed_breeder','mini_universe_generator_active','long_storage_basic',
           'long_storage_advanced','long_storage_quantum','long_storage_phantom']
    im=Image.new('RGB',(1008,430),'#101a28');d=ImageDraw.Draw(im)
    d.text((18,14),'MACHINES / SIX-FACE MATERIALS',fill='#d0e7ee',font=titlefont)
    for i,name in enumerate(names):
        ox=10+(i%7)*144;oy=55+(i//7)*180
        faces=[(name+'_top',(64,0),(3,1.5),(-3,1.5),1.0),
               (name,(16,24),(3,1.5),(0,3),.9),
               (name+'_side',(64,48),(3,-1.5),(0,3),.65)]
        for key,origin,ux,uy,lighting in faces:
            sprite=SPRITES['block/'+key]
            for sy in range(16):
                for sx in range(16):
                    rgb=sprite.getpixel((sx,sy))[:3];rgb=tuple(round(v*lighting) for v in rgb)
                    def point(a,b):return (round(ox+origin[0]+a*ux[0]+b*uy[0]),round(oy+origin[1]+a*ux[1]+b*uy[1]))
                    d.polygon([point(sx,sy),point(sx+1,sy),point(sx+1,sy+1),point(sx,sy+1)],fill=rgb)
        d.text((ox+4,oy+109),name[:21],fill='#bdd6e0');d.text((ox+4,oy+122),name[21:42],fill='#829eaf')
    im.save(ART/'machines.png')


def check():
    errors=[]
    for path,payload in OUTPUTS.items():
        if not path.exists():errors.append(str(path));continue
        if isinstance(payload,Image.Image):
            im=Image.open(path);im.load()
            if im.mode!='RGBA' or im.size!=payload.size:errors.append(str(path)+' format')
        elif path.suffix=='.json':
            data=json.loads(path.read_text(encoding='utf-8'))
            for texture in data.get('textures',{}).values():
                if texture.startswith('transcend:') and not (ASSETS/'textures'/(texture.split(':')[1]+'.png')).exists():errors.append(texture)
            refs=[data.get('parent','')]+[o['model'] for o in data.get('overrides',[])]
            for ref in refs:
                if ref.startswith('transcend:') and not (ASSETS/'models'/(ref.split(':')[1]+'.json')).exists():errors.append(ref)
    if errors:raise SystemExit('\n'.join(errors))
    print(f'Validated {len(SPRITES)} PNG textures and {len(OUTPUTS)-len(SPRITES)} model/metadata files.')


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
    assemble()
    if args.check:check();return
    ART.mkdir(parents=True,exist_ok=True)
    backup=ART/'originals.zip'
    if not backup.exists():
        with zipfile.ZipFile(backup,'w',zipfile.ZIP_DEFLATED) as z:
            for path in OUTPUTS:
                if path.exists():z.write(path,path.relative_to(ROOT))
    for path,payload in OUTPUTS.items():
        path.parent.mkdir(parents=True,exist_ok=True)
        if isinstance(payload,Image.Image):payload.save(path)
        else:path.write_text(json.dumps(payload,indent=2)+'\n',encoding='utf-8')
    manifest={'style':'Original hand-authored pixel art: graphite, machined edges, functional silhouettes',
              'textures':[{'path':str(p.relative_to(ROOT)).replace('\\','/'),'size':list(v.size),
                           'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p,v in OUTPUTS.items() if isinstance(v,Image.Image)],
              'models':[str(p.relative_to(ROOT)).replace('\\','/') for p,v in OUTPUTS.items() if not isinstance(v,Image.Image)]}
    (ART/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
    contact_sheet();check()


if __name__=='__main__':main()
