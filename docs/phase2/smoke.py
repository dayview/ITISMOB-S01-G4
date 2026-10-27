"""Device UI checks: no pet, care, medical, or account records are created."""
import re, shlex, subprocess, time, xml.etree.ElementTree as ET
from pathlib import Path
ADB='/Users/zel/Library/Android/sdk/platform-tools/adb'
results=[]
def adb(*args):return subprocess.check_output([ADB,*args],timeout=45)
def tree():
    adb('shell','uiautomator','dump','/sdcard/purrfolio-phase2.xml')
    return ET.fromstring(adb('shell','cat','/sdcard/purrfolio-phase2.xml'))
def launch(screen):
    adb('shell','am','start','-W','-S','-n','com.purrfolio.mco1/.MainActivity','--es','preview_screen',shlex.quote(screen));time.sleep(1)
def nodes(label):
    all_nodes=list(tree().iter('node'))
    return [n for n in all_nodes if n.get('content-desc')==label]+[n for n in all_nodes if n.get('text')==label and n.get('content-desc')!=label]
def tap(label):
    found=nodes(label);assert found,f'Missing control {label}'
    x1,y1,x2,y2=map(int,re.findall(r'\d+',found[0].get('bounds')))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.5)
def visible(label):assert nodes(label),f'Missing visible text {label}'
def passed(message):results.append(message);print('PASS '+message,flush=True)

launch('Home');tap('Log care');tap('Log Water');visible('Water')
passed('Home → Log Care → existing Water entry form')
launch('Pet Detail');tap('Pet options');tap('Pet Passport');tap('Share Passport');visible('Pet ID QR');tap('Customize Info');visible('QR Information Privacy')
passed('Pet options → Passport → demo ID → QR Privacy')
launch('Profile');tap('Personal Information');tap('Full Name')
assert any(n.get('class')=='android.widget.EditText' for n in tree().iter('node'))
adb('shell','input','keyevent','4');tap('Back');visible('Profile')
passed('Profile → editable personal form → back without saving')
launch('Add Your Pet');tap('Choose Species');tap('Dog');visible('Dog')
passed('Pet species dropdown selects Dog without creating a pet')
launch('Calendar');tap('Next month');visible('October 2026');tap('Previous month');visible('September 2026');tap('September 24, 2026');visible('Thu, Sep 24, 2026')
passed('Calendar month navigation and date selection update the visible date')
launch('Browse Records');tap('Vaccinations');visible('Recent Results (1)');visible('Vaccination Card')
passed('Record type filter narrows the sample results')
launch('Care Circle Access');tap('Activity Log');visible('Care Circle Activity Log')
passed('Access Control → distinct activity log destination')
launch('Sound Journal');visible('00:12');tap('Save written sound observation');visible('Other')
passed('Sound preview timer is visible and save opens the written log form')
launch('Sitter Mode');visible('Sitter Link')
launch('Care Circle');visible('Invite Caregiver')
passed('Protected Sitter Mode and new Care Circle remain separate routes')
launch('Home')
(Path(__file__).parent/'smoke-results.txt').write_text('\n'.join('PASS '+r for r in results)+'\n')
