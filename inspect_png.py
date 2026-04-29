import struct, zlib

with open('common/src/main/resources/assets/cobbledollars_villagers_overhaul_rca/textures/item/cobbledollar_sign.png','rb') as f:
    data = f.read()
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

print(f"Image: {w}x{h}")
for y in range(h):
    non_t = [(x, rows[y][x]) for x in range(w) if rows[y][x][3] > 10]
    if non_t:
        print(f"row {y}: opaque at x={[x for x,_ in non_t[:8]]}, samples={non_t[:3]}")
    else:
        print(f"row {y}: all transparent")
