#!/usr/bin/env python3
"""
Upload Bearded Robot APKs/AABs to the "SDK Bearded Robot Builds" Google Drive folder
and print the share links needed for the Confluence builds page.

Why a script instead of an MCP tool: the Drive API's resumable upload streams the file
straight off disk. A 10-26 MB APK never has to be base64-encoded into a tool argument,
which is what makes the MCP route impossible. See ../refs/bearded-robot.md.

Setup (one time)
----------------
    python3 -m venv ~/.venvs/br-drive
    ~/.venvs/br-drive/bin/pip install google-api-python-client google-auth-oauthlib

    # OAuth client (type: Desktop app) downloaded from Google Cloud Console:
    ~/.config/br-drive-upload/client_secret.json      chmod 600

Usage
-----
    ~/.venvs/br-drive/bin/python upload_br_apks.py <file> [<file> ...]

First run opens a browser once for consent and writes a cached token next to the
client secret. Later runs are non-interactive.

Options
-------
    --folder <id>     override the destination folder
    --replace         replace a same-named file in the folder instead of duplicating
    --anyone          also grant "anyone with the link" read access
"""

import argparse
import os
import sys

from google.auth.transport.requests import Request
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError
from googleapiclient.http import MediaFileUpload

# Least privilege: drive.file grants access only to files this script creates.
# If Drive rejects writing into the folder, widen to ".../auth/drive".
SCOPES = ["https://www.googleapis.com/auth/drive.file"]

CONFIG_DIR = os.path.expanduser("~/.config/br-drive-upload")
CLIENT_SECRET = os.path.join(CONFIG_DIR, "client_secret.json")
TOKEN = os.path.join(CONFIG_DIR, "token.json")

# "SDK Bearded Robot Builds" — https://wizrocket.atlassian.net/wiki/x/C4CGPQE
DEFAULT_FOLDER_ID = "1fK79PXJh4gz3-LqZAYa-AcMNsR6MVcZV"

MIME_BY_EXT = {
    ".apk": "application/vnd.android.package-archive",
    ".aab": "application/octet-stream",  # no registered type for Android App Bundles
}


def get_service():
    if not os.path.exists(CLIENT_SECRET):
        sys.exit(
            f"Missing {CLIENT_SECRET}\n"
            "Download an OAuth client of type 'Desktop app' from the Google Cloud "
            "Console and save it there (chmod 600)."
        )

    creds = None
    if os.path.exists(TOKEN):
        creds = Credentials.from_authorized_user_file(TOKEN, SCOPES)

    if not creds or not creds.valid:
        if creds and creds.expired and creds.refresh_token:
            creds.refresh(Request())
        else:
            print("Opening a browser for one-time Google consent…")
            creds = InstalledAppFlow.from_client_secrets_file(
                CLIENT_SECRET, SCOPES
            ).run_local_server(port=0)
        with open(TOKEN, "w") as fh:
            fh.write(creds.to_json())
        os.chmod(TOKEN, 0o600)  # holds a refresh token — keep it private

    return build("drive", "v3", credentials=creds)


def find_existing(svc, name, folder_id):
    """Return the id of a same-named file already in the folder, if any."""
    safe = name.replace("'", "\\'")
    resp = (
        svc.files()
        .list(
            q=f"name = '{safe}' and '{folder_id}' in parents and trashed = false",
            fields="files(id,name)",
            pageSize=1,
        )
        .execute()
    )
    files = resp.get("files", [])
    return files[0]["id"] if files else None


def upload(svc, path, folder_id, replace=False, anyone=False):
    if not os.path.isfile(path):
        raise FileNotFoundError(path)

    name = os.path.basename(path)
    ext = os.path.splitext(path)[1].lower()
    mime = MIME_BY_EXT.get(ext, "application/octet-stream")
    size_mb = os.path.getsize(path) / (1024 * 1024)

    # resumable=True is the whole point: streams from disk in chunks, survives a
    # dropped connection, and never materialises the file in memory.
    media = MediaFileUpload(path, mimetype=mime, resumable=True,
                            chunksize=8 * 1024 * 1024)

    existing = find_existing(svc, name, folder_id) if replace else None
    if existing:
        req = svc.files().update(fileId=existing, media_body=media,
                                 fields="id,webViewLink")
        verb = "replacing"
    else:
        req = svc.files().create(
            body={"name": name, "parents": [folder_id]},
            media_body=media,
            fields="id,webViewLink",
        )
        verb = "uploading"

    print(f"{verb} {name} ({size_mb:.1f} MB)")
    resp = None
    while resp is None:
        status, resp = req.next_chunk()
        if status:
            print(f"  {int(status.progress() * 100):3d}%", end="\r", flush=True)
    print("  100%")

    if anyone:
        svc.permissions().create(
            fileId=resp["id"], body={"type": "anyone", "role": "reader"}
        ).execute()

    return resp["webViewLink"]


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("files", nargs="+", help="APK/AAB paths to upload")
    ap.add_argument("--folder", default=DEFAULT_FOLDER_ID, help="destination folder id")
    ap.add_argument("--replace", action="store_true",
                    help="replace a same-named file instead of creating a duplicate")
    ap.add_argument("--anyone", action="store_true",
                    help="also grant 'anyone with the link' read access")
    args = ap.parse_args()

    svc = get_service()

    links = []
    for path in args.files:
        try:
            links.append((os.path.basename(path),
                          upload(svc, path, args.folder, args.replace, args.anyone)))
        except HttpError as err:
            if "accessNotConfigured" in str(err) or "has not been used in project" in str(err):
                sys.exit(
                    "\nThe Google Drive API is not enabled for this project.\n"
                    "Enable it in the Cloud Console (APIs & Services -> Library -> "
                    "Google Drive API), wait a minute, then retry."
                )
            if "storageQuotaExceeded" in str(err):
                sys.exit(
                    "\nstorageQuotaExceeded - this happens with service accounts, which "
                    "have no Drive quota.\nUse OAuth user credentials (as this script "
                    "does) or a Shared Drive."
                )
            raise

    print("\nLinks for the Confluence builds page:")
    for name, link in links:
        print(f"  {name}\n    {link}")


if __name__ == "__main__":
    main()
