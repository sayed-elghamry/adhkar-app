"""يحمّل صفحات مصحف الشمرلي من archive.org ويضغطها (webp) لتُدمج داخل التطبيق.
يعمل في GitHub Actions فقط (بيئة البناء لها إنترنت). الصفحات الموجودة مسبقًا (من الكاش) لا يُعاد تحميلها."""
import io, json, os, sys, time, urllib.request
from concurrent.futures import ThreadPoolExecutor
from PIL import Image

ID = '20191116_20191116_1655'
LEAVES = 522
OUT = os.path.join(os.path.dirname(__file__), '..', 'www', 'shamarly')
os.makedirs(OUT, exist_ok=True)

def get(leaf):
    dst = os.path.join(OUT, f'{leaf}.webp')
    if os.path.exists(dst) and os.path.getsize(dst) > 5000:
        return True
    url = f'https://archive.org/download/{ID}/page/n{leaf}_w900.jpg'
    for attempt in range(6):
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'adhkar-app-build'})
            with urllib.request.urlopen(req, timeout=60) as r:
                data = r.read()
            im = Image.open(io.BytesIO(data)).convert('RGB')
            if im.width > 1000:
                im = im.resize((1000, round(im.height * 1000 / im.width)), Image.LANCZOS)
            im.save(dst, 'WEBP', quality=60, method=6)
            return True
        except Exception as e:
            print(f'leaf {leaf} attempt {attempt + 1}: {e}', flush=True)
            time.sleep(3 * (attempt + 1))
    return False

with ThreadPoolExecutor(6) as ex:
    ok = list(ex.map(get, range(LEAVES)))
have = [i for i in range(LEAVES) if ok[i]]
json.dump({'leaves': have, 'ext': 'webp'}, open(os.path.join(OUT, 'index.json'), 'w'))
total = sum(os.path.getsize(os.path.join(OUT, f'{i}.webp')) for i in have)
print(f'pages: {len(have)}/{LEAVES}, size: {total / 1e6:.1f} MB')
if len(have) < LEAVES * 0.5:
    # لا نُفشل البناء: التطبيق يعود للتحميل من الإنترنت عند غياب الصفحات
    print('WARNING: many pages missing', file=sys.stderr)
