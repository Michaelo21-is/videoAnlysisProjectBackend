import re
from datetime import datetime

def extractedVideoDetailsFromTiktok(item: dict) -> dict:
    author_meta = item.get("authorMeta") or {}
    video_meta = item.get("videoMeta") or {}

    return {
        "id": item.get("id"),
        "caption": item.get("text"),
        "url": item.get("webVideoUrl"),

        "views": item.get("playCount", 0),
        "likes": item.get("diggCount", 0),
        "comments": item.get("commentCount", 0),
        "shares": item.get("shareCount", 0),

        "createdAt": item.get("createTimeISO"),

        "thumbnail": video_meta.get("coverUrl"),

        "creator": {
            "name": author_meta.get("name"),
            "nickname": author_meta.get("nickName"),
            "profileUrl": author_meta.get("profileUrl"),
            "avatar": author_meta.get("avatar"),
            "followers": author_meta.get("fans", 0),
        },

        "duration": video_meta.get("duration", 0),
    }

def extractedVideoDetailsFromInstagram(item: dict) -> dict:
    return {
        "id": item.get("id"),
        "caption": item.get("caption"),
        "url": item.get("url"),

        # Instagram מציין ש-videoViewCount deprecated,
        # לכן עדיף videoPlayCount
        "views": item.get("videoPlayCount") or 0,

        "likes": item.get("likesCount") or 0,
        "comments": item.get("commentsCount") or 0,
        "shares": item.get("reshareCount") or 0,

        "createdAt": item.get("timestamp"),

        "thumbnail": item.get("displayUrl"),

        "creator": {
            "name": item.get("ownerUsername"),
            "nickname": item.get("ownerFullName"),
            "profileUrl": (
                f"https://www.instagram.com/{item.get('ownerUsername')}/"
                if item.get("ownerUsername")
                else None
            ),
        },

        "duration": item.get("videoDuration") or 0,

        "videoUrl": item.get("videoUrl"),
        "audioUrl": item.get("audioUrl"),
    }

def extractedVideoDetailsFromX(item: dict) -> dict:
    author = item.get("author") or {}

    return {
        "id": item.get("id"),
        "caption": item.get("text"),
        "url": item.get("url"),

        "views": item.get("viewCount") or 0,
        "likes": item.get("likeCount") or 0,
        "comments": item.get("replyCount") or 0,
        "shares": item.get("retweetCount") or 0,
        "quotes": item.get("quoteCount") or 0,

        "createdAt": item.get("createdAt"),

        "creator": {
            "name": author.get("userName"),
            "nickname": author.get("name"),
            "profileUrl": author.get("url"),
            "avatar": author.get("profilePicture"),
            "followers": author.get("followers") or 0,
        },
    }

def extractedVideoDetailsFromFacebook(item: dict) -> dict:
    author = item.get("author") or {}

    time_and_views = item.get("time_and_views_raw")

    return {
        "id": item.get("video_id"),

        "caption": (
                item.get("description")
                or item.get("title")
        ),

        "url": item.get("video_url"),

        "views": _parse_facebook_views(time_and_views),

        # ה-Actor הזה לא מחזיר אותם כרגע
        "likes": 0,
        "comments": 0,
        "shares": 0,

        "createdAt": _parse_facebook_created_at(
            time_and_views
        ),

        "thumbnail": item.get("thumbnail"),

        "creator": {
            "name": author.get("name"),
            "nickname": None,
            "profileUrl": author.get("url"),
            "avatar": None,
            "followers": 0,
            "isVerified": author.get("is_verified", False),
        },

        # שימושי ל-Facebook כי יש title נפרד
        "title": item.get("title"),
    }

def _parse_facebook_views(raw_value: str | None) -> int:
    if not raw_value:
        return 0

    match = re.search(
        r"([\d,.]+)\s*([KMB]?)\s*views?",
        raw_value,
        re.IGNORECASE,
    )

    if not match:
        return 0

    number = float(match.group(1).replace(",", ""))
    suffix = match.group(2).upper()

    multiplier = {
        "": 1,
        "K": 1_000,
        "M": 1_000_000,
        "B": 1_000_000_000,
    }

    return int(number * multiplier.get(suffix, 1))

def _parse_facebook_created_at(raw_value: str | None) -> str | None:
    if not raw_value:
        return None

    # Example:
    # "2 Mar 2022 · 24K views"
    date_part = raw_value.split("·")[0].strip()

    formats = (
        "%d %b %Y",  # 2 Mar 2022
        "%b %d, %Y",  # Mar 2, 2022
        "%b %d %Y",  # Mar 2 2022
    )

    for date_format in formats:
        try:
            return datetime.strptime(
                date_part,
                date_format,
            ).date().isoformat()
        except ValueError:
            continue

    # אם Facebook החזיר פורמט אחר,
    # עדיף לא לאבד את המידע.
    return date_part
def extractedVideoDetailsFromYoutube(item: dict) -> dict:
    return {
        "id": item.get("id"),

        "caption": item.get("text") or item.get("title"),

        "url": item.get("url"),

        "views": item.get("viewCount") or 0,
        "likes": item.get("likes") or 0,
        "comments": item.get("commentsCount") or 0,

        # YouTube לא מחזיר shares ב-output הזה
        "shares": 0,

        "createdAt": item.get("date"),

        "thumbnail": item.get("thumbnailUrl"),

        "creator": {
            "name": item.get("channelName"),
            "nickname": None,
            "profileUrl": item.get("channelUrl"),
            "avatar": None,
            "followers": item.get("numberOfSubscribers") or 0,
        },

        "duration": item.get("duration"),

        "title": item.get("title"),
    }