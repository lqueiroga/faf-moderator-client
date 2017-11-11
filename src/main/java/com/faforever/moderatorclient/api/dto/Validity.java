package com.faforever.moderatorclient.api.dto;

public enum Validity {
    // Order is crucial
    VALID,
    TOO_MANY_DESYNCS,
    WRONG_VICTORY_CONDITION,
    NO_FOG_OF_WAR,
    CHEATS_ENABLED,
    PREBUILT_ENABLED,
    NORUSH_ENABLED,
    BAD_UNIT_RESTRICTIONS,
    BAD_MAP,
    TOO_SHORT,
    BAD_MOD,
    COOP_NOT_RANKED,
    MUTUAL_DRAW,
    SINGLE_PLAYER,
    FFA_NOT_RANKED,
    UNEVEN_TEAMS_NOT_RANKED,
    UNKNOWN_RESULT;
}
