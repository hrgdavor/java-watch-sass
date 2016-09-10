#The utility
Command line utility for compiling scss files using Java with libsass binding (Linux, windows).

Intended to work as command-line interface of a nodejs module called [node-sass](https://github.com/sass/node-sass#command-line-interface). This was built to learn how to use
[libsass-maven](https://github.com/warmuuh/libsass-maven-plugin) programatically, so I can
integrate scss compilation into my own Java based build process without requiring user to 
download nodejs or Ruby.

If you need a command-line tool to build scss
 - you like/have java already - you can use this tool
 - you like/have Ruby - it is the home of the original `sass` tool ([instructions](http://sass-lang.com/install))
 - you like/have nodejs - [node-sass](https://github.com/sass/node-sass#command-line-interface) 
 - try to find compiled binary of: [sassc](https://github.com/sass/sassc)

You can check the code here to get insight on how to build scss from Java program directly.


## Comamnd line options:

```bash
    -w, --watch                Watch a directory or file
    -r, --recursive            Recursively watch directories or files
    -o, --output               Output directory
    -x, --omit-source-map-url  Omit source map URL comment from output
    -i, --indented-syntax      Treat data from stdin as sass code (versus scss)
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
    --error-bell               Output a bell character on errors
    --importer                 Path to .js file containing custom importer
    --functions                Path to .js file containing custom functions
    --help                     Print usage info
```


## License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).
