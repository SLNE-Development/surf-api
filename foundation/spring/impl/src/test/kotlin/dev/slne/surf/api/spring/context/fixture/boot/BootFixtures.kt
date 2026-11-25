package dev.slne.surf.api.spring.context.fixture.boot

// Each subpackage below is one test plugin; BootConditionsTest scans them one at a time. This
// package itself is never scanned.

/**
 * A class that a test hides from the plugin's class loader.
 */
class HiddenType

/**
 * Registered in the root context of BootConditionsTest.
 */
class RootMarker

interface Ticker

data class Named(val name: String)

const val HIDDEN_TYPE = "dev.slne.surf.api.spring.context.fixture.boot.HiddenType"
const val MARKER_RESOURCE = "fixture/boot/marker.txt"
const val FEATURE_PROPERTY = "surf.test.boot.feature"
