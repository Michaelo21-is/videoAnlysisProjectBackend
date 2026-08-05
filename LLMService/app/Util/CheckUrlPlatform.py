from enum import Enum
from urllib.parse import urlparse

class Platform(str, Enum):
    YOUTUBE = "YOUTUBE"
    TIKTOK = "TIKTOK"
    FACEBOOK = "FACEBOOK"
    INSTAGRAM = "INSTAGRAM"
    X="X"
    NOT_SUPPORT = "NOT_SUPPORT"

def check_url_platform(url: str) -> Platform:
    if not url or not url.strip():
        return Platform.NOT_SUPPORT

    normalized_url = url.strip()

    if "://" not in normalized_url:
        normalized_url = f"https://{normalized_url}"

    parsed_url = urlparse(normalized_url)
    hostname = parsed_url.hostname

    if hostname is None:
        return Platform.NOT_SUPPORT

    hostname = hostname.lower()

    if hostname.startswith("www."):
        hostname = hostname[4:]

    if hostname == "youtube.com" or hostname.endswith(".youtube.com"):
        return Platform.YOUTUBE

    if hostname == "youtu.be":
        return Platform.YOUTUBE

    if hostname == "tiktok.com" or hostname.endswith(".tiktok.com"):
        return Platform.TIKTOK

    if hostname == "instagram.com" or hostname.endswith(".instagram.com"):
        return Platform.INSTAGRAM

    if (
        hostname == "facebook.com"
        or hostname.endswith(".facebook.com")
        or hostname == "fb.watch"
    ):
        return Platform.FACEBOOK

    if (
        hostname == "x.com"
        or hostname.endswith(".x.com")
        or hostname == "twitter.com"
        or hostname.endswith(".twitter.com")
    ):
        return Platform.X

    return Platform.NOT_SUPPORT