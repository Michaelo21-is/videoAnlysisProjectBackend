from apify_client import ApifyClient
import os
from app.Util.ExtractVideoUrlFromApify import (
    extract_instagram_reel_media,
    extract_tiktok_media,
    extract_X_video_media,
)
from typing import Any

api_key = os.environ["APIFY_API"]
client = ApifyClient(api_key)


def get_tiktok_video_with_url(url) -> dict[str, Any]:
    run_input = {
        "postURLs": [url],
        "scrapeRelatedVideos": False,
        "resultsPerPage": 1,
        "shouldDownloadVideos": True,
        "shouldDownloadCovers": False,
        "downloadSubtitlesOptions": "NEVER_DOWNLOAD_SUBTITLES",
        "shouldDownloadSlideshowImages": True,
        "videoKvStoreIdOrName": "tiktok-videos",
    }

    # Run the Actor and wait for it to finish
    result = client.actor("S5h7zRLfKFEr8pdj7").call(run_input=run_input)
    items = client.dataset(result["defaultDatasetId"]).iterate_items()
    item = next(items, None)
    if item is None:
        raise ValueError("No Instagram Reel was found")
    return extract_tiktok_media(item)

def get_instagram_video_with_url(reel_url: str) -> dict[str: Any]:
    run_input = {
        "username": [reel_url],
        "resultsLimit": 1,
        "onlyPostsNewerThan": None,
        "skipPinnedPosts": False,
        "skipTrialReels": False,
        "includeSharesCount": False,
        "includeTranscript": False,
        "includeDownloadedVideo": True,
    }
    result = client.actor("xMc5Ga1oCONPmWJIa").call(run_input=run_input)
    items = client.dataset(result["defaultDatasetId"] ).iterate_items()
    item = next(items, None)

    if item is None:
        raise ValueError("No Instagram Reel was found")
    return extract_instagram_reel_media(item)


def get_x_video_with_url(url: str) -> dict[str, Any]:
    run_input = {
        "tweetUrls": [url],
        "includeAllQualities": True,
        "includeMetadata": True,
    }

    result = client.actor("dviQhlVEw0nhjm66N").call(
        run_input=run_input
    )

    for item in client.dataset(result["defaultDatasetId"]).iterate_items():
        return extract_X_video_media(item)

    raise ValueError("No Twitter video was found")
def get_facebook_video_with_url(url: str) -> dict[str, Any]:
    run_input = {
        "url": "https://www.facebook.com/watch/?v=123456789",
        "urls": None,
        "proxyConfiguration": {
            "useApifyProxy": True,
            "apifyProxyGroups": ["RESIDENTIAL"],
        },
    }
    result = client.actor("bd0BAhBSbiGcmv4ho").call(run_input=run_input)
    items = client.dataset(result["defaultDatasetId"]).iterate_items()
    items = next(items, None)
    if not items:
        raise ValueError("No Facebook video was found")
    return items

