#!/usr/bin/env python3
"""
Security Auditor & Credential Leak Guard for Krypton Player.
Scans repository for accidental secrets, hardcoded tokens, and client leak risks.
Exits with 0 if clean, non-zero if potential leaks are detected.
"""

import os
import re
import subprocess
import sys

# Regex rules for known token and secret patterns
SECRET_PATTERNS = [
    ("GitHub Personal Access Token (classic)", re.compile(r"\bghp_[a-zA-Z0-9]{36}\b")),
    ("GitHub Fine-Grained Token", re.compile(r"\bgithub_pat_[a-zA-Z0-9_]{82}\b")),
    ("GitHub OAuth Access Token", re.compile(r"\bgho_[a-zA-Z0-9]{36}\b")),
    ("GitHub User-to-Server Token", re.compile(r"\bghu_[a-zA-Z0-9]{36}\b")),
    ("GitHub Server-to-Server Token", re.compile(r"\bghs_[a-zA-Z0-9]{36}\b")),
    ("GitHub Refresh Token", re.compile(r"\bghr_[a-zA-Z0-9]{36}\b")),
    ("Telegram Bot Token", re.compile(r"\b[0-9]{8,10}:[a-zA-Z0-9_-]{35}\b")),
    ("Slack API Token", re.compile(r"\bxox[baprs]-[0-9a-zA-Z]{10,48}\b")),
    ("AWS Access Key ID", re.compile(r"\b(AKIA|ABIA|ACCA|ASIA)[0-9A-Z]{16}\b")),
    ("Private RSA/EC/PGP Key", re.compile(r"-----BEGIN (?:[A-Z0-9_-]+ )?PRIVATE KEY-----")),
]

# Patterns specifically for client code (app/, core/, feature/)
CLIENT_PATTERNS = [
    ("GitHub Authorization Bearer in Client", re.compile(r'header\(["\']Authorization["\'],\s*["\']Bearer\s+\$\{BuildConfig\.[A-Za-z0-9_]+\}["\']\)')),
    ("Hardcoded GitHub Token in Code", re.compile(r'(?:token|auth|bearer)\s*[:=]\s*["\'](ghp_[a-zA-Z0-9]{36}|github_pat_[a-zA-Z0-9_]{82})["\']', re.IGNORECASE)),
]

ALLOWED_PATHS = {
    "scripts/security_audit.py",
    ".env.example",
    "local.properties.example",
}

BINARY_EXTENSIONS = (
    ".png", ".jpg", ".jpeg", ".webp", ".ico", ".gif", ".svg",
    ".so", ".jar", ".aar", ".keystore", ".jks", ".bin", ".zip",
    ".tar", ".gz", ".7z", ".mp4", ".mkv", ".mp3", ".pdf",
    ".ttf", ".otf", ".woff", ".woff2", ".class"
)

def is_allowed_file(filepath: str) -> bool:
    normalized = filepath.replace("\\", "/")
    return normalized in ALLOWED_PATHS

def is_binary(filepath: str) -> bool:
    if filepath.lower().endswith(BINARY_EXTENSIONS):
        return True
    try:
        with open(filepath, "rb") as f:
            chunk = f.read(1024)
            if b"\x00" in chunk:
                return True
    except Exception:
        return True
    return False

def scan_file(filepath: str) -> list:
    issues = []
    if is_allowed_file(filepath):
        return issues

    if not os.path.exists(filepath) or not os.path.isfile(filepath):
        return issues

    # Skip large files > 2MB
    if os.path.getsize(filepath) > 2 * 1024 * 1024:
        return issues

    if is_binary(filepath):
        return issues

    try:
        with open(filepath, "r", encoding="utf-8", errors="ignore") as f:
            lines = f.readlines()
    except Exception:
        return issues

    normalized_path = filepath.replace("\\", "/")
    is_client_code = normalized_path.startswith(("app/", "core/", "feature/"))

    for idx, line in enumerate(lines, 1):
        # Ignore comments in test/example files that explicitly say YOUR_ or example
        if "YOUR_GITHUB_TOKEN" in line or "your_keystore_password" in line or "your_key_password" in line or ("example" in line.lower() and "token" not in line.lower()):
            continue

        for rule_name, pattern in SECRET_PATTERNS:
            matches = pattern.findall(line)
            if matches:
                masked = [m[:6] + "..." + m[-4:] if len(m) > 10 else "***" for m in matches]
                issues.append((filepath, idx, rule_name, masked, line.strip()[:100]))

        if is_client_code:
            for rule_name, pattern in CLIENT_PATTERNS:
                matches = pattern.findall(line)
                if matches:
                    issues.append((filepath, idx, rule_name, ["client secret leakage risk"], line.strip()[:100]))

    return issues

def main():
    print("=" * 60)
    print("🛡️  Krypton Player Credential Leak & Security Auditor")
    print("=" * 60)

    try:
        tracked_files = subprocess.check_output(
            ["git", "ls-files"], text=True, encoding="utf-8"
        ).splitlines()
    except Exception as e:
        print(f"Error executing git ls-files: {e}")
        tracked_files = []

    print(f"Scanning {len(tracked_files)} tracked files in git...")
    all_issues = []

    for f in tracked_files:
        issues = scan_file(f)
        if issues:
            all_issues.extend(issues)

    if all_issues:
        print(f"\n❌ FOUND {len(all_issues)} POTENTIAL CREDENTIAL LEAKS / RISKS:")
        for file_path, line_no, rule, matches, snippet in all_issues:
            print(f"  - [{rule}] in {file_path}:{line_no}")
            print(f"    Matches: {matches}")
            print(f"    Snippet: {snippet}")
        print("\nAudit status: FAILED. Please remove secrets before committing.")
        sys.exit(1)
    else:
        print("\n✅ AUDIT PASSED: Zero credentials, tokens, or private secrets found in tracked code.")
        sys.exit(0)

if __name__ == "__main__":
    main()
