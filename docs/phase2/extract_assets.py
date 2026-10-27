"""Crop supplied wireframe artwork for faithful native presentation.

No generative replacements, resizing, or color changes are applied.
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent
OUT = ROOT.parents[1] / 'app/src/main/res/drawable-nodpi'
assets = {
    'pdf_mochi': ('page-02-image-01.png', (1186,247,1271,332)),
    'pdf_bailey': ('page-02-image-01.png', (1185,371,1268,456)),
    'pdf_luna': ('page-02-image-01.png', (1186,499,1269,585)),
    'pdf_simba': ('page-02-image-01.png', (1187,627,1269,710)),
    'pdf_owner': ('page-06-image-01.png', (437,216,494,274)),
    'pdf_mom': ('page-06-image-01.png', (663,216,719,274)),
    'pdf_ate': ('page-06-image-01.png', (438,336,495,391)),
    'pdf_jam': ('page-06-image-01.png', (550,350,607,406)),
    'pdf_reset_cat': ('page-01-image-01.jpeg', (1496,321,1784,501)),
    'pdf_sleeping_cat': ('page-01-image-01.jpeg', (1000,858,1178,980)),
    'pdf_notification_cat': ('page-01-image-02.png', (1255,722,1424,794)),
    'pdf_voice_cat': ('page-02-image-02.png', (843,199,1101,275)),
    'pdf_sound_cat': ('page-02-image-02.png', (1185,759,1274,838)),
    'pdf_folder': ('page-03-image-02.png', (80,387,328,499)),
    'pdf_vaccine_record': ('page-03-image-02.png', (433,190,733,539)),
    'pdf_id_cat': ('page-04-image-01.png', (800,807,889,870)),
    'pdf_offline_cat': ('page-06-image-01.png', (60,235,353,340)),
    'pdf_sync_cat': ('page-06-image-01.png', (279,776,365,846))
}
for name, (source, box) in assets.items():
    Image.open(ROOT / 'embedded' / source).crop(box).save(OUT / (name+'.png'))
print(f'Extracted {len(assets)} original artwork crops.')

