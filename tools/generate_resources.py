"""Reproducible 16x16 icons and data. Integer pixel work; no resizing in source assets."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json, colorsys

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
NS='monsterperspective'
def write(path,value):
    p=RES/path;p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def item(i):return {'item':f'{NS}:{i}'}
def vanilla(i):return {'item':'minecraft:'+i}
def recipe(i,ingredients,count=1):
    write(Path('data')/NS/'recipe'/f'{i}.json',{'type':'minecraft:crafting_shapeless','ingredients':ingredients,'result':{'id':f'{NS}:{i}','count':count}})

names={'flesh_part':'肉傀儡部件','bone_part':'骨傀儡部件','soul_glass':'灵魂沙玻璃','soul_bottle':'灵魂瓶','filled_soul_bottle':'装满的灵魂瓶','teleport_scroll':'传送卷轴','binding_pearl':'绑定珍珠','infinite_pearl':'无限珍珠','venom_bottle':'毒液瓶'}
weaves=[('fortune','时运',2,'emerald'),('looting','抢夺',2,'gold_ingot'),('silk_touch','精准采集',1,'amethyst_shard'),('sharpness','锋利',3,'iron_ingot'),('protection','保护',3,'iron_chestplate'),('feather_falling','摔落保护',4,'feather'),('fire_protection','火焰保护',3,'magma_cream'),('unbreaking','耐久',3,'obsidian')]
for key,title,lvl,material in weaves:names['silk_'+key]=f'{title}蛛丝布 {lvl}'
lang={f'item.{NS}.{k}':v for k,v in names.items()}
lang.update({'key.monsterperspective.archive':'打开永夜档案','key.categories.monsterperspective':'怪物视角','golem_material.monsterperspective.flesh':'血肉','golem_material.monsterperspective.bone':'魂骨','material.monsterperspective.flesh':'血肉','material.monsterperspective.bone':'魂骨'})
write(Path('assets')/NS/'lang/zh_cn.json',lang)
write(Path('assets')/NS/'lang/en_us.json',{'key.monsterperspective.archive':'Open Nocturne Archive','key.categories.monsterperspective':'Monster Perspective',**{f'item.{NS}.{k}':k.replace('_',' ').title() for k in names}})
recipe('flesh_part',[{'tag':NS+':meat'},vanilla('slime_ball')],2)
recipe('soul_glass',[vanilla('soul_sand'),vanilla('glass')],4)
recipe('soul_bottle',[item('soul_glass'),vanilla('glass_bottle')],2)
recipe('bone_part',[vanilla('bone'),vanilla('bone'),item('filled_soul_bottle')],2)
recipe('teleport_scroll',[vanilla('ender_pearl'),vanilla('paper'),vanilla('ink_sac')])
recipe('binding_pearl',[vanilla('ender_pearl'),vanilla('lead'),vanilla('amethyst_shard')])
recipe('infinite_pearl',[vanilla('ender_pearl')]*4+[vanilla('diamond'),vanilla('echo_shard')])
for key,title,lvl,material in weaves:recipe('silk_'+key,[vanilla('string')]*4+[vanilla(material),vanilla('lapis_lazuli')])
write(Path('data')/NS/'recipe/silk_enchant.json',{'type':NS+':silk_enchant','category':'equipment'})
write(Path('data')/NS/'tags/item/meat.json',{'replace':False,'values':['minecraft:'+i for i in ['beef','cooked_beef','porkchop','cooked_porkchop','chicken','cooked_chicken','mutton','cooked_mutton','rabbit','cooked_rabbit','rotten_flesh']]+[{'id':'#c:foods/raw_meat','required':False},{'id':'#c:foods/cooked_meat','required':False},{'id':'#c:foods/meat','required':False}]})
write(Path('data')/'curios/tags/item/charm.json',{'replace':False,'values':[NS+':filled_soul_bottle']})
write(Path('data')/'curios/slots/charm.json',{'size':1,'operation':'ADD'})
write(Path('data')/'vmdiecore/curios/entities/players.json',{'entities':['minecraft:player'],'slots':['charm']})
# Register real modular-golem materials. Their native part recipes and assembly table use these entries.
write(Path('data')/'vmdiecore/modulargolems_config/materials/racial.json',{
    'ingredients':{NS+':flesh':item('flesh_part'),NS+':bone':item('bone_part')},
    'repairIngredients':{NS+':flesh':{'tag':NS+':meat'},NS+':bone':vanilla('bone')},
    'partLimitation':{},'modifiers':{NS+':flesh':{},NS+':bone':{}},
    'stats':{NS+':flesh':{'modulargolems:attack':8.0,'modulargolems:max_health':70.0,'modulargolems:regen':2.0},NS+':bone':{'modulargolems:attack':12.0,'modulargolems:max_health':50.0,'modulargolems:speed':0.1}}})
# Native part conversion recipes are explicit, discoverable, and preserve the material component.
for mat in ['flesh','bone']:
 for part in ['metal_golem_body','metal_golem_arm','metal_golem_legs','humanoid_golem_body','humanoid_golem_arms','humanoid_golem_legs','dog_golem_body','dog_golem_legs']:
  write(Path('data')/NS/'recipe'/f'{mat}_{part}.json',{'neoforge:conditions':[{'type':'neoforge:mod_loaded','modid':'modulargolems'}],'type':'minecraft:crafting_shapeless','ingredients':[{'item':'modulargolems:'+part},item(mat+'_part')],'result':{'id':'modulargolems:'+part,'components':{'modulargolems:part_material':NS+':'+mat}}})
# Optional integration recipes are guarded before unknown item IDs are decoded.
for device,extra in [('travel_anchor','obsidian'),('staff_of_travelling','blaze_rod')]:
 write(Path('data')/NS/'recipe'/f'enderio_{device}.json',{'neoforge:conditions':[{'type':'neoforge:mod_loaded','modid':'enderio'}],'type':'minecraft:crafting_shapeless','ingredients':[item('infinite_pearl'),vanilla('iron_block'),vanilla('redstone_block'),vanilla(extra)],'result':{'id':'enderio:'+device}})

def image():return Image.new('RGBA',(16,16),(0,0,0,0))
icons={}
# Thick cut flesh held in a stitched brass frame; warm shadow and sparse pale highlights.
im=image();d=ImageDraw.Draw(im)
d.polygon([(4,2),(11,2),(14,5),(13,12),(10,14),(3,12),(1,6)],fill='#34302c')
d.polygon([(4,3),(10,3),(13,6),(12,11),(9,13),(3,11),(2,6)],fill='#704039')
d.polygon([(4,4),(9,3),(12,6),(10,11),(5,12),(3,8)],fill='#aa6255')
d.polygon([(4,4),(8,4),(9,6),(5,8),(3,7)],fill='#cf8e78')
d.line([(4,10),(7,9),(10,10),(12,7)],fill='#602f33',width=1)
for x,y in [(4,9),(6,8),(9,9),(11,7)]:d.point((x,y),fill='#e5cc9c')
d.line([(1,4),(3,3),(5,3)],fill='#b5a07c');d.line([(11,12),(13,11),(14,9)],fill='#988969')
icons['flesh_part']=im
# Ribcage with a luminous soul core, distinguished from a single vanilla bone.
im=image();d=ImageDraw.Draw(im)
d.line([(7,1),(7,14)],fill='#77765f',width=3);d.line([(7,2),(7,13)],fill='#f0e9c7')
for y in [3,6,9]:
 d.line([(6,y),(3,y-1),(1,y+1),(3,y+3),(6,y+2)],fill='#979780')
 d.line([(8,y),(11,y-1),(14,y+1),(12,y+3),(8,y+2)],fill='#d2d0ac')
 d.point((2,y),fill='#ece6c7');d.point((12,y),fill='#fff6d7')
d.rectangle((6,5,9,9),fill='#244b50');d.rectangle((7,6,8,8),fill='#70d9d1');d.point((7,6),fill='#d6fff5')
icons['bone_part']=im
# Bottle silhouette uses pale cyan glass, cork and separate inner contents.
for key,fluid in [('soul_bottle',None),('filled_soul_bottle','#6cddd0'),('venom_bottle','#a6c760')]:
 im=image();d=ImageDraw.Draw(im)
 d.rectangle((6,1,9,3),fill='#6b4b42');d.line((6,1,9,1),fill='#cfaa7a')
 d.polygon([(5,4),(10,4),(10,6),(13,9),(13,13),(11,15),(4,15),(2,13),(2,9),(5,6)],fill='#345361')
 d.polygon([(6,4),(9,4),(9,7),(12,10),(12,13),(10,14),(5,14),(3,12),(3,10),(6,7)],fill='#7897a5')
 d.polygon([(6,6),(9,7),(11,10),(10,13),(5,13),(4,11)],fill='#263c4c')
 if fluid:
  d.polygon([(4,10),(7,9),(11,10),(11,12),(9,13),(5,13)],fill=fluid)
  if key=='filled_soul_bottle':d.polygon([(7,5),(9,7),(8,10),(6,9),(6,7)],fill='#c2fff2');d.point((8,8),fill='#264550')
  else:d.point((7,11),fill='#e8e899');d.point((10,11),fill='#64783c')
 d.line([(5,7),(4,9),(4,10)],fill='#d9f3e8');d.point((5,13),fill='#b6d5d1')
 icons[key]=im
im=image();d=ImageDraw.Draw(im);d.polygon([(3,1),(12,1),(14,4),(12,14),(3,14),(1,11)],fill='#31464b');d.polygon([(4,2),(11,2),(12,4),(11,13),(4,12),(2,10)],fill='#72b8af');d.line([(4,3),(10,3),(11,5)],fill='#d6f8db');d.line([(4,4),(6,6),(4,10)],fill='#427b7e');d.line([(7,5),(9,6),(7,8),(9,10)],fill='#a9e5c8');icons['soul_glass']=im
# Scroll: ivory paper around an obsidian seal.
im=image();d=ImageDraw.Draw(im);d.polygon([(4,1),(13,1),(14,4),(11,5),(11,13),(3,14),(1,11),(3,9),(3,3)],fill='#5b4b50');d.polygon([(4,2),(12,2),(12,4),(10,4),(10,12),(4,13),(3,11),(4,10)],fill='#d1bc9a');d.line([(5,3),(10,3)],fill='#f5e4bb');d.line([(5,5),(9,5)],fill='#927d78');d.line([(5,7),(7,7)],fill='#927d78');d.rectangle((7,8,11,11),fill='#513b68');d.point((9,9),fill='#d4b8ef');icons['teleport_scroll']=im
for key in ['binding_pearl','infinite_pearl']:
 im=image();d=ImageDraw.Draw(im);d.polygon([(6,2),(10,2),(13,5),(14,9),(11,13),(6,14),(2,10),(2,6)],fill='#252735');d.polygon([(6,3),(10,3),(12,6),(12,10),(9,12),(5,11),(3,8),(4,5)],fill='#3b6a74' if key=='binding_pearl' else '#695185');d.polygon([(6,4),(9,4),(7,6),(4,7)],fill='#86d6c4' if key=='binding_pearl' else '#c7a4e2');d.polygon([(7,6),(10,6),(11,9),(8,11),(6,9)],fill='#1e394b')
 if key=='binding_pearl':d.line([(1,3),(3,1),(5,3),(3,5),(1,3)],fill='#d5b67a');d.line([(11,11),(13,9),(15,11),(13,13),(11,11)],fill='#b79569')
 else:d.line([(1,9),(3,6),(6,6),(10,10),(13,10),(15,7)],fill='#e4cf95');d.point((14,6),fill='#fff0b3')
 icons[key]=im
palette=['#71c5ac','#d6b277','#c09bdc','#a1b4c9','#8fbac9','#b6d1bf','#e0a275','#c1b9db']
for idx,(key,*_) in enumerate(weaves):
 im=image();d=ImageDraw.Draw(im);d.polygon([(3,2),(12,2),(13,12),(10,14),(2,13),(2,4)],fill='#343144');d.polygon([(4,3),(11,3),(12,11),(10,13),(3,12),(3,5)],fill=palette[idx]);d.line([(4,3),(10,3)],fill='#ede5cf');d.line([(4,5),(11,5)],fill='#716d83');d.line([(4,11),(10,12)],fill='#716d83')
 # A distinct tiny rune per enchantment, not a color-only variant.
 rune=[[(5,8),(7,6),(9,8),(7,10),(5,8)],[(5,6),(9,10),(9,6),(5,10)],[(5,8),(9,8),(7,6),(7,10)],[(5,10),(9,6),(8,6)],[(5,6),(9,6),(9,9),(7,11),(5,9),(5,6)],[(5,6),(7,9),(9,6),(7,11)],[(5,10),(6,7),(7,9),(8,6),(10,10)],[(5,7),(7,6),(9,7),(9,10),(5,10),(5,7)]][idx]
 d.line(rune,fill='#34384e');icons['silk_'+key]=im

tex=RES/'assets'/NS/'textures/item';tex.mkdir(parents=True,exist_ok=True)
pixels={}
for key,im in icons.items():
 assert im.size==(16,16)
 im.save(tex/(key+'.png'))
 write(Path('assets')/NS/'models/item'/f'{key}.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+key}})
 pixels[key]=[list(im.getdata())[i:i+16] for i in range(0,256,16)]
(ROOT/'tools/pixels.generated.json').write_text(json.dumps(pixels),encoding='utf-8')
# Native golem textures: derive the metal texture silhouettes and recolor material surfaces only.
from zipfile import ZipFile
with ZipFile(next((ROOT.parent/'mods').glob('*modulargolems*'))) as z:
 for entity in ['metal_golem','humanoid_golem','dog_golem']:
  candidates=[n for n in z.namelist() if n.startswith('assets/modulargolems/textures/entity/'+entity+'/') and n.endswith('/iron.png')]
  if not candidates:candidates=[n for n in z.namelist() if n.startswith('assets/modulargolems/textures/entity/'+entity+'/') and n.endswith('iron.png')]
  if candidates:
   import io
   source=Image.open(io.BytesIO(z.read(candidates[0]))).convert('RGBA')
   for mat in ['flesh','bone']:
    tinted=source.copy()
    for y in range(source.height):
     for x in range(source.width):
      r,g,b,a=source.getpixel((x,y));v=(r+g+b)/765
      color=(int(48+v*142),int(31+v*84),int(36+v*74),a) if mat=='flesh' else (int(57+v*175),int(63+v*163),int(64+v*140),a)
      tinted.putpixel((x,y),color)
    p=RES/'assets'/NS/'textures/entity'/entity/(mat+'.png');p.parent.mkdir(parents=True,exist_ok=True);tinted.save(p)

font=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',16)
board=Image.new('RGB',(720,((len(icons)+5)//6)*160),(16,21,31));draw=ImageDraw.Draw(board)
for i,(key,im) in enumerate(icons.items()):
 x=(i%6)*120;y=(i//6)*160
 board.paste(im.resize((96,96),Image.Resampling.NEAREST),(x+12,y+10),im.resize((96,96),Image.Resampling.NEAREST))
 draw.text((x+5,y+112),names[key],font=font,fill=(206,217,228))
 board.paste(im,(x+52,y+140),im)
(ROOT/'docs').mkdir(exist_ok=True);board.save(ROOT/'docs/items-preview.png')
print(f'Generated {len(icons)} native 16x16 icons and all resources.')
