import os, math, numpy as np
from PIL import Image
T='../src/main/resources/assets/krolasyonfutbol/textures/'
OUT=os.environ.get('OUT','/tmp/prev.png')
tiles=[]
def up(p, s):
    im=Image.open(T+p).convert('RGBA'); return im.resize((im.width*s, im.height*s), Image.NEAREST)
# ball sphere render
tex=np.array(Image.open(T+'entity/football.png').convert('RGBA')).astype(float)
H,W=tex.shape[:2]
N=200
img=np.zeros((N,N,4))
for rot in [0.0, 1.2]:
    pass
ys,xs=np.mgrid[0:N,0:N]
dx=(xs+0.5-N/2)/(N/2*0.95); dy=(ys+0.5-N/2)/(N/2*0.95)
r2=dx*dx+dy*dy; m=r2<1
dz=np.sqrt(np.clip(1-r2,0,1))
X,Y,Z=dx,-dy,dz
th=np.arccos(np.clip(Y,-1,1)); ph=np.mod(np.arctan2(Z,X),2*math.pi)
u=(ph/(2*math.pi)*W).astype(int)%W; v=np.clip((th/math.pi*H).astype(int),0,H-1)
c=tex[v,u]; light=0.45+0.55*np.clip(-0.4*dx-0.4*dy+0.8*dz,0,1)
img[...,:3]=c[...,:3]*light[...,None]; img[...,3]=m*255
ballim=Image.fromarray(img.astype(np.uint8),'RGBA')
sheet=Image.new('RGBA',(1400,900),(60,70,80,255))
x=10;y=10
def put(im):
    global x,y
    if x+im.width>1390: x=10; y+=rowh[0]+10; rowh[0]=0
    sheet.alpha_composite(im,(x,y)); x+=im.width+10; rowh[0]=max(rowh[0],im.height)
rowh=[0]
put(ballim); put(up('entity/football.png',1))
for n in ['jersey_red','jersey_blue','jersey_red_gk','jersey_blue_gk']: put(up('entity/'+n+'.png',4))
for i in range(8): put(up(f'entity/footballer_{i}.png',3))
put(up('entity/numbers.png',2))
for b in ['pitch_grass_light','pitch_grass_dark','pitch_line','pitch_side','goal_post','goal_net','corner_flag','seat_red','seat_blue','stand_step','floodlight_front','floodlight_side']: put(up('block/'+b+'.png',5))
for it in ['football','pitch_builder','whistle','bot_red','bot_blue']: put(up('item/'+it+'.png',6))
sheet.save(OUT)
