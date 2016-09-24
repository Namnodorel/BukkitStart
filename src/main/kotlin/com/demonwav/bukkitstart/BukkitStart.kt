package com.demonwav.bukkitstart

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File
import java.net.URL

@Mojo(name = "setup-server", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
class BukkitStart : AbstractMojo() {

    @field:Parameter(defaultValue = "PAPERCLIP", required = true)
    private lateinit var method: Method

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
        // TODO stuff
    }
}
