from typing import Any
from app.Util.CheckUrlPlatform import UrlPlatform

def extract_tiktok_media(
    item: dict[str, Any],
) -> dict[str, Any]:

    if error_code := item.get("errorCode"):
        error_message = (
            item.get("error")
            or "Unknown Apify error"
        )

        raise RuntimeError(
            f"{error_code}: {error_message}"
        )

    if item.get("isSlideshow") is True:
        raise ValueError(
            "TikTok slideshow posts are not supported"
        )

    video_name = (
            item.get("text")
            or f"TikTok video {item.get('id', '')}"
    )

    video_name = (
            str(video_name).strip()
            or "TikTok video"
    )

    video_meta = item.get("videoMeta") or {}
    media_urls = item.get("mediaUrls") or []

    possible_urls = [
        video_meta.get("downloadAddr"),
        video_meta.get("originalDownloadAddr"),
        *media_urls,
    ]

    for video_url in possible_urls:
        if (
            isinstance(video_url, str)
            and video_url.startswith(
                ("https://", "http://")
            )
        ):
            return {
                "name": video_name,
                "urls": [video_url],
                "platform": UrlPlatform.TIKTOK,
            }

    raise ValueError(
        "Apify did not return the TikTok video download URL"
    )
def extract_instagram_reel_media(
    item: dict[str, Any],
) -> dict[str, Any]:

    if error_code := item.get("error"):
        error_description = (
            item.get("errorDescription")
            or "Unknown Instagram scraping error"
        )

        raise RuntimeError(
            f"{error_code}: {error_description}"
        )

    reel_name = item.get("caption")

    if not isinstance(reel_name, str) or not reel_name.strip():
        raise ValueError(
            "Apify did not return the Instagram Reel name"
        )

    video_url = item.get("videoUrl")
    audio_url = item.get("audioUrl")

    if (
        not isinstance(video_url, str)
        or not video_url.startswith(("https://", "http://"))
    ):
        raise ValueError(
            "Apify did not return the Instagram Reel video URL"
        )

    return {
        "name": reel_name.strip(),
        "urls": [video_url],
        "audioUrl": audio_url,
        "platform": UrlPlatform.INSTAGRAM,
    }
def extract_X_video_media(item: dict[str, Any]) -> dict[str, Any]:
    status = item.get("status")

    if status == "failed":
        raise RuntimeError(
            item.get("error")
            or "Twitter video extraction failed"
        )

    if status == "no_media":
        raise ValueError(
            "The X post does not contain video media"
        )

    if status != "success":
        raise ValueError(
            f"Unexpected Twitter Actor status: {status}"
        )

    if item.get("mediaType") != "video":
        raise ValueError(
            "The X post does not contain a video"
        )

    download_url = item.get("downloadUrl")

    if (
            not isinstance(download_url, str)
            or not download_url.startswith(("https://", "http://"))
    ):
        raise ValueError(
            "Apify did not return the Twitter video download URL"
        )

    video_name = (
            item.get("displayText")
            or item.get("text")
            or f"X video {item.get('tweetId', '')}"
    )

    return {
        "name": str(video_name).strip(),
        "urls": [download_url],
        "platform": UrlPlatform.X
    }
def extract_facebook_video_media(item: dict[str, Any],) -> dict[str, Any]:
    download_url = item.get("download_url")
    if (
        not isinstance(download_url, str)
        or not download_url.startswith(("https://", "http://"))
    ):
        raise ValueError(
            "Apify did not return the Facebook video download URL"
        )

    video_name = (
        item.get("title")
        or item.get("description")
        or f"Facebook video {item.get('id', '')}"
    )

    return {
        "name": str(video_name).strip() or "Facebook video",
        "urls": [download_url],
        "platform": UrlPlatform.FACEBOOK
    }





