import logging
from datetime import datetime
from uuid import UUID

from app.Config.DatabaseConfig import get_database
from app.Model.DiagramModel import DiagramDocument
from app.Schemea.DiagramSchemea import DiagramCreate, DiagramNameListResponse, DiagramNameResponse


logger = logging.getLogger(__name__)


class DiagramService:
    def __init__(self):
        try:
            database = get_database()
            self.diagram_collection = database["diagram"]

        except Exception:
            logger.exception("Error initializing diagram database collection")
            raise

    def create_diagram( self, diagram: DiagramCreate, user_id: UUID  ) -> None:
        try:
            now = datetime.now()

            diagram_document = DiagramDocument(
                userId=str(user_id),
                name=diagram.name,
                prompt=diagram.prompt,
                nodes=[
                    node.model_dump(by_alias=True)
                    for node in diagram.nodes
                ],
                arrows=[
                    arrow.model_dump(by_alias=True)
                    for arrow in diagram.arrows
                ],
                createdAt=now,
                updatedAt=now,
            )

            document_data = diagram_document.model_dump(by_alias=True)

            result = self.diagram_collection.insert_one(document_data)

            logger.info(
                "Diagram created successfully: diagram_id=%s, user_id=%s",
                result.inserted_id,
                user_id,
            )

        except Exception:
            logger.exception(
                "Error creating diagram: user_id=%s, name=%s",
                user_id,
                diagram.name,
            )
            raise
    def get_recent_user_diagrams(self, user_id: UUID, limit: int = 5,) -> DiagramNameListResponse:
        try:

            documents = list(
                self.diagram_collection
                .find(
                    {"userId": str(user_id)},
                    {
                        "name": 1,
                        "createdAt": 1,
                    },
                )
                .sort("createdAt", -1)
                .limit(limit)
            )


            diagrams = [
                DiagramNameResponse(
                    _id=str(document["_id"]),
                    name=document["name"],
                    createdAt=document["createdAt"],
                )
                for document in documents
            ]

            return DiagramNameListResponse(
                diagrams=diagrams,
            )

        except Exception:
            logger.exception(
                "Error getting user diagrams: user_id=%s",
                user_id,
            )
            raise