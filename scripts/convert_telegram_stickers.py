import os
import subprocess
import imageio_ffmpeg

def convert_stickers():
    ffmpeg_exe = imageio_ffmpeg.get_ffmpeg_exe()
    src_base = r"C:\Users\vip\Downloads\Telegram Desktop\Girliess   @emojiin by @EmojiSaverBot"
    dst_dir = r"core\ui\src\main\res\drawable"
    os.makedirs(dst_dir, exist_ok=True)

    mappings = {
        "anim_girl_waiting.webp": "CAACAgUAAxUAAWqlm8Z3Wdna1wFHgk3zHoy1g6bHAAKaGQACcbqAVUtEpvR92JVTPQQ",
        "anim_girl_searching.webp": "CAACAgUAAxUAAWqlm8bMmLvzrkoivgABmm02WQgCTgACchkAAu0xgVXhnIh9C7HWcj0E",
        "anim_girl_boost.webp": "CAACAgUAAxUAAWqlm8YI2q11vKzHMiSDmMP-fRxdAAKRHAACjnKAVVqKC0Q_jVCmPQQ",
        "anim_girl_dancing.webp": "CAACAgUAAxUAAWqlm8Z95kGz2NiznreA8Hw1oqhFAAJCGgAC7EeAVcS_B3Ec4rtbPQQ"
    }

    total_size = 0
    for dst_name, folder_name in mappings.items():
        folder_path = os.path.join(src_base, folder_name)
        if not os.path.exists(folder_path):
            print(f"Skipping {folder_name}: not found")
            continue
        webm_files = [f for f in os.listdir(folder_path) if f.endswith(".webm")]
        if not webm_files:
            print(f"No webm found in {folder_path}")
            continue

        src_webm = os.path.join(folder_path, webm_files[0])
        dst_path = os.path.join(dst_dir, dst_name)

        cmd = [
            ffmpeg_exe, "-y",
            "-i", src_webm,
            "-vf", "fps=15,scale=220:220:flags=lanczos",
            "-c:v", "libwebp",
            "-lossless", "0",
            "-q:v", "65",
            "-compression_level", "6",
            "-loop", "0",
            dst_path
        ]
        subprocess.run(cmd, check=True, capture_output=True)
        size_kb = os.path.getsize(dst_path) / 1024
        total_size += size_kb
        print(f"Generated {dst_name}: {size_kb:.1f} KB")

    print(f"Done! Total size added: {total_size:.1f} KB")

if __name__ == "__main__":
    convert_stickers()
