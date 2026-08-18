from math import log1p

from app.Schemea.AnalyzeContentSchema import ExtractedVideoDetails


def select_top_relevant_videos(
    videos: list[ExtractedVideoDetails],
    limit: int = 15,
) -> list[ExtractedVideoDetails]:

    if not videos:
        return []

    valid_videos = [
        video
        for video in videos
        if video.url
    ]

    if len(valid_videos) <= limit:
        return valid_videos

    max_views = max(
        (log1p(_safe_number(v.views)) for v in valid_videos),
        default=1,
    )

    max_likes = max(
        (log1p(_safe_number(v.likes)) for v in valid_videos),
        default=1,
    )

    max_comments = max(
        (log1p(_safe_number(v.comments)) for v in valid_videos),
        default=1,
    )

    max_shares = max(
        (log1p(_safe_number(v.shares)) for v in valid_videos),
        default=1,
    )

    scored_videos = []

    total_videos = len(valid_videos)

    for index, video in enumerate(valid_videos):

        relevance_score = (
            1 - (index / (total_videos - 1))
            if total_videos > 1
            else 1
        )

        views_score = _normalize(
            video.views,
            max_views,
        )

        likes_score = _normalize(
            video.likes,
            max_likes,
        )

        comments_score = _normalize(
            video.comments,
            max_comments,
        )

        shares_score = _normalize(
            video.shares,
            max_shares,
        )

        final_score = (
            relevance_score * 0.40
            + views_score * 0.25
            + likes_score * 0.12
            + comments_score * 0.08
            + shares_score * 0.15
        )

        scored_videos.append(
            (final_score, video)
        )

    scored_videos.sort(
        key=lambda item: item[0],
        reverse=True,
    )

    return [
        video
        for _, video in scored_videos[:limit]
    ]


def _normalize(
    value: int | float | None,
    max_value: float,
) -> float:

    value = _safe_number(value)

    if value <= 0 or max_value <= 0:
        return 0

    return log1p(value) / max_value


def _safe_number(value) -> float:
    try:
        return float(value or 0)
    except (TypeError, ValueError):
        return 0