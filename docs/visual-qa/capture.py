"""Capture actual emulator screens. Run after installing the debug APK."""
import pathlib, subprocess, time, sys, shlex
adb='/Users/zel/Library/Android/sdk/platform-tools/adb'
mode=sys.argv[1] if len(sys.argv)>1 else 'dark'
out=pathlib.Path(__file__).parent/mode
out.mkdir(exist_ok=True)
def run(*args):
    for attempt in range(3):
        result=subprocess.run([adb,*args],stdout=subprocess.PIPE)
        if result.returncode==0:return result.stdout
        time.sleep(1)
    raise RuntimeError(f"adb failed: {args}")
run('shell','cmd','uimode','night','yes' if mode.startswith('dark') else 'no')
for index,screen in enumerate(['Welcome','Home','Pet Detail','Care','Pet Timeline','Care Circle','Pet Passport'],1):
    if len(sys.argv)>2 and screen not in sys.argv[2:]:continue
    run('shell','am','start','-W','-S','-n','com.purrfolio.mco1/.MainActivity','--es','preview_screen',shlex.quote(screen))
    time.sleep(3)
    (out/f'{index:02d}-{screen.lower().replace(" ","-")}.png').write_bytes(run('exec-out','screencap','-p'))
    print(screen,flush=True)
