@file:JvmName("StartServer")
package com.demonwav.bukkitstart

import java.io.File
import java.io.FileInputStream
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.ArrayList
import java.util.HashMap
import java.util.Stack
import java.util.jar.JarInputStream

fun main(args: Array<String> ) {
    val infoTxt = File("info.txt")
    if (!infoTxt.exists()) {
        error("info.txt must exist! Have you run your Maven build yet?")
    }

    val artifactJar = File(infoTxt.readText())
    if (!artifactJar.exists()) {
        error("Artifact Jar must exist.")
    }

    val runDirectory = File(".")
    if (!runDirectory.exists()) {
        error("Run Directory must exist.")
    }

    val serverJar = File(runDirectory, "server.jar")
    if (!serverJar.exists()) {
        error("server.jar must exist in Run Directory.")
    }

    val pluginDir = File(runDirectory, "plugins")
    if (!pluginDir.exists()) {
        error("plugins/ directory must exist in Run Directory.")
    }

    runServer(artifactJar, serverJar, pluginDir)
}

@Suppress("UNCHECKED_CAST")
fun runServer(artifactJar: File, serverJar: File, pluginDir: File) {
    val plugin = File(pluginDir, "plugin.jar")

    plugin.delete()
    Files.copy(artifactJar.toPath(), plugin.toPath())

    // Get main class info from jar
    val main = FileInputStream(serverJar).use { fs ->
        JarInputStream(fs).use { js ->
            js.manifest.mainAttributes.getValue("Main-Class")
        }
    } ?: error("Error reading from jar")

    // Run the jar
    val url = try {
        serverJar.toURI().toURL()
    } catch (e: MalformedURLException) {
        e.printStackTrace()
        error("Error reading path to jar")
    }

    val loader = ClassLoader.getSystemClassLoader() as? URLClassLoader ?: error("SystemClassLoader not URLClassLoader")

    // Add the url to the current system classloader
    val addUrl = URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java)
    addUrl.isAccessible = true
    addUrl.invoke(loader, url)


    // Remove the current module from the classpath
    val ucpField = URLClassLoader::class.java.getDeclaredField("ucp")
    ucpField.isAccessible = true
    val ucp = ucpField.get(loader)

    val urlsField = ucp.javaClass.getDeclaredField("urls")
    val pathField = ucp.javaClass.getDeclaredField("path")
    val lmapField = ucp.javaClass.getDeclaredField("lmap")
    val loadersField = ucp.javaClass.getDeclaredField("loaders")

    urlsField.isAccessible = true
    pathField.isAccessible = true
    lmapField.isAccessible = true
    loadersField.isAccessible = true

    val urls = urlsField.get(ucp) as Stack<URL>
    val path = pathField.get(ucp) as ArrayList<URL>
    val lmap = lmapField.get(ucp) as HashMap<String, Any>
    val loaders = loadersField.get(ucp) as ArrayList<Any>

    // Remove this module's classpath from the list of paths
    // Oh, and also remove myself from the classpath.....
    val pathUrl = ArrayList<URL>()
    for (p in path) {
        if (p.toString().contains("target/classes") || p.toString().contains("bukkitstart")) {
            pathUrl.add(p)
        }
    }

    if (!pathUrl.isEmpty()) {
        path.removeAll(pathUrl)
        urls.empty()

        for (p in path) {
            urls.add(0, p)
        }

        for (p in pathUrl) {
            val any = lmap.remove("file://${p.file}")
            // Clear the loaders
            // This is the magic. The module classes are already loaded at this point
            // Clearing this will force the classloader to refer back to the URL list to find the class
            // Since we removed this module's URL from the URL list, it won't be able to find the conflicting classes
            // and Bukkit will be able to load the classes in it's own classloader
            loaders.remove(any)
        }
    }

    // re-enable lookup cache (the addURL will disable it)
    val lookupCacheEnabledField = ucp.javaClass.getDeclaredField("lookupCacheEnabled")
    lookupCacheEnabledField.isAccessible = true
    lookupCacheEnabledField.set(null, true)

    // We want it to re-populate the lookup cache...
    val lookupCacheURLsField = ucp.javaClass.getDeclaredField("lookupCacheURLs")
    lookupCacheURLsField.isAccessible = true
    lookupCacheURLsField.set(ucp, null)

    val lookupCacheLoaderField = ucp.javaClass.getDeclaredField("lookupCacheLoader")
    lookupCacheLoaderField.isAccessible = true
    lookupCacheLoaderField.set(ucp, null)


    try {
        val cls = Class.forName(main, true, loader)
        val m = cls.getMethod("main", Array<String>::class.java)

        m.invoke(null, arrayOf<String>())
    } catch (e: Exception) {
        e.printStackTrace()
        error("Error running jar")
    }
}
