package hr.hrg.watchsass;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import hr.hrg.javawatcher.FileChangeEntry;
import hr.hrg.javawatcher.FileMatchGlob;
import hr.hrg.javawatcher.FolderWatcher;
import hr.hrg.javawatcher.FolderWatcherOld;
import hr.hrg.javawatcher.IFolderWatcher;
import hr.hrg.javawatcher.Main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bit3.jsass.CompilationException;
import wrm.libsass.SassCompiler;

public class Compiler implements Runnable{

	private CompilerOptions opts;
	private List<Path> inputFiles = new ArrayList<>();
	private Path rootPathOut;
	private Path rootPathInp;
	private Path pathInp;
	private Path rootPathSourceMap;
	private SassCompiler compiler;

	private Path[] pathInc;
	
	IFolderWatcher<MyContext> folderWatcher = Main.makeWatcher();	

	public Compiler(CompilerOptions opts) {
		System.out.println(" FolderWatcher NEW ");
		this.opts = opts;

		pathInp = opts.appRoot.resolve(opts.pathStrInput).toAbsolutePath().normalize();
		
		if (pathInp.toFile().isDirectory()) {
			rootPathInp = pathInp;
			forCompileGlob = new MyFileMatcher(rootPathInp, opts.recursive, true);
			if (opts.pathStrOutput == null)
				rootPathOut = rootPathInp;
			else
				rootPathOut = opts.appRoot.resolve(opts.pathStrOutput);

			forCompileGlob.includes("*.scss");
			if(opts.recursive) forCompileGlob.includes("**/*.scss");
		} else {
			// single SCSS file goes to same folder
			rootPathInp = pathInp.getParent().toAbsolutePath().normalize();

			if (opts.pathStrOutput == null)
				rootPathOut = rootPathInp;
			else
				rootPathOut = opts.appRoot.resolve(opts.pathStrOutput);

			forCompileGlob = new MyFileMatcher(rootPathInp, false, true);
			System.err.println();
			System.err.println(rootPathInp);
			System.err.println(rootPathInp.relativize(pathInp).toString());
			forCompileGlob.includes(rootPathInp.relativize(pathInp).toString());
		}
		// getMatched() does not work without this
		forCompileGlob.setCollectMatched(true);
		forCompileGlob.excludes(".sass-cache","**/.sass-cache");
		
		rootPathSourceMap = rootPathOut;

		// make include paths relative to configured root folder
		pathInc = new Path[opts.pathStrInclude.size()];
		StringBuffer b = new StringBuffer();
		int i = 0;
		for (String inc : opts.pathStrInclude) {
			Path tmpPath = opts.appRoot.resolve(inc);
			if (i > 0)
				b.append(File.pathSeparator);
			b.append(tmpPath);
			pathInc[i] = tmpPath;
			i++;
		}

		compiler = new SassCompiler();
		compiler.setEmbedSourceMapInCSS(opts.embedSourceMapInCSS);
		compiler.setEmbedSourceContentsInSourceMap(opts.embedSourceContentsInSourceMap);
		compiler.setGenerateSourceComments(opts.generateSourceComments);
		compiler.setGenerateSourceMap(opts.generateSourceMap);
		compiler.setIncludePaths(b.toString());
		compiler.setInputSyntax(opts.inputSyntax);
		compiler.setOmitSourceMappingURL(opts.omitSourceMapingURL);
		compiler.setOutputStyle(opts.outputStyle);
		compiler.setPrecision(opts.precision);

	}

	public void start(boolean watch) {

		init(watch);
		
		if(!watch){	
			// just compile
			// caller must make a new thread that will call run, in case watch=true
			compile();
		}
	}

	public void init(boolean watch) {
		try {
			
			folderWatcher.add(forCompileGlob);
			
			// add includes to watch list
			if (watch && opts.pathStrInclude != null){
				for (Path p : pathInc) {
					File file = p.toFile();
					
					if (file.exists() && file.isDirectory()) {
						MyFileMatcher matcher = new MyFileMatcher(p, true, false);
						folderWatcher.add(matcher);
						if(opts.pathStrExclude != null) matcher.excludes(opts.pathStrExclude);
					} else if (hr.hrg.javawatcher.Main.isWarnEnabled()) {
						if (!file.exists()) {
							hr.hrg.javawatcher.Main.logWarn("Include folder does not exist: " + p);
						} else {
							hr.hrg.javawatcher.Main.logWarn("Include folder is a file: " + p);
						}
					}
				}
			}
			
			
			// when folder watcher is started, all matchers that were added to it are 
			// filled with files found that are (included + not excluded)
			folderWatcher.init(watch);

			// so to get list of files to compile, we just check the glob that we use for compile
			inputFiles.addAll(forCompileGlob.getMatched());

			// log what we are doing
			if (hr.hrg.javawatcher.Main.isInfoEnabled()) {
				hr.hrg.javawatcher.Main.logInfo("Input  Path= " + rootPathInp);
				hr.hrg.javawatcher.Main.logInfo("Output Path= " + rootPathOut);
				hr.hrg.javawatcher.Main.logInfo(forCompileGlob.getMatched().size()+" files to compile ");
				for (Path path : inputFiles) {
					hr.hrg.javawatcher.Main.logInfo("Will watch and compile: "+path);
				}
			}
			

		} catch (Exception e) {
			hr.hrg.javawatcher.Main.logError(e.getMessage(),e);
		}
	}

