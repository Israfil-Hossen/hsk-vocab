# Copies the recordings the Android app's lessons use into its assets, so lessons speak offline.
import json, os, re, shutil
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'dawah', 'audio')
DST = os.path.join(ROOT, 'android', 'app', 'src', 'main', 'assets', 'audio')

def key(t):
    t = re.sub(r'[^一-鿿0-9A-Za-z]', '', re.sub(r'[＿_]+', '，', t))
    h = 0x811c9dc5
    for c in t:
        h = ((h ^ ord(c)) * 0x01000193) & 0xffffffff
    return '%08x' % h

c = json.load(open(os.path.join(ROOT, 'app', 'content.json'), encoding='utf-8'))
keys = set()
for l in c['levels']:
    for s in l['sections']:
        for u in s['units']:
            keys |= {key(i['zh']) for i in u.get('items', [])}
            keys |= {key(e['zh']) for p in u.get('points', []) for e in p.get('ex', [])}
os.makedirs(DST, exist_ok=True)
n = size = 0
for k in keys:
    f = os.path.join(SRC, k + '.mp3')
    if os.path.exists(f):
        shutil.copy(f, DST); n += 1; size += os.path.getsize(f)
print(f'app audio: {n} of {len(keys)} lesson lines, {size / 1e6:.1f} MB')
