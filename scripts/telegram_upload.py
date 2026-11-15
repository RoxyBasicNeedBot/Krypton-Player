#!/usr/bin/env python3
import os
import sys
import json
import uuid
import asyncio
import urllib.request
import subprocess
from pathlib import Path

def get_git_info():
    try:
        sha = subprocess.check_output(["git", "rev-parse", "--short", "HEAD"]).decode().strip()
        msg = subprocess.check_output(["git", "log", "-1", "--pretty=%B"]).decode().strip()
        return sha, msg
    except Exception:
        return "latest", "Automated release build"

def register_update_in_worker(update_data):
    worker_base = os.environ.get("ARCH_API_URL", "").rstrip("/")
    if not worker_base:
        print("ARCH_API_URL not set. Skipping worker update registration.")
        return
    try:
        url = f"{worker_base}/api/app/update"
        payload = json.dumps(update_data).encode('utf-8')
        req = urllib.request.Request(
            url,
            data=payload,
            headers={
                "Content-Type": "application/json",
                "User-Agent": "KryptonPlayer-CI/1.0"
            }
        )
        with urllib.request.urlopen(req, timeout=15) as resp:
            print(f"Worker registration response: {resp.status}")
    except Exception as e:
        print(f"Note: Worker registration ping: {e}")

async def upload_with_pyrogram(api_id, api_hash, bot_token, owner_ids, file_path, caption):
    from pyrogram import Client
    print("=== 🚀 Initializing Pyrogram MTProto 2.0 Client (Bypassing 50MB HTTP limit, supports up to 2GB) ===")
    app = Client(
        "arch_bot_uploader",
        api_id=int(api_id),
        api_hash=api_hash,
        bot_token=bot_token,
        in_memory=True
    )

    async with app:
        me = await app.get_me()
        print(f"Connected to Telegram MTProto as @{me.username} (Bot ID: {me.id})")

        first_chat_id = int(owner_ids[0])
        print(f"Uploading {file_path.name} ({file_path.stat().st_size / (1024*1024):.2f} MB) via MTProto to chat {first_chat_id}...")

        msg = await app.send_document(
            chat_id=first_chat_id,
            document=str(file_path),
            caption=caption,
            file_name=file_path.name
        )

        file_id = msg.document.file_id
        file_size = msg.document.file_size
        message_id = msg.id
        print(f"Successfully uploaded to Telegram via MTProto! file_id: {file_id}, message_id: {message_id}")

        for chat_id in owner_ids[1:]:
            try:
                cid = int(chat_id)
                print(f"Delivering APK to owner ID {cid} using file_id...")
                await app.send_document(
                    chat_id=cid,
                    document=file_id,
                    caption=caption,
                    file_name=file_path.name
                )
                print(f"Delivered to owner {cid}!")
            except Exception as ex:
                print(f"Note for owner {chat_id}: {ex}")

        return {
            "file_id": file_id,
            "file_name": file_path.name,
            "file_size": file_size,
            "message_id": message_id,
            "chat_id": first_chat_id
        }

def send_telegram_document_http(bot_token, chat_id, file_path, caption):
    file_path = Path(file_path)
    file_size = file_path.stat().st_size
    print(f"Fallback HTTP upload: {file_path.name} ({file_size / (1024*1024):.2f} MB) to {chat_id}...")
    boundary = uuid.uuid4().hex
    parts = []
    parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="chat_id"\r\n\r\n{chat_id}\r\n'.encode('utf-8'))
    parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="caption"\r\n\r\n{caption}\r\n'.encode('utf-8'))
    parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="parse_mode"\r\n\r\nHTML\r\n'.encode('utf-8'))
    parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="document"; filename="{file_path.name}"\r\nContent-Type: application/vnd.android.package-archive\r\n\r\n'.encode('utf-8'))
    with open(file_path, 'rb') as f:
        parts.append(f.read())
    parts.append(f'\r\n--{boundary}--\r\n'.encode('utf-8'))
    req = urllib.request.Request(
        f"https://api.telegram.org/bot{bot_token}/sendDocument",
        data=b''.join(parts),
        headers={"Content-Type": f"multipart/form-data; boundary={boundary}"}
    )
    with urllib.request.urlopen(req, timeout=180) as resp:
        data = json.loads(resp.read().decode('utf-8'))
        if data.get("ok"):
            doc = data["result"].get("document", {})
            return {
                "file_id": doc.get("file_id"),
                "file_name": file_path.name,
                "file_size": file_size,
                "message_id": data["result"].get("message_id"),
                "chat_id": chat_id
            }
    return None

