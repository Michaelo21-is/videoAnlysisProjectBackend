from apify_client import ApifyClient
import os
from app.Util.ExtractVideoUrlFromApify import extract_tiktok_media

api_key = os.environ["APIFY_API"]
client = ApifyClient(api_key)


def get_tiktok_video_url_mp4(url):
    run_input = {
        "postURLs": [url],
        "scrapeRelatedVideos": False,
        "resultsPerPage": 100,
        "shouldDownloadVideos": True,
        "shouldDownloadCovers": False,
        "downloadSubtitlesOptions": "NEVER_DOWNLOAD_SUBTITLES",
        "shouldDownloadSlideshowImages": True,
        "videoKvStoreIdOrName": "tiktok-videos",
    }

    # Run the Actor and wait for it to finish
    run = client.actor("S5h7zRLfKFEr8pdj7").call(run_input=run_input)

    # Fetch and print Actor results from the run's dataset (if there are any)
    for item in client.dataset(run["defaultDatasetId"]).iterate_items():
        return extract_tiktok_media(item=item)
    raise Exception("No video found")