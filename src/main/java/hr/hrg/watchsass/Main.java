package hr.hrg.watchsass;

import java.io.IOException;
import java.nio.file.Paths;

import wrm.libsass.SassCompiler.InputSyntax;
import wrm.libsass.SassCompiler.OutputStyle;

public class Main {
	
	public static void main(String[] args) throws IOException {
		
		if(args.length == 0){
			printHelp();
			return;
		}
		
		CompilerOptions opts = new CompilerOptions();		
		opts.appRoot = Paths.get("./");
		
		boolean watch = false;
		String arg = null;
		String arg2 = null;
		
		for(int i=0; i<args.length; i++){
			arg = args[i];
			arg2 = args.length > i+1 ? args[i+1]:"";//to avoid ArrayIndexOutOfBounds

			// values
			if("-o".equals(arg) || "--output".equals(arg)){
				opts.pathStrOutput = arg2;i++;
			
			}else if("--output-style".equals(arg)){
				String styleStr = arg2;i++;
				try {
					opts.outputStyle = OutputStyle.valueOf(styleStr);						
				} catch (Exception e) {
					System.out.println("Unsupported output style: "+styleStr); System.exit(1);
				}
			
			}else if("--include-path".equals(arg)){
				opts.pathStrInclude.add(arg2);i++;
			
			}else if("--precision".equals(arg)){
				opts.precision = Integer.parseInt(args[++i]);

			// switches
			}else if("-v".equals(arg) || "--version".equals(arg)){
				System.out.println("Java sass-watch version with libsass_3.2.4");
				System.exit(i);
			
			}else if("-w".equals(arg) || "--watch".equals(arg)){
				watch = true;
			
			}else if("-h".equals(arg) || "--help".equals(arg)){
				printHelp();
				System.exit(0);
			
			}else if("-r".equals(arg) || "--recursive".equals(arg)){
				opts.recursive = true;
			
			}else if("-x".equals(arg) || "--omit-source-map-url".equals(arg)){
				opts.omitSourceMapingURL = true;
			
			}else if("-i".equals(arg) || "--indented-syntax".equals(arg)){
				opts.inputSyntax = InputSyntax.sass;
			
			} if("--source-comments".equals(arg)){
				opts.generateSourceComments = true;
			
			}else if("--source-map".equals(arg)){
				opts.generateSourceMap = true;
			
			}else if("--source-map-contents".equals(arg)){
				opts.embedSourceContentsInSourceMap = true;
			
			}else if("--source-map-embed".equals(arg)){
				opts.embedSourceMapInCSS = true;
			
			}else{
				// if it is not an option, then it is input
				opts.pathStrInput = arg;
			}
			
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