def get_app_version():
    vname, vcode = "0.16.3", 56
    try:
        p = Path("app/build.gradle.kts")
        if p.exists():
            text = p.read_text(encoding="utf-8")
            import re
            m_code = re.search(r'versionCode\s*=\s*(\d+)', text)
            m_name = re.search(r'versionName\s*=\s*"([^"]+)"', text)
            if m_code:
                vcode = int(m_code.group(1))
            if m_name:
                vname = m_name.group(1)
    except Exception as e:
        print(f"Version extraction error: {e}")

    env_ver = os.environ.get("VERSION")
    if env_ver:
        vname = env_ver.strip().lstrip("v")
    env_code = os.environ.get("VERSION_CODE")
    if env_code and env_code.isdigit():
        vcode = int(env_code)

    return vname, vcode

def main():
    bot_token = os.environ.get("BOT_TOKEN")
    api_id = os.environ.get("API_ID")
    api_hash = os.environ.get("API_HASH")
    owner_ids_raw = os.environ.get("OWNER_CHAT_ID", "")
    owner_ids = [cid.strip() for cid in owner_ids_raw.split() if cid.strip()]

    if not bot_token or not owner_ids:
        print("Notice: BOT_TOKEN or OWNER_CHAT_ID environment variables are not set. Skipping Telegram upload.")
        return

    apk_dirs = [
        Path("app/build/outputs/apk/release"),
        Path("app/build/outputs/apk/debug")
    ]
    apk_dir = None
    for d in apk_dirs:
        if d.exists() and list(d.glob("*.apk")):
            apk_dir = d
            break

    if not apk_dir:
        print("No APK directory with APK files found.")
        return

    candidates = [
        apk_dir / "app-arm64-v8a-release.apk",
        apk_dir / "app-universal-release.apk",
        apk_dir / "app-arm64-v8a-debug.apk",
        apk_dir / "app-universal-debug.apk"
    ]
    arm64_apk = None
    for c in candidates:
        if c.exists():
            arm64_apk = c
            break

    if not arm64_apk:
        apks = list(apk_dir.glob("*.apk"))
        if apks:
            arm64_apk = apks[0]
        else:
            print("No APK found to upload.")
            return

    git_sha, git_msg = get_git_info()
    version_name, version_code = get_app_version()

    caption = (
        f"<b>Krypton Player New Build</b>\n"
        f"Version: {version_name} (Build {version_code})\n"
        f"Commit: <code>{git_sha}</code>\n"
        f"Changelog: {git_msg[:200]}\n"
        f"#kryptonplayer_update"
    )

    upload_res = None
    if api_id and api_hash:
        try:
            import pyrogram
            upload_res = asyncio.run(upload_with_pyrogram(api_id, api_hash, bot_token, owner_ids, arm64_apk, caption))
        except Exception as e:
            print(f"Pyrogram MTProto upload note: {e}. Falling back to HTTP Bot API...")

    if not upload_res:
        for chat_id in owner_ids:
            try:
                res = send_telegram_document_http(bot_token, chat_id, arm64_apk, caption)
                if res and not upload_res:
                    upload_res = res
            except Exception as he:
                print(f"HTTP upload to {chat_id} error: {he}")

    worker_base = os.environ.get("ARCH_API_URL", "").rstrip("/")
    repo = os.environ.get("GITHUB_REPOSITORY", "unzipv2r-design/arch-player-new-")
    file_id = upload_res.get("file_id", "") if upload_res else ""
    # Always register high-speed direct GitHub Release CDN download URL (bypasses Telegram Bot API's 20MB limit)
    download_url = f"https://github.com/{repo}/releases/download/v{version_name}/{arm64_apk.name}"

    update_data = {
        "version_name": version_name,
        "version_code": version_code,
        "commit_sha": git_sha,
        "commit_message": git_msg,
        "changelog": git_msg,
        "file_id": file_id,
        "file_name": arm64_apk.name,
        "file_size": arm64_apk.stat().st_size,
        "download_url": download_url,
        "message_id": upload_res.get("message_id", 0) if upload_res else 0,
        "chat_id": owner_ids[0] if owner_ids else ""
    }
    register_update_in_worker(update_data)
    print(f"Release update processed. Download URL: {download_url}")

if __name__ == "__main__":
    main()
