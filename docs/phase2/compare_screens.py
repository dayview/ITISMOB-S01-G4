"""Preserve both source and emulator aspect ratios in side-by-side QA sheets."""
from pathlib import Path
from PIL import Image,ImageDraw
import sys

ROOT=Path(__file__).resolve().parent
mode=sys.argv[1] if len(sys.argv)>1 else 'dark'
files=sorted((ROOT/'screenshots'/mode).glob('*.png'))
out=ROOT/'comparisons'/mode
out.mkdir(parents=True,exist_ok=True)
for file in files:
    ref=ROOT/'references'/file.name
    if not ref.exists():continue
    a=Image.open(ref).convert('RGB');b=Image.open(file).convert('RGB')
    width=365
    a=a.resize((width,round(a.height*width/a.width)))
    b=b.resize((width,round(b.height*width/b.width)))
    canvas=Image.new('RGB',(width*2+24,max(a.height,b.height)+38),'#302B39')
    draw=ImageDraw.Draw(canvas);draw.text((6,8),'PDF wireframe',fill='white');draw.text((width+24,8),'Android '+mode,fill='white')
    canvas.paste(a,(0,34));canvas.paste(b,(width+24,34));canvas.save(out/file.name)
for index in range(0,len(files),6):
    group=files[index:index+6];sheet=Image.new('RGB',(300*len(group),710),'#302B39');draw=ImageDraw.Draw(sheet)
    for i,file in enumerate(group):
        im=Image.open(file).convert('RGB');im.thumbnail((300,675));sheet.paste(im,(i*300,30));draw.text((i*300+5,8),file.stem,fill='white')
    sheet.save(out/f'sheet-{index//6+1:02}.png')
print(f'Wrote {len(files)} screenshot review entries for {mode}.')
