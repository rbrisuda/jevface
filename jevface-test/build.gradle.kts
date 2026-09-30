plugins {
    id("jevface.library-conventions")
}

description = "Test support for jevface: a type-safe stub JudgmentEngine, no Jev API key needed."

dependencies {
    api(project(":jevface-core"))
}
