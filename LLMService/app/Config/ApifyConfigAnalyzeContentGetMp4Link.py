from apify_client import ApifyClient
import os
from app.Schemea.AnalyzeContentSchema import AnalyzeContentScrapeResponse


api_key = os.environ["APIFY_API"]
client = ApifyClient(api_key)


def get_mp4_link_from_facebook(video_details: AnalyzeContentScrapeResponse) -> list[str]:

    facebook_urls = [
        video.url
        for video in video_details.videos
        if video.url
    ]

    run_input = {
        "urls": facebook_urls,
        "proxyConfiguration": {
            "useApifyProxy": True,
            "apifyProxyGroups": ["RESIDENTIAL"],
        },
    }

    run = client.actor(
        "bd0BAhBSbiGcmv4ho"
    ).call(run_input=run_input)

    download_urls = []

    for item in client.dataset(
        run.default_dataset_id
    ).iterate_items():

        download_url = item.get("download_url")

        if download_url:
            download_urls.append(download_url)

    return download_urls
def get_mp4_link_from_x(
    video_details: AnalyzeContentScrapeResponse
) -> list[str]:

    x_urls = [
        video.url
        for video in video_details.videos
        if video.url
    ]

    run_input = {
        "tweetUrls": x_urls,
        "videoOnly": True,
        "includeAllQualities": False,
        "includeMetadata": False,
    }

    run = client.actor("dviQhlVEw0nhjm66N").call(run_input=run_input)

    download_urls = []

    for item in client.dataset(run.default_dataset_id).iterate_items():

        if item.get("status") != "success":
            continue

        if item.get("mediaType") != "video":
            continue

        download_url = item.get("downloadUrl")

        if download_url:
            download_urls.append(download_url)

    return download_urls