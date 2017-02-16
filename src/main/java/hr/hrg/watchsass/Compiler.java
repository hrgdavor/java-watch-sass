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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bit3.jsass.CompilationException;
import wrm.libsass.SassCompiler;

public class Compiler implements Runnable{

	static final Logger log = LoggerFactory.getLogger(Compiler.class);

	private CompilerOptions opts;
	private List<Path> inputFiles = new ArrayList<>();
	private Path rootPathOut;
	private Path rootPathInp;
	private Path pathInp;
	private Path rootPathSourceMap;
	private SassCompiler compiler;

	private Path[] pathInc;
	
	FolderWatcher<MyFileMatcher> folderWatcher = new FolderWatcher<MyFileMatcher>();	

	public Compiler(CompilerOptions opts) {
		this.opts = opts;

		pathInp = opts.appRoot.resolve(opts.pathStrInput);
		
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
			rootPathOut = rootPathInp = pathInp.getParent();
			forCompileGlob = new MyFileMatcher(rootPathInp, false, true);
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

	private void init(boolean watch) {
		try {
			
			folderWatcher.add(forCompileGlob);
			
			// add includes to watch list
			if (watch && opts.pathStrInclude != null){
				for (Path p : pathInc) {
					File file = p.toFile();
					
					if (file.exists() && file.isDirectory()) {
						folderWatcher.add(new MyFileMatcher(p, true, false));
					} else if (log.isWarnEnabled()) {
						if (!file.exists()) {
							log.warn("Include folder does not exist: " + p);
						} else {
							log.warn("Include folder is a file: " + p);
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
			if (log.isDebugEnabled()) {
				log.debug("Input  Path= " + rootPathInp);
				log.debug("Output Path= " + rootPathOut);
				log.debug("{} files to compile ",forCompileGlob.getMatched().size());
				for (Path path : inputFiles) {
					log.debug("Will watch and compile: {}",path);
				}
			}
			

		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
	}


	protected void compile() {
		for (Path p : inputFiles) {
			processFile(p);
		}
	}
	
	public void run(){
		// single thread version that uses FolderWatcher.poll(timeout) to wait for more burst changes
		// before processing the changed files

		HashSet<Path> forUpdate = new HashSet<>();

		// main watch loop, that checks for files
		Collection<FileChangeEntry<MyFileMatcher>> changed = null;
		while(!Thread.interrupted()){

			if(changed != null && changed.size() > 0){
				// there are some files changed, so we process them and prepare for processing
				for (FileChangeEntry<MyFileMatcher> fileChangeEntry : changed) {
					if(fileChangeEntry.getMatcher().isForCompile())
						forUpdate.add(fileChangeEntry.getPath());
					else{
						log.trace("Include changed, adding all input SCSS to recompile queue ({})",fileChangeEntry.getPath());
						forUpdate.addAll(inputFiles);
					}
				}
				
			}else if(forUpdate.size() >0){
				// there are some updates and
				// there are no more new changes, it is ok now to process whatever we gathered so far
				for (Path p : forUpdate){
					try {
						processFile(p);						
					} catch (Exception e) {
						log.error("error processing path "+p.toAbsolutePath(),e);
					}
				}				
				forUpdate.clear();
			}			
			
			// if we received some changes, poll again with opts.updateDelay until no new
			// changes arrive in that short period
			changed = folderWatcher.poll(changed == null ? 500:opts.updateDelay, TimeUnit.MILLISECONDS);
		}
	}
	
	private MyFileMatcher forCompileGlob;

	private final boolean processFile(Path inputFilePath) {

		if(log.isDebugEnabled()) log.debug("rebuild: " + inputFilePath);

		Path relativeInputPath = rootPathInp.relativize(inputFilePath);

		Path outputFilePath = rootPathOut.resolve(relativeInputPath);
		outputFilePath = Paths.get(outputFilePath.toAbsolutePath().toString().replaceFirst("\\.scss$", ".css"));

		Path sourceMapOutputPath = rootPathSourceMap.resolve(relativeInputPath);
		sourceMapOutputPath = Paths
				.get(sourceMapOutputPath.toAbsolutePath().toString().replaceFirst("\\.scss$", ".css.map"));

		io.bit3.jsass.Output out;
		try {
			out = compiler.compileFile(inputFilePath.toAbsolutePath().toString(),
					outputFilePath.toAbsolutePath().toString(), sourceMapOutputPath.toAbsolutePath().toString());
		} catch (CompilationException e) {
			log.error(e.getMessage(),e);
			return false;
		}

		if(log.isDebugEnabled()) log.debug("Compilation finished.");

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

		if (log.isTraceEnabled())
			log.trace("Written to: " + outputFilePath);
	}

	class MyFileMatcher extends FileMatchGlob{
		private boolean forCompile;

		public MyFileMatcher(Path root, boolean recursive, boolean forCompile) {
			super(root, recursive);
			this.forCompile = forCompile;
		}
		
		public boolean isForCompile() {
			return forCompile;
		}
		
		@Override
		public String toString() {
			return (forCompile ? "compile":"include")+":"+rootString;
		}
	}
}
