#!/usr/bin/env python3
"""
Krypton README Bot
------------------
Fetches live repository telemetry (stars, forks, downloads, watchers, open issues,
latest release APKs, top contributors, and recent commits) from the GitHub REST API
and refreshes marked sections inside README.md without external dependencies.
"""

from __future__ import annotations

import json
import os
import re
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

REPO = os.environ.get("GITHUB_REPOSITORY", "RoxyBasicNeedBot/Krypton-Player")
TOKEN = os.environ.get("GITHUB_TOKEN", "")
README_PATH = Path(os.environ.get("README_PATH", "README.md"))
API_BASE = "https://api.github.com"

STAR_MILESTONES = [25, 50, 100, 250, 500, 1000, 2500, 5000, 10000, 25000, 50000]


def api_get(path: str) -> Any:
    url = f"{API_BASE}{path}"
    headers = {
        "Accept": "application/vnd.github+json",
        "User-Agent": "Krypton-README-Bot/1.0",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    if TOKEN:
        headers["Authorization"] = f"Bearer {TOKEN}"

    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        print(f"[warn] GitHub API {path} returned HTTP {exc.code}: {exc.reason}", file=sys.stderr)
        return None
    except Exception as exc:
        print(f"[warn] GitHub API {path} failed: {exc}", file=sys.stderr)
        return None


def fmt_num(value: int) -> str:
    if value >= 1_000_000:
        return f"{value / 1_000_000:.1f}M"
    if value >= 10_000:
        return f"{value / 1_000:.1f}K"
    return f"{value:,}"


def fmt_bytes(size_bytes: int) -> str:
    if size_bytes <= 0:
        return "—"
    mb = size_bytes / (1024 * 1024)
    return f"{mb:.1f} MB"


def fmt_date(iso_str: str | None) -> str:
    if not iso_str:
        return "—"
    try:
        dt = datetime.fromisoformat(iso_str.replace("Z", "+00:00"))
        return dt.strftime("%b %d, %Y")
    except ValueError:
        return iso_str[:10]


def next_milestone(stars: int) -> int:
    for m in STAR_MILESTONES:
        if stars < m:
            return m
    return ((stars // 25000) + 1) * 25000


def progress_bar(current: int, target: int, width: int = 20) -> str:
    ratio = min(max(current / max(target, 1), 0.0), 1.0)
    filled = round(ratio * width)
    bar = "█" * filled + "░" * (width - filled)
    pct = int(round(ratio * 100))
    return f"`{bar}` **{pct}%** ({fmt_num(current)} / {fmt_num(target)} ⭐)"


def detect_abi_label(filename: str) -> tuple[str, int]:
    lower = filename.lower()
    if "arm64" in lower:
        return ("**ARM64-v8a** *(Recommended)*", 0)
    if "armeabi" in lower or "armv7" in lower:
        return ("**ARMv7** *(32-bit)*", 1)
    if "universal" in lower:
        return ("**Universal** *(All Devices)*", 2)
    if "x86_64" in lower:
        return ("**x86_64** *(Emulator / Chromebook)*", 3)
    return (f"`{filename}`", 4)


def replace_section(content: str, marker: str, body: str) -> str:
    start_tag = f"<!-- BOT:{marker}:START -->"
    end_tag = f"<!-- BOT:{marker}:END -->"
    pattern = re.compile(
        rf"{re.escape(start_tag)}.*?{re.escape(end_tag)}",
        flags=re.DOTALL,
    )
    replacement = f"{start_tag}\n{body.strip()}\n{end_tag}"
    if not pattern.search(content):
        print(f"[warn] Marker {marker} not found in {README_PATH}", file=sys.stderr)
        return content
    return pattern.sub(lambda _: replacement, content, count=1)


def build_stats_block(repo_data: dict[str, Any], releases: list[dict[str, Any]]) -> str:
    stars = int(repo_data.get("stargazers_count", 0))
    forks = int(repo_data.get("forks_count", 0))
    watchers = int(repo_data.get("subscribers_count", repo_data.get("watchers_count", 0)))
    open_issues = int(repo_data.get("open_issues_count", 0))

    total_downloads = 0
    latest_tag = "Latest"
    latest_url = f"https://github.com/{REPO}/releases/latest"

    for idx, rel in enumerate(releases):
        if idx == 0 and rel.get("tag_name"):
            latest_tag = rel["tag_name"]
            latest_url = rel.get("html_url", latest_url)
        for asset in rel.get("assets", []):
            total_downloads += int(asset.get("download_count", 0))

    goal = next_milestone(stars)
    bar = progress_bar(stars, goal)
    synced_at = datetime.now(timezone.utc).strftime("%b %d, %Y • %H:%M UTC")

    return f"""<div align="center">

| ⭐ Stars | 🍴 Forks | 📥 Total Downloads | 👀 Watchers | 🏷️ Latest Release | 🐛 Open Issues |
| :---: | :---: | :---: | :---: | :---: | :---: |
| **{fmt_num(stars)}** | **{fmt_num(forks)}** | **{fmt_num(total_downloads)}** | **{fmt_num(watchers)}** | **[{latest_tag}]({latest_url})** | **[{fmt_num(open_issues)}](https://github.com/{REPO}/issues)** |

<p>
  <b>🏆 Next Star Milestone:</b> {bar}
</p>

<sub>🤖 <i>Auto-synced by <b>Krypton README Bot</b> &bull; Last updated: {synced_at}</i></sub>

</div>"""


def build_latest_release_block(releases: list[dict[str, Any]]) -> str:
    if not releases:
        return (
            f"> [!TIP]\n"
            f"> Grab the latest signed APK from [**GitHub Releases**](https://github.com/{REPO}/releases/latest)."
        )

    latest = releases[0]
    tag = latest.get("tag_name", "Latest")
    name = latest.get("name") or tag
    rel_url = latest.get("html_url", f"https://github.com/{REPO}/releases/latest")
    published = fmt_date(latest.get("published_at"))

    apks = [
        asset
        for asset in latest.get("assets", [])
        if str(asset.get("name", "")).lower().endswith(".apk")
    ]

    if not apks:
        return (
            f"> [!TIP]\n"
            f"> **Latest Release:** [{name}]({rel_url}) &bull; Published **{published}**\n"
            f"> Download APK builds directly from the [**Releases Page**]({rel_url})."
        )

    apks.sort(key=lambda a: detect_abi_label(a.get("name", ""))[1])

    rows = [
        f"### 🚀 Latest Release: [{name}]({rel_url}) — `{published}`",
        "",
        "| Architecture / Variant | Size | Downloads | Direct Download |",
        "| :--- | :---: | :---: | :---: |",
    ]

    for asset in apks:
        file_name = asset.get("name", "KryptonPlayer.apk")
        abi_label, _ = detect_abi_label(file_name)
        size_str = fmt_bytes(int(asset.get("size", 0)))
        dl_count = fmt_num(int(asset.get("download_count", 0)))
        dl_url = asset.get("browser_download_url", rel_url)
        rows.append(f"| {abi_label} | `{size_str}` | {dl_count} | [**⬇ Download APK**]({dl_url}) |")

    return "\n".join(rows)


def build_contributors_block(contributors: list[dict[str, Any]]) -> str:
    humans = [
        c
        for c in contributors
        if c.get("type") == "User"
        and not str(c.get("login", "")).endswith("[bot]")
    ][:12]

    if not humans:
        return (
            '<div align="center">\n'
            f'  <a href="https://github.com/{REPO}/graphs/contributors">\n'
            f'    <img src="https://contrib.rocks/image?repo={REPO}" alt="Krypton Player Contributors" />\n'
            "  </a>\n"
            "</div>"
        )

    cells: list[str] = []
    for user in humans:
        login = user.get("login", "contributor")
        avatar = user.get("avatar_url", "")
        avatar_sized = f"{avatar}{'&' if '?' in avatar else '?'}s=96" if avatar else ""
        profile = user.get("html_url", f"https://github.com/{login}")
        commits = int(user.get("contributions", 0))
        cells.append(
            f'<td align="center" width="120">'
            f'<a href="{profile}">'
            f'<img src="{avatar_sized}" width="56" height="56" alt="{login}" /><br />'
            f'<sub><b>@{login}</b></sub>'
            f"</a><br />"
            f"<sub>{fmt_num(commits)} commits</sub>"
            f"</td>"
        )

    rows: list[str] = []
    per_row = 6
    for i in range(0, len(cells), per_row):
        rows.append("  <tr>\n    " + "\n    ".join(cells[i : i + per_row]) + "\n  </tr>")

    table = "<table>\n" + "\n".join(rows) + "\n</table>"
    return f'<div align="center">\n{table}\n</div>'


def build_activity_block(commits: list[dict[str, Any]]) -> str:
    if not commits:
        return f"<sub>View full commit history on [GitHub](https://github.com/{REPO}/commits/main).</sub>"

    lines = [
        "| Commit | Message | Author | Date |",
        "| :---: | :--- | :--- | :---: |",
    ]
    shown = 0
    for item in commits:
        commit_obj = item.get("commit", {})
        raw_msg = str(commit_obj.get("message", "")).splitlines()[0].strip()
        # Skip bot-generated README sync commits so real code changes stay front and center
        if "[skip ci]" in raw_msg and "README" in raw_msg:
            continue
        sha = str(item.get("sha", ""))[:7]
        url = item.get("html_url", f"https://github.com/{REPO}/commit/{sha}")
        safe_msg = raw_msg.replace("|", "\\|")
        if len(safe_msg) > 72:
            safe_msg = safe_msg[:69].rstrip() + "..."
        author = (
            (item.get("author") or {}).get("login")
            or (commit_obj.get("author") or {}).get("name")
            or "unknown"
        )
        date_str = fmt_date((commit_obj.get("author") or {}).get("date"))
        lines.append(f"| [`{sha}`]({url}) | {safe_msg} | `@{author}` | {date_str} |")
        shown += 1
        if shown >= 5:
            break

    if shown == 0:
        return f"<sub>View full commit history on [GitHub](https://github.com/{REPO}/commits/main).</sub>"

    return "\n".join(lines)


def main() -> int:
    if not README_PATH.exists():
        print(f"[error] {README_PATH} not found.", file=sys.stderr)
        return 1

    repo_data = api_get(f"/repos/{REPO}")
    if not isinstance(repo_data, dict):
        print("[error] Could not fetch repository metadata; leaving README unchanged.", file=sys.stderr)
        return 1

    releases = api_get(f"/repos/{REPO}/releases?per_page=30")
    if not isinstance(releases, list):
        releases = []
    # Prefer published non-draft releases
    published_releases = [r for r in releases if not r.get("draft", False)]

    contributors = api_get(f"/repos/{REPO}/contributors?per_page=20")
    if not isinstance(contributors, list):
        contributors = []

    commits = api_get(f"/repos/{REPO}/commits?per_page=15")
    if not isinstance(commits, list):
        commits = []

    original = README_PATH.read_text(encoding="utf-8")
    updated = original
    updated = replace_section(updated, "STATS", build_stats_block(repo_data, published_releases))
    updated = replace_section(updated, "LATEST_RELEASE", build_latest_release_block(published_releases))
    updated = replace_section(updated, "CONTRIBUTORS", build_contributors_block(contributors))
    updated = replace_section(updated, "ACTIVITY", build_activity_block(commits))

    # Ignore timestamp-only changes so the bot never pushes empty commits when stats haven't changed
    ts_pattern = re.compile(r"Last updated: [^<]+")
    if ts_pattern.sub("", updated) != ts_pattern.sub("", original):
        README_PATH.write_text(updated, encoding="utf-8")
        print("[ok] README.md updated with latest GitHub metrics.")
    else:
        print("[ok] No metric changes detected — skipping README update.")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
