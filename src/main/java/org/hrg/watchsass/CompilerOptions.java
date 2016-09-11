package org.hrg.watchsass;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import wrm.libsass.SassCompiler.InputSyntax;
import wrm.libsass.SassCompiler.OutputStyle;

public class CompilerOptions {

	// sass compiler options
	public boolean embedSourceMapInCSS = false;
	public boolean embedSourceContentsInSourceMap = false;
	public boolean generateSourceComments = false;
	public boolean generateSourceMap = false;
	public InputSyntax inputSyntax = InputSyntax.scss;
	public boolean omitSourceMapingURL = false;
	public OutputStyle outputStyle = OutputStyle.expanded;
	public int precision = 5;

	// our compile options
	public Path appRoot = Paths.get("./");
	public String pathStrInput = "./";
	public String pathStrOutput = "./";
	public List<String> pathStrInclude = new ArrayList<>();
	public boolean recursive;
	public long updateDelay = 20;
}
