BukkitStartPlugin
===========

Inspired by [SpongeStart](https://github.com/Qixalite/SpongeStart)

Maven plugin to run CraftBukkit, Spigot, or Paper inside your workspace.

License
-------

[MIT](license.txt)

Usage
-----

Using BukkitStart is quite easy.

There are 2 steps:

 1. Setting up the `pom.xml`
 2. Setting up the run config

Setting up the `pom.xml` requires four parts:

 1. The repository. This is how Maven finds the start dependency to use:
 
 ```xml
 <repositories>
     <repository>
         <id>demonwav</id>
         <url>https://nexus.demonwav.com/content/repositories/snapshots/</url>
     </repository>
 </repositories>
 ```
 2. The plugin repository. This is how Maven finds the Maven plugin. They are the same as above, but Maven doesn't know that.
 
 ```xml
 <pluginRepositories>
     <pluginRepository>
         <id>demonwav</id>
         <url>https://nexus.demonwav.com/content/repositories/snapshots/</url>
     </pluginRepository>
 </pluginRepositories>
 ```
 
 3. The dependency. This is how we get the `StartServer` class which we use in the run config.
 
 ```xml
 <dependencies>
     <dependency>
         <groupId>com.demonwav</groupId>
         <artifactId>bukkitstart</artifactId>
         <version>1.0-SNAPSHOT</version>
         <scope>runtime</scope>
     </dependency>
 </dependencies>
 ```
 4. The plugin. This is how we get the run directory setup in the `Maven` build process.
 
 ```xml
 <plugin>
     <groupId>com.demonwav</groupId>
     <artifactId>bukkitstart</artifactId>
     <version>1.0-SNAPSHOT</version>
     <configuration>
         <acceptEula>true</acceptEula>
         <url>LATEST_SPIGOT</url>
     </configuration>
     <executions>
         <execution>
             <goals>
                 <goal>setup-server</goal>
             </goals>
         </execution>
     </executions>
 </plugin>
 ```
 The `url` tag here can be any url you want that points to a runnable jar file. There are also 3 convenience values you can use for the
 latest version of CraftBukkit, Spigot, and Paper under the [Paperclip](https://aquifermc.org/threads/paperclip.4/) distribution method:
 
  * `LATEST_CRAFTBUKKIT`
  * `LATEST_SPIGOT`
  * `LATEST_PAPER`
  
 If you want to test using other versions, run [BuildTools](https://www.spigotmc.org/wiki/buildtools/) to get the jar you want, and define
 it by replacing the `url` tag with a `file` tag, pointing to the location of the jar.

An example project is available to look at [here](https://github.com/DemonWav/BukkitStart/tree/master/example) with a full `pom.xml` file.

Once you have your `pom.xml` setup, run `mvn package` to setup your run directory. Now for step two, we need to setup the run config.

The run config should be a normal "Java Application" run config, with the following parameters:

 1. Main class: `StartServer`
 2. Working directory: The working directory you either defined in the `pom.xml`, or was just the default `run` directory.
 3. (Optional) For optimal convenience, configure the `mvn package` run config to run before launch.

You should now be able to easily run, debug, and hot swap code with your plugin directly from your IDE.
