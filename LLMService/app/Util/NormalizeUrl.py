from urllib.parse import urlsplit, urlunsplit

def normalize_x_url(url: str) -> str:
    parsed = urlsplit(url.strip())

    return urlunsplit((
        parsed.scheme,
        parsed.netloc,
        parsed.path,
        "",
        "",
    ))