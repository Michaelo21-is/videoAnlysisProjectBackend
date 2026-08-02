from datetime import datetime
from enum import Enum
from uuid import uuid4
from pydantic import BaseModel, ConfigDict, Field


class DiagramNodeType(str, Enum):
    HOOK = "hook"
    VIDEO_PART = "videoPart"
    CTA = "cta"


class DiagramNode(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    id: str
    type: DiagramNodeType
    text: str = ""

    x: float
    y: float
    width: float
    height: float

    z_index: int = Field(default=1, alias="zIndex")


class DiagramArrow(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    id: str

    source_node_id: str = Field(alias="sourceNodeId")
    target_node_id: str = Field(alias="targetNodeId")

    text: str = ""



class DiagramDocument(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    id: str = Field(
        default_factory=lambda: str(uuid4()),
        alias="_id",
    )
    user_id: str = Field(alias="userId")

    name: str
    prompt: str = ""
    private: bool = False

    nodes: list[DiagramNode] = Field(default_factory=list)
    arrows: list[DiagramArrow] = Field(default_factory=list)

    created_at: datetime = Field(alias="createdAt")
    updated_at: datetime = Field(alias="updatedAt")