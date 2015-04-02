package org.jetbrains.kotlin.cli.common.modules

import java.util.ArrayList

public class SourcesBuilder(private val parent: ModuleBuilder) {
    public fun plusAssign(pattern: String) {
        parent.addSourceFiles(pattern)
    }
}

public class ClasspathBuilder(private val parent: ModuleBuilder) {
    public fun plusAssign(name: String) {
        parent.addClasspathEntry(name)
    }
}

public class AnnotationsPathBuilder(private val parent: ModuleBuilder) {
    public fun plusAssign(name: String) {
        parent.addAnnotationsPathEntry(name)
    }
}

public class ModuleBuilder(private val name: String, private val outputDir: String) : Module {
    // http://youtrack.jetbrains.net/issue/KT-904
    private val sourceFiles = ArrayList<String>()

    private val classpathRoots = ArrayList<String>()
    private val javaSourceRoots = ArrayList<String>()
    private val annotationsRoots = ArrayList<String>()

    public fun addSourceFiles(pattern: String) {
        sourceFiles.add(pattern)
    }

    public fun addClasspathEntry(name: String) {
        classpathRoots.add(name)
    }

    public fun addAnnotationsPathEntry(name: String) {
        annotationsRoots.add(name)
    }

    public fun addJavaSourceRoot(name: String) {
        javaSourceRoots.add(name)
    }

    public override fun getOutputDirectory(): String = outputDir
    public override fun getJavaSourceRoots(): List<String> = javaSourceRoots
    public override fun getSourceFiles(): List<String> = sourceFiles
    public override fun getClasspathRoots(): List<String> = classpathRoots
    public override fun getAnnotationsRoots(): List<String> = annotationsRoots
    public override fun getModuleName(): String = name
}

