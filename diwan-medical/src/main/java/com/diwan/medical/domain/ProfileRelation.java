package com.diwan.medical.domain;

/** How a person relates to the account holder. Every account has exactly one SELF profile. */
public enum ProfileRelation {
    SELF, SPOUSE, CHILD, PARENT, SIBLING, OTHER
}
