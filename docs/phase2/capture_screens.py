"""Actual Pixel 6 captures; debug-only route extra does not mutate user records."""
import json
import os
from pathlib import Path
import shlex
import subprocess
import sys
import time

ROOT=Path(__file__).resolve().parent
ADB=os.environ.get('ANDROID_ADB','/Users/zel/Library/Android/sdk/platform-tools/adb')
SCREENS={
    '02-create-account':'Create Account','03-log-in':'Log In','04-reset-password':'Reset Password',
    '05-profile':'Profile','06-personal-information':'Personal Information','07-account-settings':'Account Settings',
    '08-notifications':'Notifications','09-add-your-pet':'Add Your Pet','11-edit-pet-profile':'Edit Pet Profile',
    '12-my-pets':'Pets','13-care-circle':'Care Circle','15-voice-log':'Voice Log','16-sound-event-journal':'Sound Journal',
    '17-pet-records':'Records','18-vaccination-card':'Vaccination Card','19-records-search':'Browse Records',
    '21-reminders':'Reminders','22-care-calendar':'Calendar','23-pet-id-qr':'Pet ID QR','24-emergency-mode':'Emergency',
    '25-offline-sync':'Offline & Sync','26-access-control':'Care Circle Access','27-activity-log':'Activity Log','28-qr-privacy':'QR Privacy',
    'phase1-welcome':'Welcome','phase1-home':'Home','phase1-overview':'Pet Detail','phase1-log-care':'Care',
    'phase1-timeline':'Pet Timeline','phase1-sitter':'Sitter Mode','phase1-passport':'Pet Passport'
}
def adb(*args):
    for attempt in range(3):
        result=subprocess.run([ADB,*args],stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=40)
        if result.returncode==0:return result.stdout
        time.sleep(1)
    raise RuntimeError(result.stderr.decode())

if __name__=='__main__':
    mode=sys.argv[1] if len(sys.argv)>1 else 'dark'
    selected=sys.argv[2:]
    output=ROOT/'screenshots'/mode
    output.mkdir(parents=True,exist_ok=True)
    adb('shell','cmd','uimode','night','yes' if mode=='dark' else 'no')
    info={'size':adb('shell','wm','size').decode(),'density':adb('shell','wm','density').decode(),'mode':mode}
    (output/'device.json').write_text(json.dumps(info,indent=2))
    for slug,screen in SCREENS.items():
        if selected and slug not in selected:continue
        adb('shell','am','start','-W','-S','-n','com.purrfolio.mco1/.MainActivity','--es','preview_screen',shlex.quote(screen))
        time.sleep(2)
        screenshot=adb('exec-out','screencap','-p')
        if not screenshot.startswith(b'\x89PNG'):raise RuntimeError('Invalid screenshot response')
        (output/(slug+'.png')).write_bytes(screenshot)
        print(f'{mode}: {screen}',flush=True)

