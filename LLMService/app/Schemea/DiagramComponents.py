from enum import Enum

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