	public void compile() {
		for (Path p : inputFiles) {
			processFile(p, false);
		}
	}
	
	public void run(){
		// single thread version that uses FolderWatcher.poll(timeout) to wait for more burst changes
		// before processing the changed files

		HashSet<Path> forUpdate = new HashSet<>();
		boolean initial = true;
		// main watch loop, that checks for files
		Collection<FileChangeEntry<MyContext>> changed = null;
		try {
			while(!Thread.interrupted()){
				
				changed = folderWatcher.takeBatch(opts.burstDelay);
				if(changed == null) {
					initial = false;
					break; // interrupted
					
				}
				
				for (FileChangeEntry<MyContext> fileChangeEntry: changed) {
					if(fileChangeEntry.getMatcher().getContext().isForCompile())
						forUpdate.add(forCompileGlob.relativize(fileChangeEntry.getPath()));
					else{
						if(hr.hrg.javawatcher.Main.isInfoEnabled())
							hr.hrg.javawatcher.Main.logInfo("Include changed, adding all input SCSS to recompile queue ("+fileChangeEntry.getPath()+")");
						forUpdate.addAll(inputFiles);
					}
				}
				
				for (Path p : forUpdate){
					try {
						processFile(p, initial);
					} catch (Exception e) {
						hr.hrg.javawatcher.Main.logError("error processing path "+p.toAbsolutePath(),e);
					}
				}				
				initial = false;
				forUpdate.clear();
			}		
		} finally {
			folderWatcher.close();
		}
	}
	
	private MyFileMatcher forCompileGlob;

	private final boolean processFile(Path inputFilePath, boolean initial) {
		inputFilePath = forCompileGlob.getRootPathAbs().resolve(inputFilePath);
		Path relativeInputPath = rootPathInp.relativize(inputFilePath);
		Path outputFilePath = rootPathOut.resolve(relativeInputPath);

		if(initial) {
			long modIn  = inputFilePath .toFile().lastModified();
			long modOut = outputFilePath.toFile().lastModified();
//			if(modIn > modOut) {
//				//if(VERBOSE > 1)
//				hr.hrg.javawatcher.Main.logInfo("skip older: " + inputFilePath);
//				return false;
//			}
		}
		hr.hrg.javawatcher.Main.logInfo("rebuild: " + inputFilePath);


		outputFilePath = Paths.get(outputFilePath.toAbsolutePath().toString().replaceFirst("\\.scss$", ".css"));

		Path sourceMapOutputPath = rootPathSourceMap.resolve(relativeInputPath);
		sourceMapOutputPath = Paths
				.get(sourceMapOutputPath.toAbsolutePath().toString().replaceFirst("\\.scss$", ".css.map"));

		io.bit3.jsass.Output out;
		try {
			out = compiler.compileFile(inputFilePath.toAbsolutePath().toString(),
					outputFilePath.toAbsolutePath().toString(), sourceMapOutputPath.toAbsolutePath().toString());
		} catch (CompilationException e) {
			hr.hrg.javawatcher.Main.logError(e.getMessage(),e);
			return false;
		}

		if(hr.hrg.javawatcher.Main.isInfoEnabled()) hr.hrg.javawatcher.Main.logInfo("Compilation finished.");

		writeContentToFile(outputFilePath, out.getCss());
		String sourceMap = out.getSourceMap();
		if (sourceMap != null) {
			writeContentToFile(sourceMapOutputPath, sourceMap);
		}
		return true;
	}

	private static final void writeContentToFile(Path outputFilePath, String content) {
		File f = outputFilePath.toFile();
		f.getParentFile().mkdirs();
		FileOutputStream fos = null;
		try {
			f.createNewFile();
			fos = new FileOutputStream(f);
			fos.write(content.getBytes()); // FIXME: potential problem here:
											// What is the expected encoding of
											// the output?
			fos.flush();
		} catch (Exception e) {
			throw new RuntimeException("Error writing file " + outputFilePath, e);
		} finally {
			try {
				if (fos != null)
					fos.close();
			} catch (IOException e) {
				throw new RuntimeException("Error writing file " + outputFilePath, e);
			}
		}

		if (hr.hrg.javawatcher.Main.isInfoEnabled())
			hr.hrg.javawatcher.Main.logInfo("Written to: " + outputFilePath);
	}

	class MyContext{
		private boolean forCompile;
		public MyContext(boolean forCompile) {
			this.forCompile = forCompile;
		}
		public boolean isForCompile() {
			return forCompile;
		}
	}
	
	class MyFileMatcher extends FileMatchGlob<MyContext>{
		public MyFileMatcher(Path root, boolean recursive, boolean forCompile) {
			super(root, new MyContext(forCompile), recursive);
		}
		
		public boolean isForCompile() {
			return getContext().isForCompile();
		}
		
		@Override
		public String toString() {
			return (isForCompile() ? "compile":"include")+":"+rootString;
		}
	}
}
