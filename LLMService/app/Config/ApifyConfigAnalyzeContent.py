from apify_client import ApifyClient
import os
from app.Util.ExtractVideoDetailsFromApify import (extractedVideoDetailsFromTiktok
, extractedVideoDetailsFromInstagram, extractedVideoDetailsFromX, extractedVideoDetailsFromFacebook,
   extractedVideoDetailsFromYoutube)
api_key = os.environ["APIFY_API"]
client = ApifyClient(api_key)
from app.Schemea.AnalyzeContentSchema import ExtractedVideoDetails

def get_first_dataset_item(result) -> dict:
    dataset_id = result.default_dataset_id
    items = client.dataset(dataset_id).iterate_items()
    item = next(items, None)
    if item is None:
        raise ValueError("Apify did not return any dataset items")
    return item
def search_tiktok_videos(queries: list[str]) -> list[ExtractedVideoDetails]:
    run_input = {
        "hashtags": [],
        "resultsPerPage": 50,

        "profiles": [],
        "profileScrapeSections": ["videos"],
        "profileSorting": "latest",
        "excludePinnedPosts": False,

        "searchQueries": queries,
        "searchSection": "",
        "maxProfilesPerQuery": 10,

        "videoSearchSorting": "MOST_RELEVANT",
        "videoSearchDateFilter": "ALL_TIME",

        "scrapeRelatedSearchWords": False,

        "postURLs": [],

        "scrapeRelatedVideos": False,
        "scrapeAdditionalAuthorMeta": False,

        "shouldDownloadVideos": False,
        "shouldDownloadCovers": False,
        "shouldDownloadSlideshowImages": False,
        "shouldDownloadAvatars": False,
        "shouldDownloadMusicCovers": False,

        "downloadSubtitlesOptions": "NEVER_DOWNLOAD_SUBTITLES",

        "aiVideoDescription": False,
        "aiVideoSummary": False,

        "commentsPerPost": 0,
        "topLevelCommentsPerPost": 0,
        "maxRepliesPerComment": 0,

        "proxyCountryCode": "None",
    }

    run = client.actor("GdWCkxBtKWOsKjdch").call(run_input=run_input)

    videos = []

    for item in client.dataset(run.default_dataset_id).iterate_items():

        if item.get("errorCode"):
            continue

        video_details = extractedVideoDetailsFromTiktok(item)

        videos.append(video_details)

    return videos


def search_instagram_videos(queries: list[str]) -> list[ExtractedVideoDetails]:
    run_input = {
        "resultsType": "details",
        "directUrls": None,

        "search": queries,
        "searchType": "hashtag",
        "searchLimit": 50,

        "addParentData": False,
    }
    run = client.actor("shu8hvrXbJbY3Eb9W").call(run_input=run_input)
    videos = []

    for item in client.dataset(run.default_dataset_id).iterate_items():
        if item.get("error"):
            continue

        video_details = extractedVideoDetailsFromInstagram(item)

        videos.append(video_details)

    return videos

def search_x_videos(queries: list[str]) -> list[ExtractedVideoDetails]:
    run_input = {
        "startUrls": [],
        "twitterHandles": [],
        "conversationIds": [],

        "searchTerms": queries,

        "maxItems": 50,

        "sort": "Latest + Top",
        "tweetLanguage": "en",

        "onlyVideo": True,

        "onlyVerifiedUsers": False,
        "onlyTwitterBlue": False,
        "onlyImage": False,
        "onlyQuote": False,

        "minimumRetweets": 0,
        "minimumFavorites": 0,
        "minimumReplies": 0,

        "includeSearchTerms": True,
    }

    run = client.actor("61RPP7dywgiy0JPD0").call(run_input=run_input)
    videos = []
    for item in client.dataset(run.default_dataset_id).iterate_items():
        if item.get("error"):
            continue
        video_details = extractedVideoDetailsFromX(item)
        videos.append(video_details)
    return videos

def search_facebook_videos(queries: list[str]) -> list[dict]:
    run_input = {
        "query": queries,
        "recent_videos": True,
        "maxResults": 50,
    }
    run = client.actor("i3bvo5XREqhCpa2f8").call(run_input=run_input)
    videos = []
    for item in client.dataset(run.default_dataset_id).iterate_items():
        video_details = extractedVideoDetailsFromFacebook(item)
        videos.append(video_details)
    return videos

def search_youtube_videos(queries: list[str])-> list[ExtractedVideoDetails]:
    run_input = {
        "searchQueries": queries,

        "maxResults": 0,
        "maxResultsShorts": 10,
        "maxResultStreams": 0,

        "dateFilter": "month",

        "downloadSubtitles": False,
        "aiVideoDescription": False,
        "aiVideoSummary": False,
        "saveSubsToKVS": False,
    }

    run = client.actor("h7sDV53CddomktSi5").call(run_input=run_input)

    videos = []

    for item in client.dataset(run.default_dataset_id).iterate_items():

        if item.get("error"):
            continue

        videos.append(
            extractedVideoDetailsFromYoutube(item)
        )

    return videos

