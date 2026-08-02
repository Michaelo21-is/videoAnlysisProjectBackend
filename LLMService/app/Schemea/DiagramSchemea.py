from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field

from app.Schemea.DiagramComponents import DiagramArrow, DiagramNode


class DiagramCreate(BaseModel):
    name: str
    prompt: str = ""
    private: bool = False

    nodes: list[DiagramNode] = Field(default_factory=list)
    arrows: list[DiagramArrow] = Field(default_factory=list)


class DiagramUpdate(BaseModel):
    id: str

    name: str | None = None
    prompt: str | None = None

    nodes: list[DiagramNode] | None = None
    arrows: list[DiagramArrow] | None = None


class DiagramResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    name: str
    prompt: str = ""

    nodes: list[DiagramNode] = Field(default_factory=list)
    arrows: list[DiagramArrow] = Field(default_factory=list)

    created_at: datetime = Field(alias="createdAt")
    updated_at: datetime = Field(alias="updatedAt")

class DiagramNameResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    id: str = Field(alias="_id")
    name: str
    created_at: datetime = Field(alias="createdAt")


class DiagramNameListResponse(BaseModel):
    diagrams: list[DiagramNameResponse]
class UserDiagramsResponse(BaseModel):
    diagrams: list[DiagramNameResponse]
    total_pages: int | None = Field(default=None, alias="totalPages")
    sum_of_diagram: int | None = Field(default=None, alias="sumOfDiagram")
