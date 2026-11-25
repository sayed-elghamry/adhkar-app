import io, os, urllib.request, zipfile
from PIL import Image
ID='20191116_20191116_1655'
out='exp'; os.makedirs(out, exist_ok=True)
for leaf in (5, 200, 400):
    for w in (900,):
        data=urllib.request.urlopen(urllib.request.Request(f'https://archive.org/download/{ID}/page/n{leaf}_w{w}.jpg',headers={'User-Agent':'x'}),timeout=60).read()
        open(f'{out}/{leaf}_orig.jpg','wb').write(data)
        im=Image.open(io.BytesIO(data)).convert('RGB')
        im.save(f'{out}/{leaf}_a_q60.webp','WEBP',quality=60,method=6)
        im.save(f'{out}/{leaf}_b_q35.webp','WEBP',quality=35,method=6)
        g=im.convert('L'); g.save(f'{out}/{leaf}_c_gray_q45.webp','WEBP',quality=45,method=6)
        p=im.quantize(colors=8,method=Image.Quantize.MEDIANCUT); p.save(f'{out}/{leaf}_d_pal8.png',optimize=True)
        p.convert('RGB').save(f'{out}/{leaf}_e_pal8_lossless.webp','WEBP',lossless=True,method=6)
        p4=im.quantize(colors=4,method=Image.Quantize.MEDIANCUT); p4.convert('RGB').save(f'{out}/{leaf}_f_pal4_lossless.webp','WEBP',lossless=True,method=6)
        s=im.resize((720,round(im.height*720/im.width)),Image.LANCZOS); s.save(f'{out}/{leaf}_g_720_q50.webp','WEBP',quality=50,method=6)
for f in sorted(os.listdir(out)): print(f, os.path.getsize(f'{out}/{f}'))
with zipfile.ZipFile('exp.zip','w') as z:
    for f in os.listdir(out): z.write(f'{out}/{f}', f)
