package com.demonwav.bukkitstart

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File

@Mojo(name = "setup-server", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
class BukkitStart : AbstractMojo() {

    @field:Parameter(defaultValue = "run", required = false)
    private lateinit var runDirectory: File

    @field:Parameter(required = true)
    private lateinit var server: ServerType

    @field:Parameter(required = true)
    private var acceptEula: Boolean? = null

    @field:Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject

    override fun execute() {
        // TODO stuff
    }
}
