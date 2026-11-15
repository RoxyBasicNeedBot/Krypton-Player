#!/usr/bin/env python3
import os, sys, time, subprocess

def find_adb():
    candidates = [
        os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"),
        os.path.expandvars(r"%PROGRAMFILES%\Android\platform-tools\adb.exe"),
        os.path.expandvars(r"%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe"),
        "adb"
    ]
    for c in candidates:
        if os.path.exists(c):
            return c
    return "adb"

def main():
    adb = find_adb()
    print("=" * 60)
    print(">>> Krypton Player Live Crash Catcher <<<")
    print(f"ADB Binary: {adb}")
    print("Target: dev.roxy.vip.archplayer.debug")
    print("=" * 60)

    while True:
        res = subprocess.run([adb, "devices", "-l"], capture_output=True, text=True)
        lines = [l for l in res.stdout.split('\n')[1:] if 'device' in l and not l.startswith('*')]
        if lines:
            print(f"Connected Device: {lines[0].strip()}")
            break
        print("Waiting for USB device...")
        time.sleep(3)

    print("\n[LIVE LOGCAT] Monitoring dev.roxy.vip.archplayer.debug for crashes/exceptions...\n")
    cmd = [adb, "logcat", "-v", "time", "*:E"]
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding='utf-8', errors='replace')
    for line in p.stdout:
        ll = line.lower()
        if any(i in ll for i in ['archplayer', 'fatal', 'sigabrt', 'fatalerror', 'tdjni']):
            print(line.strip())
            sys.stdout.flush()

if __name__ == '__main__':
    main()
