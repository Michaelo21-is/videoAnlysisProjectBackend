from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Header, Query, status

from app.Schemea.DiagramSchemea import DiagramCreate, DiagramNameListResponse
from app.Service.DiagramService import DiagramService


router = APIRouter(prefix="/api/llm", tags=["LLM"])

diagram_service = DiagramService()


@router.post(
    "/create-diagram",
    status_code=status.HTTP_201_CREATED,
)
def create_diagram(
    diagram: DiagramCreate,
    user_id: Annotated[UUID, Header(alias="X-USER-ID")],
) -> None:
    diagram_service.create_diagram(
        diagram=diagram,
        user_id=user_id,
    )


@router.get("/user-diagrams/recent",status_code=status.HTTP_200_OK,)
def get_user_diagrams(   user_id: Annotated[UUID, Header(alias="X-USER-ID")] ) -> DiagramNameListResponse:
    return diagram_service.get_user_diagrams(
        user_id=user_id,
    )