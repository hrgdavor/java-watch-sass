package org.hrg.sasswatch;

import java.io.IOException;
import java.nio.file.Paths;

import wrm.libsass.SassCompiler.InputSyntax;
import wrm.libsass.SassCompiler.OutputStyle;

public class Main {
	
	public static void main(String[] args) throws IOException {
		
		CompilerOptions opts = new CompilerOptions();		
		opts.appRoot = Paths.get("./");
		
		boolean watch = false;
		boolean isOption;
		
		for(int i=0; i<args.length; i++){
			
			isOption = true;

			// option with value
			if(args.length > i+1){

				if("-o".equals(args[i]) || "--output".equals(args[i])){
					opts.pathStrOutput = args[++i];
				}else if("--output-style".equals(args[i])){
					String styleStr = args[++i];
					try {
						opts.outputStyle = OutputStyle.valueOf(styleStr);						
					} catch (Exception e) {
						System.out.println("Unsupported output style: "+styleStr); System.exit(1);
					}
				}else if("-o".equals(args[i]) || "--output".equals(args[i])){
					opts.pathStrOutput = args[++i];
				}else if("--include-path".equals(args[i])){
					opts.pathStrInclude.add(args[++i]);
				}else if("--precision".equals(args[i])){
					opts.precision = Integer.parseInt(args[++i]);
				}else{
					isOption = false;
				}

			}else{
				isOption = false;
			}
			
			// check if it is a switch
			if(!isOption){
				isOption = true;

				// switches
				if("-v".equals(args[i]) || "--version".equals(args[i])){
					System.out.println("Java sass-watch version with libsass_3.2.4");
					System.exit(i);
				}else if("-w".equals(args[i]) || "--watch".equals(args[i])){
					watch = true;
				}else if("-h".equals(args[i]) || "--help".equals(args[i])){
					printHelp();
					System.exit(0);
				}else if("-r".equals(args[i]) || "--recursive".equals(args[i])){
					opts.recursive = true;
				}else if("-x".equals(args[i]) || "--omit-source-map-url".equals(args[i])){
					opts.omitSourceMapingURL = true;
				}else if("-i".equals(args[i]) || "--indented-syntax".equals(args[i])){
					opts.inputSyntax = InputSyntax.sass;
				} if("--source-comments".equals(args[i])){
					opts.generateSourceComments = true;
				}else if("--source-map".equals(args[i])){
					opts.generateSourceMap = true;
				}else if("--source-map-contents".equals(args[i])){
					opts.embedSourceContentsInSourceMap = true;
				}else if("--source-map-embed".equals(args[i])){
					opts.embedSourceMapInCSS = true;
				}else{
					isOption = false;
				}
			}
			
			if(!isOption) opts.pathStrInput = args[i];

		}
		
		Compiler compiler = new Compiler(opts);
		
		compiler.start(watch);
	}

	static void printHelp(){
		System.out.println("    -w, --watch                Watch a directory or file");		
		System.out.println("    -r, --recursive            Recursively watch directories or files");
		System.out.println("    -o, --output               Output directory");
		System.out.println("    -x, --omit-source-map-url  Omit source map URL comment from output");
		System.out.println("    -i, --indented-syntax      Treat data from stdin as sass code (versus scss)");
		System.out.println("    -v, --version              Prints version info");
		System.out.println("    --output-style             CSS output style (nested | expanded | compact | compressed)");
		System.out.println("    --source-comments          Include debug info in output");
		System.out.println("    --source-map               Emit source map");
		System.out.println("    --source-map-contents      Embed include contents in map");
		System.out.println("    --source-map-embed         Embed sourceMappingUrl as data URI");
		System.out.println("    --source-map-root          Base path, will be emitted in source-map as is");
		System.out.println("    --include-path             Path to look for imported files");
		System.out.println("    --precision                The amount of precision allowed in decimal numbers");
		System.out.println("    -h, --help                 Print usage info");
		System.out.println("");
				
	}
	
	public Main(){

	}
	

   
}
