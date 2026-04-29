import struct, zlib

def read_png(path):
    with open(path,'rb') as f: data=f.read()
    pos=8; idat=b''; w=h=0
    while pos<len(data):
        l=struct.unpack('>I',data[pos:pos+4])[0]; t=data[pos+4:pos+8].decode('ascii'); c=data[pos+8:pos+8+l]
        if t=='IHDR': w,h=struct.unpack('>II',c[:8])
        elif t=='IDAT': idat+=c
        pos+=12+l
    raw=zlib.decompress(idat); stride=w*4+1
    prev=[0]*(w*4); rows=[]
    for y in range(h):
        ftype=raw[y*stride]; line=list(raw[y*stride+1:y*stride+1+w*4])
        if ftype==1:
            for i in range(4,len(line)): line[i]=(line[i]+line[i-4])&255
        elif ftype==2:
            for i in range(len(line)): line[i]=(line[i]+prev[i])&255
        elif ftype==3:
            for i in range(len(line)):
                a=line[i-4] if i>=4 else 0; line[i]=(line[i]+(a+prev[i])//2)&255
        elif ftype==4:
            for i in range(len(line)):
                a=line[i-4] if i>=4 else 0; b=prev[i]; c2=prev[i-4] if i>=4 else 0
                p=a+b-c2; pa=abs(p-a); pb=abs(p-b); pc=abs(p-c2)
                pr=a if pa<=pb and pa<=pc else (b if pb<=pc else c2)
                line[i]=(line[i]+pr)&255
        prev=line[:]; rows.append([(line[x*4],line[x*4+1],line[x*4+2],line[x*4+3]) for x in range(w)])
    return w,h,rows

def write_png_rgba(path, w, h, rows):
    def chunk(name, data):
        c=name+data; return struct.pack('>I',len(data))+c+struct.pack('>I',zlib.crc32(c)&0xffffffff)
    raw=b''
    for row in rows:
        raw+=b'\x00'+bytes([v for px in row for v in px])
    png=b'\x89PNG\r\n\x1a\n'
    png+=chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))
    png+=chunk(b'IDAT',zlib.compress(raw,9))
    png+=chunk(b'IEND',b'')
    with open(path,'wb') as f: f.write(png)

src='common/src/main/resources/assets/cobbledollars_villagers_overhaul_rca/textures/gui/cobbledollars_Icon.png'
w,h,rows=read_png(src)

# Print all unique opaque colours to spot any remaining BG shades
unique={rows[y][x][:3] for y in range(h) for x in range(w) if rows[y][x][3]>0}
print("Unique opaque RGB colours:", sorted(unique))

# Flood-fill from all 4 edges: mark any opaque dark pixel reachable from
# the border as background and set alpha=0. This preserves dark border
# pixels that are enclosed inside the icon shape.
DARK_THRESH=80  # any pixel with R=G=B < threshold is "potentially background"
def is_dark(px): r,g,b,a=px; return a>0 and r<DARK_THRESH and g<DARK_THRESH and b<DARK_THRESH
bg=[[False]*w for _ in range(h)]
stack=[]
for y in range(h):
    for x in [0,w-1]:
        if is_dark(rows[y][x]) and not bg[y][x]: bg[y][x]=True; stack.append((y,x))
for x in range(w):
    for y in [0,h-1]:
        if is_dark(rows[y][x]) and not bg[y][x]: bg[y][x]=True; stack.append((y,x))
while stack:
    cy,cx=stack.pop()
    for ny,nx in [(cy-1,cx),(cy+1,cx),(cy,cx-1),(cy,cx+1)]:
        if 0<=ny<h and 0<=nx<w and not bg[ny][nx] and is_dark(rows[ny][nx]):
            bg[ny][nx]=True; stack.append((ny,nx))
cleaned=[]
for y in range(h):
    cleaned.append([(r,g,b,0) if bg[y][x] else (r,g,b,a) for x,(r,g,b,a) in enumerate(rows[y])])

# Extract bounding box of non-transparent content
xs=[x for y in range(h) for x in range(w) if cleaned[y][x][3]>0]
ys=[y for y in range(h) for x in range(w) if cleaned[y][x][3]>0]
if xs and ys:
    x0,x1,y0,y1=min(xs),max(xs),min(ys),max(ys)
    cw,ch=x1-x0+1,y1-y0+1
    print(f"Content bounding box: {cw}x{ch} at ({x0},{y0})-({x1},{y1})")
    # Build 16x16 canvas, center the content
    OUT=16
    ox=(OUT-cw)//2; oy=(OUT-ch)//2
    canvas=[[(0,0,0,0)]*OUT for _ in range(OUT)]
    for cy in range(ch):
        for cx in range(cw):
            px=cleaned[y0+cy][x0+cx]
            if 0<=oy+cy<OUT and 0<=ox+cx<OUT:
                canvas[oy+cy][ox+cx]=px
    out='common/src/main/resources/assets/cobbledollars_villagers_overhaul_rca/textures/item/cobbledollar_sign.png'
    write_png_rgba(out, OUT, OUT, canvas)
    print(f"Saved {OUT}x{OUT} clean texture to {out}")
else:
    print("No non-transparent pixels found after background removal!")
