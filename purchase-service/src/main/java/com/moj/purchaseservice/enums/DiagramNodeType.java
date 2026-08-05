package com.moj.purchaseservice.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum DiagramNodeType {
    @JsonProperty("hook")
    HOOK,
    @JsonProperty("videoPart")
    VIDEO_PART,
    @JsonProperty("cta")
    CTA
}