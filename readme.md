This is cursed. If you need to migrate from one set of intermediaries to another, you've come to the right place, provided your original intermediaries are merged.

It probably would be pretty easy to modify this hackjob of a program to support legacy formats, but I've swiftly decided that my extremely limited time is best spent elsewhere.

I made the program nice enough to at least tell you what you're doing wrong.

You need:
- A target intermediary set (`mappings.tiny`)
- An enigma folder you want to convert (`mappings`)
- Java 17 or above, because I abused records again.

Run the program like any other java program, or via commandline in case the thing doesn't work first try and you want to try debugging.

Tested with BINY, compat with anything using cursed-formatted mappings should work just fine too.

[Compiled file, for folk who are too lazy to run "gradlew build"](https://maven.glass-launcher.net/#/releases/net/glasslauncher/intermediary-ruiner/1.0+thisisawful)