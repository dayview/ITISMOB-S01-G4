from PIL import Image, ImageDraw
from pathlib import Path
import sys
mode=sys.argv[1] if len(sys.argv)>1 else 'dark'
root=Path(__file__).parent
files=sorted((root/mode).glob('*.png'))
canvas=Image.new('RGB',(280*len(files),660),'#27212F')
for i,f in enumerate(files):
    im=Image.open(f);im.thumbnail((280,622));canvas.paste(im,(i*280,30))
    ImageDraw.Draw(canvas).text((i*280+8,8),f.stem,fill='white')
canvas.save(root/f'{mode}-contact-sheet.png')
