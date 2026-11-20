# F-Droid flavor only. R8 isn't fully deterministic between machines: its obfuscator sometimes
# gives two classes each other's short names, and its optimiser sometimes narrows a parameter type
# differently. Either breaks reproducible builds, so this flavor only shrinks (removes unused
# code and resources) and keeps names and code as compiled.
-dontobfuscate
-dontoptimize
