import urllib.request
import json
import os

run_id = 40010662052
url = f'https://api.github.com/repos/unzipv2r-design/arch-player-new-/actions/runs/{run_id}/jobs'
token = os.environ.get('GITHUB_TOKEN', 'YOUR_GITHUB_TOKEN')
headers = {
    'Accept': 'application/vnd.github+json',
    'User-Agent': 'Antigravity-Agent',
    'Authorization': f'Bearer {token}'
}

req = urllib.request.Request(url, headers=headers)
try:
    with urllib.request.urlopen(req) as resp:
        data = json.loads(resp.read().decode('utf-8'))
        for j in data.get('jobs', []):
            print(f"Job: {j['name']} | Status: {j['status']} | Conclusion: {j['conclusion']}")
            for s in j.get('steps', []):
                print(f"   Step: {s['name']} -> {s['status']} ({s['conclusion']})")
except Exception as e:
    print("Error:", e)
