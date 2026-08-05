import logging
from datetime import datetime
from uuid import UUID
from fastapi import HTTPException, status
from typing import Any

from app.Config.DatabaseConfig import get_database
from app.Model.DiagramModel import DiagramDocument
from app.Schemea.DiagramSchema import (DiagramCreate, DiagramNameListResponse
, DiagramNameResponse, UserDiagramsResponse, DiagramResponse, DiagramUpdate)

logger = logging.getLogger(__name__)


class DiagramService:
    def __init__(self):
        try:
            database = get_database()
            self.diagram_collection = database["diagram"]

        except Exception:
            logger.exception("Error initializing diagram database collection")
            raise

    async def create_diagram( self, diagram: DiagramCreate, user_id: UUID  ) -> None:
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

    async def update_diagram(
            self,
            diagram: DiagramUpdate,
            user_id: UUID,
    ) -> None:
        try:
            document = self.diagram_collection.find_one({
                "_id": diagram.id,
                "userId": str(user_id),
            })

            if document is None:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Diagram not found or access denied",
                )

            removed_node_ids = set(diagram.removed_nodes or [])
            removed_arrow_ids = set(diagram.removed_arrows or [])

            # Nodes that should replace existing nodes
            edited_nodes_by_id = {
                node.id: node.model_dump(by_alias=True)
                for node in (diagram.edited_nodes or [])
            }

            # Arrows that should replace existing arrows
            edited_arrows_by_id = {
                arrow.id: arrow.model_dump(by_alias=True)
                for arrow in (diagram.edited_arrows or [])
            }

            nodes = []

            for existing_node in document.get("nodes", []):
                node_id = existing_node["id"]

                # Do not add removed nodes to the new array
                if node_id in removed_node_ids:
                    continue

                # Use the edited node when it exists,
                # otherwise keep the original node
                updated_node = edited_nodes_by_id.get(
                    node_id,
                    existing_node,
                )

                nodes.append(updated_node)

            arrows = []

            for existing_arrow in document.get("arrows", []):
                arrow_id = existing_arrow["id"]

                if arrow_id in removed_arrow_ids:
                    continue

                # Replace the existing arrow when it was edited
                updated_arrow = edited_arrows_by_id.get(
                    arrow_id,
                    existing_arrow,
                )

                # Remove arrows connected to removed nodes
                if (
                        updated_arrow["sourceNodeId"] in removed_node_ids
                        or updated_arrow["targetNodeId"] in removed_node_ids
                ):
                    continue

                arrows.append(updated_arrow)

            # Add newly created nodes
            nodes.extend(
                node.model_dump(by_alias=True)
                for node in (diagram.added_nodes or [])
            )

            # Add newly created arrows
            arrows.extend(
                arrow.model_dump(by_alias=True)
                for arrow in (diagram.added_arrows or [])
            )

            fields_to_update: dict[str, Any] = {
                "nodes": nodes,
                "arrows": arrows,
                "updatedAt": datetime.now(),
            }

            if diagram.name is not None:
                fields_to_update["name"] = diagram.name

            if diagram.prompt is not None:
                fields_to_update["prompt"] = diagram.prompt

            if diagram.private is not None:
                fields_to_update["private"] = diagram.private

            result = self.diagram_collection.update_one(
                {
                    "_id": diagram.id,
                    "userId": str(user_id),
                },
                {
                    "$set": fields_to_update,
                },
            )

            if result.matched_count == 0:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Diagram not found or access denied",
                )

        except HTTPException:
            raise

        except Exception:
            logger.exception(
                "Error updating diagram: diagram_id=%s, user_id=%s",
                diagram.id,
                user_id,
            )
            raise

        except Exception:
            logger.exception(
                "Error updating diagram: diagram_id=%s, user_id=%s",
                diagram.id,
                user_id,
            )
            raise

    async def get_recent_user_diagrams(self, user_id: UUID, limit: int = 5,) -> DiagramNameListResponse:
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
    async def get_user_diagrams(self, user_id: UUID, page: int = 1, limit: int = 9, firstTimeRequest: bool = False) -> UserDiagramsResponse:
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

    async def get_diagram_by_query(
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
    async def get_diagram(self, diagram_id: str, user_id: UUID) -> DiagramResponse:
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
                private=document["private"],
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
