"""Supplemental compact-display check; always restore emulator settings."""
import subprocess,time,pathlib
adb='/Users/zel/Library/Android/sdk/platform-tools/adb'
out=pathlib.Path(__file__).parent/'compact';out.mkdir(exist_ok=True)
def run(*args):return subprocess.check_output([adb,*args])
original=run('shell','wm','density').decode()
night=run('shell','cmd','uimode','night').decode()
try:
 run('shell','wm','density','480')
 run('shell','cmd','uimode','night','yes')
 for label in ['Home','Care']:
  run('shell','am','start','-W','-S','-n','com.purrfolio.mco1/.MainActivity','--es','preview_screen',label);time.sleep(3)
  (out/f'{label.lower()}.png').write_bytes(run('exec-out','screencap','-p'))
 run('shell','cmd','overlay','enable-exclusive','--category','com.android.internal.systemui.navbar.threebutton')
 run('shell','am','start','-W','-S','-n','com.purrfolio.mco1/.MainActivity','--es','preview_screen','Home');time.sleep(3)
 (out/'home-three-button.png').write_bytes(run('exec-out','screencap','-p'))
finally:
 run('shell','wm','density',original.split('Override density:')[-1].strip() if 'Override density:' in original else 'reset')
 run('shell','cmd','overlay','enable-exclusive','--category','com.android.internal.systemui.navbar.gestural')
 run('shell','cmd','uimode','night','no' if 'no' in night else 'yes')
print('Captured 360dp-wide Home, Care, and Home with three-button navigation; restored settings.')
