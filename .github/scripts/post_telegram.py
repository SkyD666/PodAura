import json
import os
from pathlib import Path
import subprocess


def main():
    directory = Path("telegram-artifacts")
    files = sorted(directory.glob("PodAura_*_Android_arm64-v8a_GitHub.apk"))
    files += sorted(directory.glob("PodAura_*_iOS_arm64_Unsigned.ipa"))
    if not files:
        print("No APK or IPA available; skipping Telegram upload.")
        return

    caption = os.environ["COMMIT_MESSAGE"][:1024]
    method = "sendMediaGroup" if len(files) > 1 else "sendDocument"
    command = [
        "curl", "--fail-with-body", "--silent", "--show-error",
        f"https://api.telegram.org/bot{os.environ['BOT_TOKEN']}/{method}",
        "--form-string", f"chat_id={os.environ['CHANNEL_ID']}",
    ]
    if len(files) == 1:
        command += ["--form-string", f"caption={caption}", "-F", f"document=@{files[0]}"]
    else:
        media = [
            {"type": "document", "media": f"attach://file_{index}"}
            for index in range(len(files))
        ]
        media[0]["caption"] = caption
        command += ["--form-string", f"media={json.dumps(media, ensure_ascii=False)}"]
        for index, file in enumerate(files):
            command += ["-F", f"file_{index}=@{file}"]
    subprocess.run(command, check=True)


if __name__ == "__main__":
    main()
