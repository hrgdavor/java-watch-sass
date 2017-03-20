# Java watch sass [![Maven Central](https://img.shields.io/maven-central/v/hr.hrg/java-watch-sass.svg)](https://mvnrepository.com/artifact/hr.hrg/java-watch-sass)

## The utility
Command line utility for compiling scss files using Java with libsass binding (Linux, windows).

Intended to work as replacement for command-line interface of [node-sass](https://github.com/sass/node-sass#command-line-interface) (a nodejs module i used for sometime now). 

Reason I made this is to learn how to use
[libsass-maven](https://github.com/warmuuh/libsass-maven-plugin) programatically, so I can
integrate scss compilation into some of my own Java based build scripts without requiring user to 
download nodejs or Ruby.

If you need a command-line tool to build and watch scss
 - if you like/have Java - you can use this tool
 - if you like/have Ruby - it is the home of the original `sass` tool ([instructions](http://sass-lang.com/install))
 - if you like/have nodejs - [node-sass](https://github.com/sass/node-sass#command-line-interface) 
 - or try to find compiled binary of: [sassc](https://github.com/sass/sassc)

You can check the code here to get insight on how to build scss from Java program directly.


## Comamnd line:

Download the single runnable jar with all the dependencies from the  [releases](https://github.com/hrgdavor/java-watch-sass/releases).

```bash
usage:

java -jar java-watch-sass-{$version}.jar [options]

    -w, --watch                Watch a directory or file
    -r, --recursive            Recursively watch directories or files
    -o, --output               Output directory
    -x, --omit-source-map-url  Omit source map URL comment from output
    -i, --indented-syntax      Treat sources as sass code (versus scss)
    -v, --version              Prints version info
    --output-style             CSS output style (nested | expanded | compact | compressed)
    --source-comments          Include debug info in output
    --source-map               Emit source map
    --source-map-contents      Embed include contents in map
    --source-map-embed         Embed sourceMappingUrl as data URI
    --source-map-root          Base path, will be emitted in source-map as is
    --include-path             Path to look for imported files
    --follow                   Follow symlinked directories
    --precision                The amount of precision allowed in decimal numbers
    --help                     Print usage info
```


## License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).
