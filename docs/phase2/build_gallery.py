"""Build a local review index without stretching any reference or screenshot."""
from pathlib import Path
from html import escape
ROOT=Path(__file__).resolve().parent
blocks=[]
protected={'01-welcome-existing':'phase1-welcome','10-pet-overview-existing':'phase1-overview',
           '14-log-care-existing':'phase1-log-care','20-passport-existing':'phase1-passport'}
for ref in sorted((ROOT/'references').glob('*.png')):
    slug=ref.stem
    images=[('PDF crop','references/'+ref.name)]
    for mode in ('dark','light'):
        path='screenshots/'+mode+'/'+protected.get(slug,slug)+'.png'
        if (ROOT/path).exists():images.append(('Pixel 6 · '+mode,path))
    figures=''.join(f'<figure><figcaption>{label}</figcaption><a href="{path}"><img loading="lazy" src="{path}" alt="{escape(slug)} {label}"></a></figure>' for label,path in images)
    blocks.append(f'<section><h2>{escape(slug.replace("-"," "))}</h2><a href="rendered-crops/{ref.name}">400 DPI crop</a><div class="images">{figures}</div></section>')
for slug in ('phase1-home','phase1-timeline','phase1-sitter'):
    images=[('Pixel 6 · '+mode,'screenshots/'+mode+'/'+slug+'.png') for mode in ('dark','light')]
    figures=''.join(f'<figure><figcaption>{label}</figcaption><a href="{path}"><img loading="lazy" src="{path}" alt="{slug} {label}"></a></figure>' for label,path in images)
    blocks.append(f'<section><h2>{slug.replace("-"," ")} · preserved</h2><p>Phase 1 reference was supplied earlier in chat.</p><div class="images">{figures}</div></section>')
html='''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>PURRFOLIO reference review</title><style>
body{margin:24px;background:#101014;color:#f8f2ff;font:16px system-ui}h1,h2{color:#d8b4f8}h2{font-size:20px}a{color:#d8b4f8}p{max-width:850px;line-height:1.6}.images{display:flex;gap:20px;align-items:flex-start;overflow-x:auto}figure{margin:18px 0;flex:0 0 min(340px,85vw)}img{width:100%;height:auto;display:block}figcaption{padding-bottom:10px}section{padding:15px 0;border-top:1px solid #373243}</style>
<h1>PURRFOLIO · Phase 2 visual review</h1><p>The PDF was rendered at 400 DPI. Each phone was cropped separately and inspected before implementation. Images below retain their original aspect ratios; click one to inspect its original resolution. Device captures use real Android system bars. The PDF contains taller phones than the Pixel 6, so longer native screens scroll.</p><p><a href="REFERENCE_INVENTORY.md">Reference inventory</a> · <a href="QA.md">Validation and limitations</a> · <a href="pages/page-01.png">400 DPI page 1</a></p>'''+''.join(blocks)+'</html>'
(ROOT/'index.html').write_text(html)
print('Wrote local reference and emulator review gallery.')
