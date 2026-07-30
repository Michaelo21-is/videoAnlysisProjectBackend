import os

from pymongo import MongoClient


def get_database():
    connection_string = os.environ["MONGODB_URL"]
    database_name = os.getenv("MONGODB_DATABASE", "video_patterns")

    client = MongoClient(connection_string)

    return client[database_name]