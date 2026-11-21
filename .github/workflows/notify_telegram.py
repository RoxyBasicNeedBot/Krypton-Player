#!/usr/bin/env python3
import html
import os
import re
import subprocess
import requests


def get_git_commit_info():
    try:
        commit_author = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%an']).decode('utf-8').strip()
        commit_message = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%s']).decode('utf-8').strip()
        commit_hash = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%H']).decode('utf-8').strip()
        commit_hash_short = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%h']).decode('utf-8').strip()
        return commit_author, commit_message, commit_hash, commit_hash_short
    except Exception:
        return "Unknown", "Automated CI Build", "latest", "latest"


def parse_chat_ids(raw_value):
    if not raw_value:
        return []
    tokens = re.split(r'[\s,]+', raw_value.strip())
    result = []
    for token in tokens:
        cleaned = token.strip().strip('"').strip("'")
        if not cleaned:
            continue
        # Ensure channel/user handles start with @ if non-numeric
        if not re.fullmatch(r'-?\d+', cleaned) and not cleaned.startswith('@'):
            cleaned = f"@{cleaned}"
        if cleaned not in result:
            result.append(cleaned)
    return result


def send_telegram_message(bot_token, chat_id, message, topic_id=None):
    url = f"https://api.telegram.org/bot{bot_token}/sendMessage"
    payload = {
        "chat_id": chat_id,
        "text": message,
        "parse_mode": "HTML",
        "disable_web_page_preview": True,
    }
    # Only apply topic_id for forum supergroups (-100...)
    if topic_id and chat_id.startswith("-100"):
        payload["message_thread_id"] = topic_id

    response = requests.post(url, json=payload, timeout=15)
    if response.status_code == 200:
        print(f"Start message sent successfully to {chat_id}.")
        return True

    # Retry without topic_id if the target chat is not a forum / thread doesn't exist
    if "message_thread_id" in payload and "thread" in response.text.lower():
        payload.pop("message_thread_id", None)
        retry = requests.post(url, json=payload, timeout=15)
        if retry.status_code == 200:
            print(f"Start message sent successfully to {chat_id} (without topic_id).")
            return True
        response = retry

    print(f"Failed to send start message to {chat_id}: {response.status_code} {response.text}")
    if "chat not found" in response.text.lower():
        print(
            "Hint: Telegram returned 'chat not found'. Make sure:\n"
            "  1) For a personal DM (OWNER_CHAT_ID), open your bot in Telegram and tap Start (/start).\n"
            "  2) For a group/channel (CHAT_ID), add the bot to the chat (as admin for channels) and include the '-100' prefix for supergroups/channels.\n"
            "  3) Verify BOT_TOKEN matches the same bot that has access to the chat."
        )
    return False


def main():
    bot_token = (os.environ.get('BOT_TOKEN') or '').strip()
    is_release = (os.environ.get('IS_RELEASE') or '').strip().lower() == 'true'
    owner_chat_ids = parse_chat_ids(os.environ.get('OWNER_CHAT_ID'))
    public_chat_ids = [cid for cid in parse_chat_ids(os.environ.get('CHAT_ID')) if cid not in owner_chat_ids]

    # On non-release pushes, only notify the owner (never spam public group/channel users)
    target_chat_ids = owner_chat_ids if not is_release else (owner_chat_ids + public_chat_ids)

    if not bot_token or not target_chat_ids:
        print("Notice: BOT_TOKEN or target chat ID is not set (silent push skips public CHAT_ID). Skipping Telegram notification.")
        return

    topic_id_raw = (os.environ.get('TOPIC_ID') or '').strip()
    topic_id = int(topic_id_raw) if topic_id_raw.isdigit() else None
    repo = os.environ.get('GITHUB_REPOSITORY', 'RoxyBasicNeedBot/Krypton-Player')

    commit_author, commit_message, commit_hash, commit_hash_short = get_git_commit_info()

    safe_author = html.escape(commit_author)
    safe_message = html.escape(commit_message)
    commit_url = f"https://github.com/{repo}/commit/{commit_hash}"

    message = (
        f"🚀 <b>Krypton Player CI / CD Build Started</b>\n\n"
        f"A new <a href=\"{commit_url}\">commit</a> has been pushed by <b>{safe_author}</b>:\n"
        f"<blockquote>{safe_message}</blockquote>\n\n"
        f"Building with <b>Native TDLib MTProto 2.0</b> &amp; Gradle cache. APKs will be delivered here automatically upon completion.\n\n"
        f"#{commit_hash_short}"
    )

    for chat_id in target_chat_ids:
        try:
            send_telegram_message(bot_token, chat_id, message, topic_id=topic_id)
        except Exception as e:
            print(f"Error sending start message to {chat_id}: {e}")


if __name__ == "__main__":
    main()
