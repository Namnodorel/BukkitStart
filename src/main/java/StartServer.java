import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

public class StartServer {
    public static void main(String[] args)
            throws IOException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException, InvocationTargetException {
        final File infoTxt = new File("info.txt");
        if (!infoTxt.exists()) {
            System.err.println("info.txt must exist! Have you run your Maven build yet?");
            return;
        }

        final File artifactJar = new File(new String(Files.readAllBytes(infoTxt.toPath()), StandardCharsets.UTF_8));
        if (!artifactJar.exists()) {
            System.err.println("Artifact Jar must exist.");
            return;
        }

        final File runDirectory = new File(".");
        if (!runDirectory.exists()) {
            if (!runDirectory.mkdirs()) {
                System.err.println("Run Directory does not exist and could not be created.");
                return;
            }
        }

        final File serverJar = new File(runDirectory, "server.jar");
        if (!serverJar.exists()) {
            System.err.println("server.jar must exist in Run Directory.");
            return;
        }

        final File pluginDir = new File(runDirectory, "plugins");
        if (!pluginDir.exists()) {
            if (!pluginDir.mkdirs()) {
                System.err.println("plugins/ directory does not exist in the Run Directory and could not be created.");
                return;
            }
        }

        runServer(pluginDir, artifactJar, serverJar);
    }

    @SuppressWarnings("unchecked")
    private static void runServer(final File pluginDir, final File artifactJar, final File serverJar)
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException, IOException {

        final File plugin = new File(pluginDir, "plugin.jar");

        //noinspection ResultOfMethodCallIgnored
        plugin.delete();
        Files.copy(artifactJar.toPath(), plugin.toPath());

        final String main;
        try (
            final FileInputStream fs = new FileInputStream(serverJar);
            final JarInputStream js = new JarInputStream(fs)
        ) {
            main = js.getManifest().getMainAttributes().getValue("Main-Class");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error reading from jar");
            return;
        }

        // Run the jar
        final URL url;
        try {
            url = serverJar.toURI().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.err.println("Error reading path to jar");
            return;
        }

        final ClassLoader loader = ClassLoader.getSystemClassLoader();
        if (!(loader instanceof URLClassLoader)) {
            System.err.println("SystemClassLoader not URLClassLoader");
            return;
        }


        // Remove the current module from the classpath
        final Field ucpField = URLClassLoader.class.getDeclaredField("ucp");
        ucpField.setAccessible(true);
        final Object ucp = ucpField.get(loader);

        final Field urlsField = ucp.getClass().getDeclaredField("urls");
        final Field pathField = ucp.getClass().getDeclaredField("path");
        final Field lmapField = ucp.getClass().getDeclaredField("lmap");
        final Field loadersField = ucp.getClass().getDeclaredField("loaders");

        urlsField.setAccessible(true);
        pathField.setAccessible(true);
        lmapField.setAccessible(true);
        loadersField.setAccessible(true);

        final Stack<URL> urls = (Stack<URL>) urlsField.get(ucp);
        final ArrayList<URL> path = (ArrayList<URL>) pathField.get(ucp);
        final HashMap<String, Closeable> lmap = (HashMap<String, Closeable>) lmapField.get(ucp);
        final ArrayList<Closeable> loaders = (ArrayList<Closeable>) loadersField.get(ucp);

        System.out.println("init:");
        System.out.println(urls);
        System.out.println(path);
        System.out.println(lmap);
        System.out.println(loaders);

        // Remove this module's classpath from the list of paths
        // Also remove maven dependencies
        // Oh, and also remove myself from the classpath.....
        final List<URL> pathUrl = path.stream()
            .filter(p -> p.toString().contains("target/classes") || p.toString().contains("bukkitstart") || p.toString().contains(".m2"))
            .collect(Collectors.toList());

        System.out.println("pathUrl" + pathUrl);

        if (!pathUrl.isEmpty()) {
            path.removeAll(pathUrl);
            urls.clear();

            for (URL p: path) {
                urls.add(0, p);
            }

            // Remove the necessary loaders
            // This is the magic. The module classes are already loaded at this point
            // Clearing this will force the classloader to refer back to the URL list to find the class
            // Since we removed this module's URL from the URL list, it won't be able to find the conflicting classes
            // and Bukkit will be able to load the classes in it's own classloader
            for (URL p: pathUrl) {
                final Closeable o = lmap.remove("file://" + p.getFile());
                if (o != null) {
                    System.out.println("remove: " + p + " " + o);
                    loaders.remove(o);
                    o.close();
                }
            }
        }

        System.out.println("after remove:");
        System.out.println(urls);
        System.out.println(path);
        System.out.println(lmap);
        System.out.println(loaders);

        // Add the url to the current system classloader
        final Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addUrl.setAccessible(true);
        addUrl.invoke(loader, url);

        // We need to create the loader for this url
        final Method getLoader = ucp.getClass().getDeclaredMethod("getLoader", URL.class);
        getLoader.setAccessible(true);
        final Closeable newLoader = (Closeable) getLoader.invoke(ucp, url);

        // Add to loaders and lmap
        loaders.add(newLoader);
        lmap.put("file://" + url.getFile(), newLoader);

        // re-enable lookup cache (the addURL will disable it)
        final Field lookupCacheEnabledField = ucp.getClass().getDeclaredField("lookupCacheEnabled");
        lookupCacheEnabledField.setAccessible(true);
        lookupCacheEnabledField.set(null, true);

        // We want it to re-populate the lookup cache...
        final Field lookupCacheURLsField = ucp.getClass().getDeclaredField("lookupCacheURLs");
        lookupCacheURLsField.setAccessible(true);
        lookupCacheURLsField.set(ucp, null);

        final Field lookupCacheLoaderField = ucp.getClass().getDeclaredField("lookupCacheLoader");
        lookupCacheLoaderField.setAccessible(true);
        lookupCacheLoaderField.set(ucp, null);

        System.out.println("after add:");
        System.out.println(urls);
        System.out.println(path);
        System.out.println(lmap);
        System.out.println(loaders);

        try {
            final Class<?> cls = Class.forName(main, true, loader);
            final Method m = cls.getMethod("main", String[].class);

            m.invoke(null, (Object) new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error running jar");
        }
    }
}
