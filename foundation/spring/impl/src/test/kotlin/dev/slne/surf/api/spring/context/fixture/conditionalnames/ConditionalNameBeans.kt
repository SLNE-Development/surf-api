package dev.slne.surf.api.spring.context.fixture.conditionalnames

import dev.slne.surf.api.spring.ConditionalOnPlugin
import org.springframework.stereotype.Service

/**
 * Registered: one of the alternative names is installed.
 */
@Service
@ConditionalOnPlugin("present", "Present")
class AlternativeNameBean

/**
 * Left out: no alternative name is installed.
 */
@Service
@ConditionalOnPlugin("missing", "Missing")
class NoNameBean

/**
 * Registered: each repetition has an installed name.
 */
@Service
@ConditionalOnPlugin("Present")
@ConditionalOnPlugin("Missing", "Present")
class EveryRepetitionBean

/**
 * Left out: the second repetition has no installed name.
 */
@Service
@ConditionalOnPlugin("Present")
@ConditionalOnPlugin("Missing")
class OneRepetitionMissingBean
