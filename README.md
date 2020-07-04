# CC: Tweaked Javadoc Generator
cct-javadoc is a [Javadoc doclet][doclet], to extract documentation from functions using CC: Tweaked's
[`@LuaFunction`][lua_function] annotation.

## Usage
Using cct-javadoc is a bit of an odditiy, so I'd generally recommend avoiding it if you can help it! This was written
for CC: Tweaked, and so is very tied to its existing toolchain.

### Gradle setup
This project depends on Java 9 (or later), but ForgeGradle only works on Java 8! In order to do this, Gradle should
run on Java 8, but use a more recent `javadoc` executable.

```groovy
repositories {
    maven {
        name "SquidDev"
        url "https://squiddev.cc/maven"
    }
}

configurations {
    cctJavadoc
}

dependencies {
    cctJavadoc 'cc.tweaked:cct-javadoc:1.0.0'
}

task luaJavadoc(type: Javadoc) {
    description "Generates documentation for @LuaFunctions."
    group "documentation"

    source = sourceSets.main.allJava
    destinationDir = file("build/luaJavadoc")
    classpath = sourceSets.main.compileClasspath

    options.docletpath = configurations.cctJavadoc.files as List
    options.doclet = "cc.tweaked.javadoc.LuaDoclet"

    // Attempt to run under Java 11 - this assumes that you have some variable which tells us where it's located.
    // JAVA_HOME_11_X64 is defined by default on GH Actions environments.
    if(System.getProperty("java.version").startsWith("1.")
        && (System.getenv("JAVA_HOME_11_X64") != null || project.hasProperty("java11Home"))) {
        executable = "${System.getenv("JAVA_HOME_11_X64") ?: project.property("java11Home")}/bin/javadoc"
    }
}
```

One can then build documentation using `./gradlew luaJavadoc`.

### Writing documentation
Generally one may just write standard Javadoc comments, and the tool will correctly handle it. Parameters and return
values are _generally_ inferred, and references to other Lua methods will be correctly converted to Lua ones.

However, there are some things you will need to do manually.

 - Any classes with `@LuaFunction` methods do not have a "name" by default, and so are not exported. You must add a
   `@cc.module` annotation to the top of a function:

   ```java
   /** @cc.module my_peripheral */
   public class MyPeripheral implements IPeripheral {}
   ```

 - If your function may return `null`, make sure to annotate it with `@Nullable`! This will be expressed in the
   documentation.

 - Some functions do not include any type information (such as returning `Object[]`, or taking `IArguments` as a
   argument). In these cases, one may use `@cc.treturn` or `@cc.tparam` tags (equivalent to illuaminate/[LDoc]'s) as an
   override.

   Similarly, any other LDoc annotation (such as `@see` or `@usage`) may be used by prefixing them by `@cc.` (i.e.
   `@cc.see`).

[doclet]: https://docs.oracle.com/javase/9/docs/api/jdk/javadoc/doclet/package-summary.html
[illuaminate]: https://squiddev.cc/illuaminate/
[lua_function]: https://github.com/SquidDev-CC/CC-Tweaked/blob/mc-1.15.x/src/main/java/dan200/computercraft/api/lua/LuaFunction.java
[ldoc]: https://stevedonovan.github.io/ldoc/manual/doc.md.html
