from email.parser import BytesParser
from email.policy import default
from http.server import BaseHTTPRequestHandler, HTTPServer
import json
import os
from pathlib import Path
import tempfile
import subprocess
import threading
import unittest
from unittest.mock import patch

import post_telegram


class TelegramUploadTest(unittest.TestCase):
    def test_available_artifacts(self):
        apk = "PodAura_3.4-beta10_Android_arm64-v8a_GitHub.apk"
        ipa = "PodAura_1.0_iOS_arm64_Unsigned.ipa"
        for filenames in [[], [apk], [ipa], [apk, ipa]]:
            with self.subTest(files=filenames), tempfile.TemporaryDirectory() as temp:
                previous = Path.cwd()
                try:
                    os.chdir(temp)
                    directory = Path("telegram-artifacts")
                    directory.mkdir()
                    for filename in filenames:
                        (directory / filename).touch()
                    (directory / "PodAura_3.4-beta10_Android_x86_64_GitHub.apk").touch()
                    env = {"BOT_TOKEN": "test", "BOT_API_URL": "http://127.0.0.1:8081", "CHANNEL_ID": "channel", "COMMIT_MESSAGE": "Commit `text` & 中文\n"}
                    response = subprocess.CompletedProcess([], 0, stdout='{"ok":true,"result":true}')
                    with patch.dict(os.environ, env), patch.object(post_telegram.subprocess, "run", return_value=response) as run:
                        post_telegram.main()
                    if not filenames:
                        run.assert_not_called()
                        continue
                    run.assert_called_once()
                    args = run.call_args.args[0]
                    self.assertTrue(run.call_args.kwargs["capture_output"])
                    if len(filenames) == 1:
                        self.assertIn("http://127.0.0.1:8081/bottest/sendDocument", args)
                        self.assertIn(f"document=@{directory / filenames[0]}", args)
                        self.assertIn("caption=" + env["COMMIT_MESSAGE"], args)
                    else:
                        self.assertIn("http://127.0.0.1:8081/bottest/sendMediaGroup", args)
                        media = json.loads(next(a[6:] for a in args if a.startswith("media=")))
                        self.assertEqual([m["media"] for m in media], ["attach://file_0", "attach://file_1"])
                        self.assertTrue(all(m["type"] == "document" for m in media))
                        self.assertEqual(media[0]["caption"], env["COMMIT_MESSAGE"])
                        self.assertNotIn("caption", media[1])
                        for index, filename in enumerate(filenames):
                            self.assertIn(f"file_{index}=@{directory / filename}", args)
                finally:
                    os.chdir(previous)

    def test_large_album_over_http(self):
        received = []

        class Handler(BaseHTTPRequestHandler):
            def do_POST(self):
                size = int(self.headers['Content-Length'])
                first = self.rfile.read(min(4096, size))
                remaining = size - len(first)
                while remaining:
                    chunk = self.rfile.read(min(1024 * 1024, remaining))
                    if not chunk:
                        break
                    remaining -= len(chunk)
                received.append((self.path, size, first, remaining))
                self.send_response(200)
                self.end_headers()
                self.wfile.write(b'{"ok":true,"result":true}')

            def log_message(self, *args):
                pass

        server = HTTPServer(('127.0.0.1', 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        previous = Path.cwd()
        try:
            with tempfile.TemporaryDirectory() as temp:
                os.chdir(temp)
                directory = Path('telegram-artifacts')
                directory.mkdir()
                for filename, size in [
                    ('PodAura_3.4-beta10_Android_arm64-v8a_GitHub.apk', 24_158_418),
                    ('PodAura_1.0_iOS_arm64_Unsigned.ipa', 37_550_130),
                ]:
                    with (directory / filename).open('wb') as file:
                        file.truncate(size)
                env = {
                    'BOT_TOKEN': 'test', 'CHANNEL_ID': 'channel', 'COMMIT_MESSAGE': 'caption',
                    'BOT_API_URL': f'http://127.0.0.1:{server.server_port}',
                }
                with patch.dict(os.environ, env):
                    post_telegram.main()
            self.assertEqual(len(received), 1)
            path, size, first, remaining = received[0]
            self.assertEqual(path, '/bottest/sendMediaGroup')
            self.assertGreater(size, 61_708_548)
            self.assertIn(b'attach://file_0', first)
            self.assertIn(b'attach://file_1', first)
            self.assertEqual(remaining, 0)
        finally:
            os.chdir(previous)
            server.shutdown()
            server.server_close()
            thread.join()

    def test_cloud_logout_cooldown(self):
        response = subprocess.CompletedProcess([], 22, stdout='{"ok":false,"error_code":429}')
        env = {'BOT_TOKEN': 'test', 'BOT_API_URL': 'https://api.telegram.org'}
        with patch.dict(os.environ, env), patch.object(post_telegram.subprocess, 'run', return_value=response):
            self.assertIsNone(post_telegram.request('logOut', {}))
            with self.assertRaises(RuntimeError):
                post_telegram.request('sendDocument', {})
        env['BOT_API_URL'] = 'http://127.0.0.1:8081'
        with patch.dict(os.environ, env), patch.object(post_telegram.subprocess, 'run', return_value=response):
            with self.assertRaises(RuntimeError):
                post_telegram.request('logOut', {})

    def test_special_characters_over_http(self):
        received = []

        class Handler(BaseHTTPRequestHandler):
            def do_POST(self):
                body = self.rfile.read(int(self.headers['Content-Length']))
                headers = f"Content-Type: {self.headers['Content-Type']}\r\nMIME-Version: 1.0\r\n\r\n"
                message = BytesParser(policy=default).parsebytes(headers.encode() + body)
                fields = {
                    part.get_param('name', header='content-disposition'): part.get_payload(decode=True)
                    for part in message.iter_parts()
                }
                received.append((self.path, fields))
                self.send_response(200)
                self.end_headers()
                self.wfile.write(b'{"ok":true,"result":true}')

            def log_message(self, *args):
                pass

        server = HTTPServer(('127.0.0.1', 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        previous = Path.cwd()
        captions = [
            '@not-a-file;type=text/plain',
            '<not-a-file',
            "引号 ' \" 反斜杠 \\ 换行\nCRLF\r\n制表符\t😀 & <tag> _*[]()~`>#+-=|{}.!%"
            '\n$(touch injected) `touch injected` ; touch injected'
            '\n${{ secrets.TEST }} ::set-output name=test::value',
            '😀' * 1030,
        ]
        try:
            with tempfile.TemporaryDirectory() as temp:
                os.chdir(temp)
                directory = Path('telegram-artifacts')
                directory.mkdir()
                (directory / 'PodAura_3.4-beta10_Android_arm64-v8a_GitHub.apk').touch()
                ipa = directory / 'PodAura_1.0_iOS_arm64_Unsigned.ipa'
                for count in [1, 2]:
                    if count == 2:
                        ipa.touch()
                    for caption in captions:
                        with self.subTest(files=count, caption=caption):
                            env = {
                                'BOT_TOKEN': 'test', 'CHANNEL_ID': 'channel', 'COMMIT_MESSAGE': caption,
                                'BOT_API_URL': f'http://127.0.0.1:{server.server_port}',
                            }
                            with patch.dict(os.environ, env):
                                post_telegram.main()
                            path, fields = received[-1]
                            self.assertNotIn('parse_mode', fields)
                            if count == 1:
                                self.assertEqual(path, '/bottest/sendDocument')
                                actual = fields['caption'].decode('utf-8')
                            else:
                                self.assertEqual(path, '/bottest/sendMediaGroup')
                                media = json.loads(fields['media'].decode('utf-8'))
                                self.assertNotIn('parse_mode', media[0])
                                actual = media[0]['caption']
                            self.assertEqual(actual, caption[:1024])
                            self.assertFalse(Path('injected').exists())
                self.assertEqual(len(received), 8)
        finally:
            os.chdir(previous)
            server.shutdown()
            server.server_close()
            thread.join()

    def test_api_error_does_not_expose_token(self):
        response = subprocess.CompletedProcess([], 22, stdout='{"ok":false,"description":"Request Entity Too Large"}')
        with patch.dict(os.environ, {"BOT_TOKEN": "secret-token"}), patch.object(post_telegram.subprocess, "run", return_value=response):
            with self.assertRaises(RuntimeError) as raised:
                post_telegram.request("sendMediaGroup", {"chat_id": "channel"})
        self.assertIn("Request Entity Too Large", str(raised.exception))
        self.assertNotIn("secret-token", str(raised.exception))


if __name__ == "__main__":
    unittest.main()
