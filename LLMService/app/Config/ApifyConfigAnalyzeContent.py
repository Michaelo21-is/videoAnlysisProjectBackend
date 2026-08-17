from apify_client import ApifyClient
import os
from app.Util.ExtractVideoDetailsFromApify import (extractedVideoDetailsFromTiktok
, extractedVideoDetailsFromInstagram, extractedVideoDetailsFromX, extractedVideoDetailsFromFacebook,
   extractedVideoDetailsFromYoutube)
api_key = os.environ["APIFY_API"]
client = ApifyClient(api_key)

def get_first_dataset_item(result) -> dict:
    dataset_id = result.default_dataset_id
    items = client.dataset(dataset_id).iterate_items()
    item = next(items, None)
    if item is None:
        raise ValueError("Apify did not return any dataset items")
    return item
def search_tiktok_videos(niche: str) -> list[dict]:
    run_input = {
        "hashtags": None,
        "profiles": None,
        "postURLs": None,

        "searchQueries": [niche],
        "resultsPerPage": 50,

        "videoSearchSorting": "MOST_RELEVANT",
        "videoSearchDateFilter": "ALL_TIME",

        "shouldDownloadVideos": False,
        "shouldDownloadCovers": False,
        "shouldDownloadSlideshowImages": False,
        "shouldDownloadAvatars": False,
        "shouldDownloadMusicCovers": False,

        "scrapeRelatedVideos": False,
        "scrapeAdditionalAuthorMeta": False,

        "commentsPerPost": 0,
        "topLevelCommentsPerPost": 0,

        "proxyCountryCode": "None",
    }

    run = client.actor("GdWCkxBtKWOsKjdch").call(run_input=run_input)

    videos = []

    for item in client.dataset(
        run["defaultDatasetId"]
    ).iterate_items():

        if item.get("errorCode"):
            continue

        video_details = extractedVideoDetailsFromTiktok(item)

        videos.append(video_details)

    return videos

def search_instagram_videos(niche: str) -> list[dict]:
    run_input = {
        "resultsType": "details",
        "directUrls": None,

        "search": niche,
        "searchType": "hashtag",
        "searchLimit": 50,

        "addParentData": False,
    }
    run = client.actor("shu8hvrXbJbY3Eb9W").call(run_input=run_input)
    videos = []

    for item in client.dataset( run["defaultDatasetId"]).iterate_items():
        if item.get("error"):
            continue

        video_details = extractedVideoDetailsFromInstagram(item)

        videos.append(video_details)

    return videos

def search_x_videos(niche: str) -> list[dict]:
    run_input = {
        "startUrls": None,
        "twitterHandles": None,
        "conversationIds": None,

        "searchTerms": [
            niche
        ],

        "maxItems": 50,

        # בשביל discovery הייתי מעדיף את זה
        "sort": "Latest + Top",

        "tweetLanguage": "en",

        # חשוב מאוד אצלך
        "onlyVideo": True,

        "onlyVerifiedUsers": None,
        "onlyTwitterBlue": None,
        "onlyImage": None,
        "onlyQuote": None,

        "minimumRetweets": None,
        "minimumFavorites": None,
        "minimumReplies": None,

        "includeSearchTerms": True,
    }
    run = client.actor("61RPP7dywgiy0JPD0").call(run_input=run_input)
    videos = []
    for item in client.dataset(run["defaultDatasetId"]).iterate_items():
        video_details = extractedVideoDetailsFromX(item)
        videos.append(video_details)
    return videos

def search_facebook_videos(niche: str) -> list[dict]:
    run_input = {
        "query": niche,
        "recent_videos": None,
        "location_uid": None,
        "start_date": None,
        "end_date": None,
        "maxResults": 50,
    }
    run = client.actor("i3bvo5XREqhCpa2f8").call(run_input=run_input)
    videos = []
    for item in client.dataset(run["defaultDatasetId"]).iterate_items():
        video_details = extractedVideoDetailsFromFacebook(item)
        videos.append(video_details)
    return videos

def search_youtube_videos(niche: str)-> list[dict]:
    run_input = {
        "searchQueries": [niche],

        # Regular YouTube videos
        "maxResults": 0,

        # Only Shorts
        "maxResultsShorts": 50,

        "maxResultStreams": 0,

        "sortingOrder": "relevance",

        "dateFilter": "week",

        "downloadSubtitles": False,
        "aiVideoDescription": False,
        "aiVideoSummary": False,
        "saveSubsToKVS": False,
    }

    run = client.actor("h7sDV53CddomktSi5").call(run_input=run_input)

    videos = []

    for item in client.dataset(
            run["defaultDatasetId"]
    ).iterate_items():

        if item.get("error"):
            continue

        videos.append(
            extractedVideoDetailsFromYoutube(item)
        )

    return videos

