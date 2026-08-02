from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Header, Query, status

from app.Schemea.DiagramSchemea import DiagramCreate, DiagramNameListResponse, UserDiagramsResponse, DiagramResponse
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
    return diagram_service.get_recent_user_diagrams(
        user_id=user_id,
    )

@router.get( "/user-diagrams/get-diagrams", status_code=status.HTTP_200_OK,)
def get_user_diagrams( user_id: Annotated[UUID, Header(alias="X-USER-ID")], page: Annotated[int, Query(ge=1)] = 1,
   first_time_request: Annotated[ bool, Query(alias="firstTimeRequest"),] = False,) -> UserDiagramsResponse:
    return diagram_service.get_user_diagrams(
        user_id=user_id,
        page=page,
        firstTimeRequest=first_time_request,
    )
@router.get("/get-diagram/{diagram_id}")
def get_diagram(diagram_id: str, user_id: Annotated[UUID, Header(alias="X-USER-ID")]) -> DiagramResponse:
    return diagram_service.get_diagram(
        diagram_id=diagram_id,
        user_id=user_id,
    )
@router.get( "/user-diagrams/get-diagrams", status_code=status.HTTP_200_OK,)
def get_user_diagrams( user_id: Annotated[UUID, Header(alias="X-USER-ID")], page: Annotated[int, Query(ge=1)] = 1,
   first_time_request: Annotated[ bool, Query(alias="firstTimeRequest"),] = False,) -> UserDiagramsResponse:
    return diagram_service.get_user_diagrams(
        user_id=user_id,
        page=page,
        firstTimeRequest=first_time_request,
    )
@router.get("/user-diagrams/get-diagram-by-query")
def get_diagram_by_query(user_id: Annotated[UUID, Header(alias="X-USER-ID")],
    query: Annotated[str, Query(min_length=1)],page: Annotated[int, Query(ge=1)] = 1,
) -> UserDiagramsResponse:
    return diagram_service.get_diagram_by_query(
        query=query,
        user_id=user_id,
        page=page,
    )
