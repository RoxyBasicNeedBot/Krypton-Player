import os
import shutil
import json

src_base = r'C:\Users\vip\Downloads\Telegram Desktop\ZNV CLAN ( @ZnvDragons ) @TgEmodziBot by @EmojiSaverBot'
dst_dir = r'core\ui\src\main\res\raw'

mappings = {
    'anim_telegram_bolt.json': 'CAACAgUAAxUAAWqY9_RSjkNxRzZFK7t-qmwG7A88AAJ2FQACqjz5Vi8NfOXINNIXPQQ',
    'anim_telegram_phone.json': 'CAACAgUAAxUAAWqY9_TACodBOunRqfbnPxHFWW-wAAJ7EgACZLAxVxxlyjC1J7dWPQQ',
    'anim_telegram_bell.json': 'CAACAgUAAxUAAWqY9_RB-1J3t-DdMLvE9z--TtsaAAL5GQACSwPwVne8aD4pqwQsPQQ',
    'anim_telegram_music.json': 'CAACAgUAAxUAAWqY9_TKxl2VydzbhiFr8emJxJWmAAIiFQACM7YxVw0kVPmN0zg9PQQ',
    'anim_telegram_sparkles.json': 'CAACAgUAAxUAAWqY9_SHXof_OmlHObIqwcBbYEPwAALxEgACZB45V36vtJLq80nmPQQ',
    'anim_telegram_star.json': 'CAACAgUAAxUAAWqY9_TQG574r-Egr0JlEbfUckT6AAJSFAAC13zxVhmUOBezxYqrPQQ',
    'anim_telegram_chart.json': 'CAACAgUAAxUAAWqY9_Tjtn4UlmEd1KwfLRyFBqoSAAITFAAC5aH4VnBM_PGsHgU6PQQ',
    'anim_telegram_flame.json': 'CAACAgUAAxUAAWqY9_RheZZva7qaZ_4SCIHrI3lzAAJ7GgAC8PzwVqdE5daK9GWJPQQ',
    'anim_telegram_like.json': 'CAACAgUAAxUAAWqY9_RdBsXip4xmOtCgUSJA_M8sAAIvGAACP275VrypGCfA7phgPQQ',
    'anim_telegram_hourglass.json': 'CAACAgUAAxUAAWqY9_TuGFw56tZgEdfhbttY9jGOAALpFQACk6v5Vl0ognE7pB4-PQQ',
    'anim_telegram_warning.json': 'CAACAgUAAxUAAWqY9_T28CwrqMhYJg72HLdlmx7jAAJoFAACkv_4VstnA98AAZFZ0T0E',
    'anim_telegram_check_circle.json': 'CAACAgUAAxUAAWqY9_RiLXTQwApw4mDtGDaIgjd5AAKwFgACH6j5Vu1eSkOhFmq8PQQ',
    'anim_telegram_stop.json': 'CAACAgUAAxUAAWqY9_Q6zXaVRisgTBz3TZAzRl_NAAICFQAC0sn5VntPTwpA3TO3PQQ',
    'anim_telegram_cup.json': 'CAACAgUAAxUAAWqY9_SEaAhXG_I3n6udvVRJw6VLAAJOFQACGsP4VhXf8A3ZX3-SPQQ',
    'anim_telegram_megaphone.json': 'CAACAgUAAxUAAWqY9_SQUWlkjB5KC7nnLXr3bkvMAAIQFwACKVH5Vs_enPSlRKUmPQQ'
}

for dst_name, folder in mappings.items():
    folder_path = os.path.join(src_base, folder)
    json_files = [f for f in os.listdir(folder_path) if f.endswith('.json')]
    if not json_files:
        print(f"ERROR: No json in {folder}")
        continue
    src_file = os.path.join(folder_path, json_files[0])
    dst_file = os.path.join(dst_dir, dst_name)
    with open(src_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    shutil.copyfile(src_file, dst_file)
    print(f"Copied {dst_name} ({os.path.getsize(dst_file)/1024:.1f} KB, nm={ascii(data.get('nm'))})")

print("Done copying selected Telegram Lottie icons!")
