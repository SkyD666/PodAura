import argparse
import json
import os
from pathlib import Path
import subprocess
import sys


def request(method, fields, attachments=None):
    api_url = os.environ.get("BOT_API_URL", "http://127.0.0.1:8081").rstrip("/")
    command = [
        "curl", "--fail-with-body", "--silent", "--show-error",
        "--connect-timeout", "10", "--max-time", "600" if method.startswith("send") else "120",
        f"{api_url}/bot{os.environ['BOT_TOKEN']}/{method}",
    ]
    for name, value in fields.items():
        command += ["--form-string", f"{name}={value}"]
    for name, file in (attachments or {}).items():
        command += ["-F", f"{name}=@{file}"]
    response = subprocess.run(command, capture_output=True, text=True)
    try:
        payload = json.loads(response.stdout)
    except ValueError:
        raise RuntimeError(f"Telegram {method} failed (curl exit {response.returncode}).") from None
    if method == "logOut" and api_url == "https://api.telegram.org" and payload.get("error_code") == 429:
        print("Cloud bot login is cooling down; continuing with the local server.")
        return
    if response.returncode or not payload.get("ok"):
        raise RuntimeError(f"Telegram {method}: {payload.get('description', 'request failed')}")
    return payload["result"]


def main():
    directory = Path("telegram-artifacts")
    files = sorted(directory.glob("PodAura_*_Android_arm64-v8a_GitHub.apk"))
    files += sorted(directory.glob("PodAura_*_iOS_arm64_Unsigned.ipa"))
    if not files:
        print("No APK or IPA available; skipping Telegram upload.")
        return

    caption = os.environ["COMMIT_MESSAGE"][:1024]
    channel = os.environ["CHANNEL_ID"]
    total_size = sum(file.stat().st_size for file in files)
    print(f"Telegram upload: {len(files)} file(s), {total_size} bytes total.")
    if len(files) == 1:
        request("sendDocument", {"chat_id": channel, "caption": caption}, {"document": files[0]})
    else:
        media = [
            {"type": "document", "media": f"attach://file_{index}"}
            for index in range(len(files))
        ]
        media[0]["caption"] = caption
        request("sendMediaGroup", {
            "chat_id": channel, "media": json.dumps(media, ensure_ascii=False),
        }, {f"file_{index}": file for index, file in enumerate(files)})


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--api-url", default=os.environ.get("BOT_API_URL", "http://127.0.0.1:8081"))
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--check", action="store_true")
    mode.add_argument("--logout", action="store_true")
    args = parser.parse_args()
    os.environ["BOT_API_URL"] = args.api_url
    try:
        if args.check:
            request("getMe", {})
            print("Local Bot API Server is ready.")
        elif args.logout:
            request("logOut", {})
        else:
            main()
    except RuntimeError as error:
        sys.exit(str(error))
