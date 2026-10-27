"""Render supplied PDF and extract original embedded artwork without stretching.

Requires PyMuPDF and Pillow. Phone crops are supplied in crops.json after inspection.
"""
import json
from pathlib import Path
import fitz
from PIL import Image

ROOT = Path(__file__).resolve().parent
SOURCE = Path('/Users/zel/Downloads/[ITISMOB] MCO1_ Purrfolio.docx.pdf')
PDF = fitz.open(SOURCE)
for folder in ('pages', 'embedded', 'references', 'rendered-crops'):
    (ROOT / folder).mkdir(parents=True, exist_ok=True)
manifest = []
for i, page in enumerate(PDF, 1):
    output = ROOT / 'pages' / f'page-{i:02}.png'
    if not output.exists():
        page.get_pixmap(dpi=400, alpha=False).save(output)
    for j, item in enumerate(page.get_images(full=True), 1):
        xref = item[0]
        raw = PDF.extract_image(xref)
        name = f'page-{i:02}-image-{j:02}.{raw["ext"]}'
        (ROOT / 'embedded' / name).write_bytes(raw['image'])
        manifest.append(dict(page=i, name=name, width=raw['width'], height=raw['height'],
                             pdf_bounds=[list(r) for r in page.get_image_rects(xref)]))
(ROOT / 'embedded-manifest.json').write_text(json.dumps(manifest, indent=2))
if (ROOT / 'crops.json').exists():
    crops = json.loads((ROOT / 'crops.json').read_text())
    for crop in crops:
        source = ROOT / crop['source']
        image = Image.open(source)
        image.crop(crop['box']).save(ROOT / 'references' / (crop['name'] + '.png'))
        # Render each isolated original phone through PDF at 400 DPI. This retains
        # the PDF placement scale while recovering artwork clipped by page edges.
        entry = next(m for m in manifest if m['name'] == source.name)
        left, top, right, bottom = crop['box']
        bounds = entry['pdf_bounds'][0]
        scale = (bounds[2] - bounds[0]) / image.width
        single = fitz.open()
        phone = single.new_page(width=(right-left)*scale, height=(bottom-top)*scale)
        phone.insert_image(fitz.Rect(-left*scale, -top*scale,
                                    (image.width-left)*scale, (image.height-top)*scale),
                           filename=str(source))
        phone.get_pixmap(dpi=400, alpha=False).save(ROOT / 'rendered-crops' / (crop['name']+'.png'))
    print(f'Saved {len(crops)} phone crops at their original aspect ratios.')
print(json.dumps(manifest, indent=2))
