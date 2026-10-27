"""UI smoke check through actual accessible controls; creates no user records."""
import subprocess,time,xml.etree.ElementTree as ET,re,pathlib
adb='/Users/zel/Library/Android/sdk/platform-tools/adb'
def run(*args):return subprocess.check_output([adb,*args])
def tree():
 run('shell','uiautomator','dump','/sdcard/purrfolio-ui.xml')
 return ET.fromstring(run('shell','cat','/sdcard/purrfolio-ui.xml'))
def tap(label):
 nodes=[n for n in tree().iter('node') if n.get('content-desc')==label or n.get('text')==label]
 assert nodes,f'Missing control: {label}'
 x1,y1,x2,y2=map(int,re.findall(r'\d+',nodes[0].get('bounds')))
 run('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(1)
run('shell','am','start','-W','-S','-n','com.purrfolio.mco1/.MainActivity','--es','preview_screen','Home');time.sleep(2)
tap('Log care');tap('Log Water')
assert any(n.get('text')=='Water' for n in tree().iter('node')),'Care category was not preserved'
print('PASS Home center button -> Log Care -> Water preselected in existing entry form')
run('shell','am','start','-W','-S','-n','com.purrfolio.mco1/.MainActivity','--es','preview_screen',"'Pet Detail'");time.sleep(2)
tap('Pet options');tap('Pet Passport')
assert any(n.get('text')=='Vaccinations' for n in tree().iter('node'))
tap('Share Passport')
assert any('not yet integrated' in n.get('text','') for n in tree().iter('node'))
print('PASS Pet options -> Passport -> existing sharing placeholder; no fabricated share success')
