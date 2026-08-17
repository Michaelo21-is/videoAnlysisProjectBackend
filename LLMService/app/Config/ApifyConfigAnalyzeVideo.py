from apify_client import ApifyClient
import os
from typing import Any


from app.Util.ExtractVideoUrlFromApifyVideoAnalyzer import (
    extract_instagram_reel_media,
    extract_tiktok_media,
    extract_X_video_media,
    extract_facebook_video_media,
)
from app.Util.NormalizeUrl import normalize_x_url


api_key = os.environ["APIFY_API"]
client = ApifyClient(api_key)


def get_first_dataset_item(result) -> dict[str, Any]:
    dataset_id = result.default_dataset_id
    items = client.dataset(dataset_id).iterate_items()
    item = next(items, None)

    if item is None:
        raise ValueError("Apify did not return any dataset items")

    return item


def get_tiktok_video_with_url(url: str) -> dict[str, Any]:
    run_input = {
        "postURLs": [url],
        "scrapeRelatedVideos": False,
        "resultsPerPage": 1,
        "shouldDownloadVideos": True,
        "shouldDownloadCovers": False,
        "downloadSubtitlesOptions": "NEVER_DOWNLOAD_SUBTITLES",
        "shouldDownloadSlideshowImages": False,
        "videoKvStoreIdOrName": "tiktok-videos",
    }

    result = client.actor("S5h7zRLfKFEr8pdj7").call(
        run_input=run_input
    )

    item = get_first_dataset_item(result)
    return extract_tiktok_media(item)


def get_instagram_video_with_url(
    reel_url: str,
) -> dict[str, Any]:
    run_input = {
        "username": [reel_url],
        "resultsLimit": 1,
        "skipPinnedPosts": False,
        "skipTrialReels": False,
        "includeSharesCount": False,
        "includeTranscript": False,
        "includeDownloadedVideo": False,
    }

    result = client.actor("xMc5Ga1oCONPmWJIa").call(
        run_input=run_input
    )

    item = get_first_dataset_item(result)

    return extract_instagram_reel_media(item)


def get_x_video_with_url(url: str) -> dict[str, Any]:
    url = normalize_x_url(url)

    run_input = {
        "tweetUrls": [url],
        "includeAllQualities": True,
        "includeMetadata": True,
    }

    result = client.actor("dviQhlVEw0nhjm66N").call(
        run_input=run_input
    )

    item = get_first_dataset_item(result)

    return extract_X_video_media(item)


def get_facebook_video_with_url( url: str,) -> dict[str, Any]:
    run_input = {
        "urls": url,
        "proxyConfiguration": {
            "useApifyProxy": True,
            "apifyProxyGroups": ["RESIDENTIAL"],
        },
    }

    result = client.actor("bd0BAhBSbiGcmv4ho").call(
        run_input=run_input
    )

    item = get_first_dataset_item(result)

    return extract_facebook_video_media(item)