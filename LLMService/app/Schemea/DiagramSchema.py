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
    private: bool | None = None

    edited_nodes: list[DiagramNode] | None = Field(alias="editedNodes")
    edited_arrows: list[DiagramArrow] | None = Field(alias="editedArrows")

    removed_nodes: list[str] | None = Field(alias="removedNodes", default=None)
    removed_arrows: list[str] | None = Field(alias="removedArrows", default=None)

    added_nodes: list[DiagramNode] | None = Field(alias="addedNodes")
    added_arrows: list[DiagramArrow] | None = Field(alias="addedArrows")



class DiagramResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    name: str
    prompt: str = ""
    private: bool = False

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
