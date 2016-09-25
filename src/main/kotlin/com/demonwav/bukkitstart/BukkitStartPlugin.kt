package com.demonwav.bukkitstart

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File
import java.io.FileOutputStream
import java.net.MalformedURLException
import java.net.URL
import java.nio.channels.Channels

@Mojo(name = "setup-server", defaultPhase = LifecyclePhase.PACKAGE)
class BukkitStartPlugin : AbstractMojo() {

    @field:Parameter(required = false)
    private var file: File? = null

    @field:Parameter(required = false)
    private var url: String? = null

    // General settings
    @field:Parameter(defaultValue = "run", required = true)
    private lateinit var runDirectory: File

    @field:Parameter(required = false)
    private var acceptEula: Boolean? = null

    @field:Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject

    override fun execute() {
        setupDir()

        if (url == null && file == null) {
            log.error("Either url or file must be set.")
            return
        }

        if (url != null && file != null) {
            log.warn("Both url and file were set, using url.")
        }

        if (url != null) {
            try {
                val urlInst = when (url) {
                    "LATEST_CRAFTBUKKIT" -> URL("https://ci.demonwav.com/repository/download/Spigot_BuildTools/latest.lastSuccessful/craftbukkit-paperclip-1.10.2.jar")
                    "LATEST_SPIGOT" -> URL("https://ci.demonwav.com/repository/download/Spigot_BuildTools/latest.lastSuccessful/spigot-paperclip-1.10.2.jar")
                    "LATEST_PAPER" -> URL("https://ci.destroystokyo.com/job/PaperSpigot/lastSuccessfulBuild/artifact/paperclip.jar")
                    else -> URL(url)
                }

                updateServer(urlInst)
            } catch (e: MalformedURLException) {
                log.error("url must be a valid url or LATEST_CRAFTBUKKIT, LATEST_SPIGOT, or LATEST_PAPER")
            }
        } else {
            updateServer(file!!.toURI().toURL())
        }
    }

    fun setupDir() {
        if (!runDirectory.exists()) {
            val successful = runDirectory.mkdirs()
            if (!successful) {
                log.error("Could not create run directory at: " + runDirectory.absolutePath)
                return
            }
        }

        val pluginDir = File(runDirectory, "plugins")
        if (!pluginDir.exists()) {
            val successful = pluginDir.mkdirs()
            if (!successful) {
                log.error("Could not create plugins directory at: " + pluginDir.absolutePath)
                return
            }
        }

        val eula = File(runDirectory, "eula.txt")

        if (acceptEula == true && !eula.exists()) {
            eula.writeText("eula=true\n")
        }

        val infoTxt = File(runDirectory, "info.txt")
        infoTxt.writeText(project.artifact.file.absolutePath)
    }

    fun updateServer(url: URL) {
        val serverJar = File(runDirectory, "server.jar")

        if (!serverJar.exists()) {
            val connection = url.openConnection()

            // Dirty, dirty, dirty...
            System.setProperty("http.agent", "")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2")

            connection.inputStream.use { stream ->
                val channel = Channels.newChannel(stream)
                val fos = FileOutputStream(File(runDirectory, "server.jar"))
                fos.use {
                    it.channel.transferFrom(channel, 0, Long.MAX_VALUE)
                }
            }
        }
    }
}
