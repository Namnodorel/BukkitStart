package com.demonwav.bukkitstart

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels

@Mojo(name = "setup-server", defaultPhase = LifecyclePhase.PACKAGE)
class BukkitStartPlugin : AbstractMojo() {

    @field:Parameter(defaultValue = "PAPERCLIP", required = true)
    private lateinit var method: StartMethod

    // Paperclip settings
    @field:Parameter(required = false)
    private var serverType: ServerType? = null

    @field:Parameter(required = false)
    private var mcVersion: String? = null

    // Direct settings
    @field:Parameter(required = false)
    private var url: URL? = null

    // General settings
    @field:Parameter(defaultValue = "run", required = true)
    private lateinit var runDirectory: File

    @field:Parameter(required = false)
    private var acceptEula: Boolean? = null

    // Maven thing
    @field:Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject

    override fun execute() {
        if (method == StartMethod.DIRECT) {
            TODO()
        }

        if (url != null) {
            TODO()
        }

        if (serverType != ServerType.PAPER) {
            TODO()
        }

        if (mcVersion != "1.10.2") {
            TODO()
        }

        if (!runDirectory.exists()) {
            val successful = runDirectory.mkdirs()
            if (!successful) {
                TODO()
            }
        }

        val pluginDir = File(runDirectory, "plugins")
        if (!pluginDir.exists()) {
            val successful = pluginDir.mkdirs()
            if (!successful) {
                TODO()
            }
        }

        val eula = File(runDirectory, "eula.txt")

        if (acceptEula == true && !eula.exists()) {
            eula.writeText("eula=true\n")
        }

        val serverJar = File(runDirectory, "server.jar")
        updatePaperclip(serverJar)
    }

    fun updatePaperclip(serverJar: File) {
        if (!serverJar.exists()) {
            val downloadUrl = URL("https://ci.destroystokyo.com/job/PaperSpigot/lastSuccessfulBuild/artifact/paperclip.jar")
            val connection = downloadUrl.openConnection()

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
