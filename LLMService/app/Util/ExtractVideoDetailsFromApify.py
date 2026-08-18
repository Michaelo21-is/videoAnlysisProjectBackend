import re
from datetime import datetime

from app.Schemea.AnalyzeContentSchema import (
    ExtractedVideoDetails,
    CreatorDetails,
)


def extractedVideoDetailsFromTiktok(item: dict) -> ExtractedVideoDetails:
    author_meta = item.get("authorMeta") or {}
    video_meta = item.get("videoMeta") or {}

    return ExtractedVideoDetails(
        id=item.get("id"),
        caption=item.get("text"),
        url=item.get("webVideoUrl"),

        views=item.get("playCount") or 0,
        likes=item.get("diggCount") or 0,
        comments=item.get("commentCount") or 0,
        shares=item.get("shareCount") or 0,

        createdAt=item.get("createTimeISO"),
        thumbnail=video_meta.get("coverUrl"),

        creator=CreatorDetails(
            name=author_meta.get("nickName"),
            profileUrl=author_meta.get("profileUrl"),
            avatar=author_meta.get("avatar"),
            followers=author_meta.get("fans") or 0,
            isVerified=None,
        ),

        duration=video_meta.get("duration"),
    )


def extractedVideoDetailsFromInstagram(item: dict) -> ExtractedVideoDetails:
    return ExtractedVideoDetails(
        id=item.get("id"),
        caption=item.get("caption"),
        url=item.get("url"),

        views=item.get("videoPlayCount") or 0,
        likes=item.get("likesCount") or 0,
        comments=item.get("commentsCount") or 0,
        shares=item.get("reshareCount") or 0,

        createdAt=item.get("timestamp"),
        thumbnail=item.get("displayUrl"),

        creator=CreatorDetails(
            name=item.get("ownerUsername"),
            profileUrl=(
                f"https://www.instagram.com/{item.get('ownerUsername')}/"
                if item.get("ownerUsername")
                else None
            ),
            avatar=None,
            followers=0,
            isVerified=None,
        ),

        duration=item.get("videoDuration"),
    )


def extractedVideoDetailsFromX(item: dict) -> ExtractedVideoDetails:
    author = item.get("author") or {}

    return ExtractedVideoDetails(
        id=item.get("id"),
        caption=item.get("text"),
        url=item.get("url"),

        views=item.get("viewCount") or 0,
        likes=item.get("likeCount") or 0,
        comments=item.get("replyCount") or 0,
        shares=item.get("retweetCount") or 0,

        createdAt=item.get("createdAt"),
        thumbnail=None,

        creator=CreatorDetails(
            name=author.get("userName"),
            profileUrl=author.get("url"),
            avatar=author.get("profilePicture"),
            followers=author.get("followers") or 0,
            isVerified=None,
        ),

        duration=None,
    )


def extractedVideoDetailsFromFacebook(item: dict) -> ExtractedVideoDetails:
    author = item.get("author") or {}
    time_and_views = item.get("time_and_views_raw")

    return ExtractedVideoDetails(
        id=item.get("video_id"),

        caption=(
            item.get("description")
            or item.get("title")
        ),

        url=item.get("video_url"),

        views=_parse_facebook_views(time_and_views),

        likes=0,
        comments=0,
        shares=0,

        createdAt=_parse_facebook_created_at(
            time_and_views
        ),

        thumbnail=item.get("thumbnail"),

        creator=CreatorDetails(
            name=author.get("name"),
            profileUrl=author.get("url"),
            avatar=None,
            followers=0,
            isVerified=author.get("is_verified"),
        ),

        duration=None,
    )


def extractedVideoDetailsFromYoutube(item: dict) -> ExtractedVideoDetails:
    return ExtractedVideoDetails(
        id=item.get("id"),

        caption=(
            item.get("text")
            or item.get("title")
        ),

        url=item.get("url"),

        views=item.get("viewCount") or 0,
        likes=item.get("likes") or 0,
        comments=item.get("commentsCount") or 0,
        shares=0,

        createdAt=item.get("date"),
        thumbnail=item.get("thumbnailUrl"),

        creator=CreatorDetails(
            name=item.get("channelName"),
            profileUrl=item.get("channelUrl"),
            avatar=None,
            followers=item.get("numberOfSubscribers") or 0,
            isVerified=None,
        ),

        duration=item.get("duration"),
    )


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

    number = float(
        match.group(1).replace(",", "")
    )

    suffix = match.group(2).upper()

    multiplier = {
        "": 1,
        "K": 1_000,
        "M": 1_000_000,
        "B": 1_000_000_000,
    }

    return int(
        number * multiplier.get(suffix, 1)
    )


def _parse_facebook_created_at(
    raw_value: str | None,
) -> str | None:

    if not raw_value:
        return None

    date_part = raw_value.split("·")[0].strip()

    formats = (
        "%d %b %Y",
        "%b %d, %Y",
        "%b %d %Y",
    )

    for date_format in formats:
        try:
            return datetime.strptime(
                date_part,
                date_format,
            ).date().isoformat()

        except ValueError:
            continue

    return date_part