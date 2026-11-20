# F-Droid flavor only: keep class and member names.
# R8's choice of obfuscated names isn't always the same between machines (two classes can swap
# short names), which breaks reproducible builds. Shrinking and optimisation stay on.
-dontobfuscate
