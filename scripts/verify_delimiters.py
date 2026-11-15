import re
import sys
from pathlib import Path

def clean_text(text):
    clean = re.sub(r"/\*.*?\*/", "", text, flags=re.DOTALL)
    clean = re.sub(r'"""[\s\S]*?"""', '""', clean)
    clean = re.sub(r'"(?:\\.|[^"\\])*"', '""', clean)
    clean = re.sub(r"'(?:\\.|[^'\\])'", "''", clean)
    clean = re.sub(r"//.*", "", clean)
    return clean

def check_file(path_str):
    path = Path(path_str)
    if not path.exists():
        print(f"File not found: {path}")
        return False
    text = path.read_text(encoding="utf-8")
    clean = clean_text(text)

    p = clean.count("(") - clean.count(")")
    b = clean.count("[") - clean.count("]")
    c = clean.count("{") - clean.count("}")
    print(f"{path.name}: p={p}, b={b}, c={c}")
    if p != 0 or b != 0 or c != 0:
        lines = text.split("\n")
        curr_p, curr_b, curr_c = 0, 0, 0
        for idx, line in enumerate(lines):
            cl = clean_text(line)
            dp = cl.count("(") - cl.count(")")
            db = cl.count("[") - cl.count("]")
            dc = cl.count("{") - cl.count("}")
            curr_p += dp
            curr_b += db
            curr_c += dc
            if dp != 0 or db != 0 or dc != 0:
                print(f"  Line {idx+1}: p={curr_p} (dp={dp}), b={curr_b}, c={curr_c} | {line.strip()[:60]}")
        return False
    return True

if __name__ == "__main__":
    files = sys.argv[1:]
    all_ok = True
    for f in files:
        ok = check_file(f)
        if not ok:
            all_ok = False
    sys.exit(0 if all_ok else 1)
