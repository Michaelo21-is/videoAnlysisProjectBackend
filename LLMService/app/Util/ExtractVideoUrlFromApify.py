from typing import Any


def extract_tiktok_media(item: dict[str, Any]) -> dict[str, Any]:
    if error_code := item.get("errorCode"):
        error_message = item.get("error") or "Unknown Apify error"
        raise RuntimeError(f"{error_code}: {error_message}")

    if item.get("isSlideshow") is True:
        slideshow_images = item.get("slideshowImageLinks") or []

        image_urls = []

        for image in slideshow_images:
            if not isinstance(image, dict):
                continue

            image_url = image.get("downloadLink") or image.get("tiktokLink")

            if isinstance(image_url, str) and image_url.startswith(
                ("https://", "http://")
            ):
                image_urls.append(image_url)

        if not image_urls:
            raise ValueError("Apify did not return slideshow images")

        return {
            "type": "slideshow",
            "urls": image_urls,
        }

    video_meta = item.get("videoMeta") or {}
    media_urls = item.get("mediaUrls") or []

    possible_urls = [
        video_meta.get("downloadAddr"),
        video_meta.get("originalDownloadAddr"),
        *media_urls,
    ]

    for video_url in possible_urls:
        if isinstance(video_url, str) and video_url.startswith(
            ("https://", "http://")
        ):
            return {
                "type": "video",
                "urls": [video_url],
            }

    raise ValueError("Apify did not return TikTok media")