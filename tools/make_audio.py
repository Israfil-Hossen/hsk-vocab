# Records every Chinese sentence of the book and the app with a natural male voice (Microsoft Yunxi, through edge-tts).
# Files go to dawah/audio/<key>.mp3, where key is the same FNV-1a hash the website and the app use.
# Run it again to resume; existing files are kept unless --redo is given.
import asyncio, json, os, sys
import edge_tts

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'dawah', 'audio')
VOICE = os.environ.get('VOICE', 'zh-CN-YunxiNeural')
RATE = os.environ.get('RATE', '-10%')
texts = json.load(open(os.path.join(OUT, 'texts.json'), encoding='utf-8'))
if '--redo' in sys.argv:
    for f in os.listdir(OUT):
        if f.endswith('.mp3'):
            os.remove(os.path.join(OUT, f))
todo = [(k, t) for k, t in texts.items() if not os.path.exists(os.path.join(OUT, k + '.mp3'))]
print(f'{len(texts)} sentences, {len(todo)} to record with {VOICE} {RATE}', flush=True)
failed = []

async def one(k, t, sem, n):
    async with sem:
        path = os.path.join(OUT, k + '.mp3')
        for attempt in range(5):
            try:
                await edge_tts.Communicate(t, VOICE, rate=RATE).save(path + '.part')
                if os.path.getsize(path + '.part') > 500:
                    os.replace(path + '.part', path)
                    break
            except Exception as e:
                err = e
            await asyncio.sleep(3 * (attempt + 1))
        else:
            failed.append(k)
        if os.path.exists(path + '.part'):
            os.remove(path + '.part')
        n[0] += 1
        if n[0] % 100 == 0:
            print(f'  {n[0]}/{len(todo)}', flush=True)

async def main():
    sem, n = asyncio.Semaphore(8), [0]
    await asyncio.gather(*(one(k, t, sem, n) for k, t in todo))

if todo:
    asyncio.run(main())
made = sorted(f[:-4] for f in os.listdir(OUT) if f.endswith('.mp3'))
json.dump(made, open(os.path.join(OUT, 'index.json'), 'w'))
print(f'{len(made)} of {len(texts)} sentences have audio; {len(failed)} failed this run')
