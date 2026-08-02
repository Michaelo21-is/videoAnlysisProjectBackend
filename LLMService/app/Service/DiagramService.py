import logging
from datetime import datetime
from uuid import UUID
from fastapi import HTTPException, status

from app.Config.DatabaseConfig import get_database
from app.Model.DiagramModel import DiagramDocument
from app.Schemea.DiagramSchemea import (DiagramCreate, DiagramNameListResponse
, DiagramNameResponse, UserDiagramsResponse, DiagramResponse)

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
                private=diagram.private,
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
    def get_user_diagrams(self, user_id: UUID, page: int = 1, limit: int = 9, firstTimeRequest: bool = False) -> UserDiagramsResponse:
        try:
            skip = (page - 1) * limit
            totalPages: int | None = None
            total_diagrams: int | None = None
            if page == 1 and firstTimeRequest == True:
                total_diagrams = self.diagram_collection.count_documents({"userId": str(user_id)})
                totalPages = (total_diagrams + limit - 1) // limit  # Calculates the total number of pages, rounding up.
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
                .skip(skip)
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

            return UserDiagramsResponse(
                diagrams=diagrams,
                totalPages=totalPages,
                sumOfDiagram= total_diagrams,
            )

        except Exception:
            logger.exception(
                "Error getting user diagrams: user_id=%s",
                user_id,
            )
            raise

    def get_diagram_by_query(
            self,
            query: str,
            user_id: UUID,
            limit: int = 9,
            page: int = 1,
    ) -> UserDiagramsResponse:
        try:
            skip = (page - 1) * limit
            cleaned_query = query.strip()

            search_filter = {
                "userId": str(user_id),
                "name": {
                    "$regex": f"^{cleaned_query}",
                    "$options": "i",
                },
            }

            total_diagrams = self.diagram_collection.count_documents(
                search_filter
            )

            total_pages = (
                                  total_diagrams + limit - 1
                          ) // limit

            documents = list(
                self.diagram_collection
                .find(
                    search_filter,
                    {
                        "name": 1,
                        "createdAt": 1,
                    },
                )
                .sort("createdAt", -1)
                .skip(skip)
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

            return UserDiagramsResponse(
                diagrams=diagrams,
                totalPages=total_pages,
                sumOfDiagram=total_diagrams,
            )

        except Exception:
            logger.exception(
                "Error searching user diagrams: user_id=%s, query=%s",
                user_id,
                query,
            )
            raise
    def get_diagram(self, diagram_id: str, user_id: UUID) -> DiagramResponse:
        try:



            document = self.diagram_collection.find_one({
                "_id": diagram_id,
            })

            if document is None:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Diagram not found",
                )
            if document["private"]:
                if document["userId"] != str(user_id):
                    raise HTTPException(
                        status_code=status.HTTP_403_FORBIDDEN,
                        detail="You are not authorized to access this diagram",
                    )

            return DiagramResponse(
                name=document["name"],
                prompt=document["prompt"],
                nodes=document["nodes"],
                arrows=document["arrows"],
                createdAt=document["createdAt"],
                updatedAt=document["updatedAt"],
            )

        except HTTPException:
            raise

        except Exception:
            logger.exception(
                "Error getting diagram: diagram_id=%s, user_id=%s",
                diagram_id,
                user_id,
            )
            raise
