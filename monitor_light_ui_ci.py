import urllib.request
import json
import time
import sys
import os

TOKEN = os.environ.get("GITHUB_TOKEN", "YOUR_GITHUB_TOKEN")
REPO = "unzipv2r-design/arch-player-new-"
RUN_ID = 39977719074
URL = f"https://api.github.com/repos/{REPO}/actions/runs/{RUN_ID}"

headers = {
    "Authorization": f"Bearer {TOKEN}",
    "Accept": "application/vnd.github+json",
    "User-Agent": "CI-Monitor"
}

print(f"Monitoring GitHub Actions CI Run {RUN_ID} for commit 96fc7e5...", flush=True)

start_time = time.time()
while True:
    try:
        req = urllib.request.Request(URL, headers=headers)
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode())
            status = data.get("status")
            conclusion = data.get("conclusion")
            elapsed = int(time.time() - start_time)
            print(f"[{elapsed}s] Status: {status} | Conclusion: {conclusion}", flush=True)
            
            if status == "completed":
                print(f"\nCI Run {RUN_ID} finished with conclusion: {str(conclusion).upper()}", flush=True)
                if conclusion == "success":
                    print("Build succeeded! Release and debug APKs published.", flush=True)
                    sys.exit(0)
                else:
                    print(f"Build failed with conclusion: {conclusion}", flush=True)
                    sys.exit(1)
    except Exception as e:
        print(f"Polling error: {e}", flush=True)
    
    time.sleep(25)
