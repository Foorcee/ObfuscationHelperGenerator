package de.foorcee.mappings;

import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.jar.JarInputStream;

@Slf4j(topic = "MojangServerGenerator")
public class MojangMethodGenerator {

    private final String version;
    private final String paperUrl;
    private final String spigotMappingsUrl;
    private final String mojangMappingsUrl;

    private File paperFile;
    private File mojangMappingsFile;
    private File spigotMappingsFile;
    private File mojangPaperFile;

    private final File workDir;

    public MojangMethodGenerator(Properties properties) {
        version = properties.getProperty("version");
        paperUrl = properties.getProperty("paper-version");
        spigotMappingsUrl = properties.getProperty("spigot-mappings");
        mojangMappingsUrl = properties.getProperty("mojang-mappings");
        workDir = new File("cache/");
        if (workDir.exists()) {
            try {
                FileUtils.deleteDirectory(workDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        workDir.mkdir();
    }

    public void load() {
        log.info("Download Paper jar...");
        File paperclipFile = new File(workDir, "paperclip.jar");
        if (!downloadFile(paperUrl, paperclipFile)) {
            log.info("Download failed.");
            return;
        }
        log.info("Download Spigot mappings ...");
        spigotMappingsFile = new File(workDir, "spigot-mappings-" + version + ".txt");
        if (!downloadFile(spigotMappingsUrl, spigotMappingsFile)) {
            log.info("Download failed.");
            return;
        }
        log.info("Download Mojang mappings ...");
        mojangMappingsFile = new File(workDir, "mojang-mappings-" + version + ".txt");
        if (!downloadFile(mojangMappingsUrl, mojangMappingsFile)) {
            log.info("Download failed.");
            return;
        }
        log.info("All files has been downloaded.");
        log.info("Start patching paper ...");
        try {
            Process process = Runtime.getRuntime().exec("java -Dpaperclip.patchonly=true -Xmx512M -jar " + paperclipFile.getName(), new String[]{}, workDir);
            Thread thread = new Thread(() -> {
                while (process.isAlive()) {
                    try {
                        try (Scanner scanner = new Scanner(process.getInputStream())) {
                            while (scanner.hasNextLine()) {
                                log.info(scanner.nextLine());
                            }
                        }
                        Thread.sleep(50L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.setName("Paper Patch");
            thread.start();

            process.waitFor();

            try (Scanner scanner = new Scanner(process.getErrorStream())) {
                paperFile = new File(workDir, "cache/patched_" + version + ".jar");
                if (!scanner.hasNextLine() && paperFile.exists()) {
                    log.info("Paper was successfully patched");
                    applyMonjangMethods();
                } else {
                    log.error("Paper patch failed:");
                    while (scanner.hasNextLine()) {
                        log.warn(scanner.nextLine());
                    }
                    return;
                }
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void applyMonjangMethods() {
        log.info("Generate mapping table ...");
        try {
            MojangMappings.load(mojangMappingsFile, spigotMappingsFile);
            log.info("Mapping table was generated.");
        } catch (IOException | MappingParseException e) {
            log.error("Failed to generate mapping table", e);
            return;
        }

        log.info("Load resources from paper jar ...");
        List<Resource> resources;
        try {
            resources = JarFileManager.loadResources(paperFile);
        } catch (IOException e) {
            log.error("Failed to load resources", e);
            return;
        }

        log.info("Add deobfuscated nms mojang methods");
        int globalCount = 0;
        for (Resource resource : resources) {
            if (resource.isMinecraftServer() && resource.isClassFile()) {
                ClassTransformer classTransformer = new ClassTransformer(resource, MojangMappings.classMethodList.get(resource.getSimpleName()));
                try {
                    int count = classTransformer.addMojangMethods();
                    globalCount = globalCount + count;
                    if (count > 0) {
                        log.info(resource.getSimpleName() + " >> " + count + " method" + (count > 1 ? "s" : "") + " added");
                    }
                    if (classTransformer.isModified() && Main.verify) {
                        try (FileOutputStream fileOutputStream = new FileOutputStream("verify.txt"); PrintWriter printWriter = new PrintWriter(fileOutputStream)) {
                            classTransformer.verify(printWriter);
                        }
                    }
                } catch (IOException e) {
                    log.error("Failed to add methods to class " + resource.getFileName(), e);
                    return;
                }
            }
        }
        log.info(globalCount + " methods were added");
        log.info("Create new Jar file ...");
        mojangPaperFile = new File(workDir, "mojang_patched_" + version + ".jar");
        try {
            JarFileManager.saveResources(resources, mojangPaperFile);
            log.info("Creation of the file was completed.");
            log.info("File saved as " + mojangPaperFile.getPath());
        } catch (IOException e) {
            log.error("Failed to save the file", e);
            return;
        }
    }

    private boolean downloadFile(String url, File file) {
        try {
            URLConnection urlConnection = new URL(url).openConnection();
            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.29 Safari/537.36");
            try (BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }
                return file.exists();
            }
        } catch (IOException e) {
            log.error("Download failed from " + url, e);
            return false;
        }
    }

    public void startServer(String[] args){
        Path paperFile = mojangPaperFile.toPath();
        String main = getMainClass(paperFile);
        Method mainMethod = getMainMethod(paperFile, main);
        try {
            mainMethod.invoke(null, new Object[] { args });
        } catch (IllegalAccessException|java.lang.reflect.InvocationTargetException e) {
            System.err.println("Error while running patched jar");
            e.printStackTrace();
            System.exit(1);
        }
    }



    private String getMainClass(Path paperJar) {
        try(InputStream is = new BufferedInputStream(Files.newInputStream(paperJar, new OpenOption[0]));
            JarInputStream js = new JarInputStream(is)) {
            return js.getManifest().getMainAttributes().getValue("Main-Class");
        } catch (IOException e) {
            System.err.println("Error reading from patched jar");
            e.printStackTrace();
            System.exit(1);
            throw new InternalError();
        }
    }

    private Method getMainMethod(Path paperJar, String mainClass) {
        Agent.addToClassPath(paperJar);
        try {
            Class<?> cls = Class.forName(mainClass, true, ClassLoader.getSystemClassLoader());
            return cls.getMethod("main", new Class[] { String[].class });
        } catch (NoSuchMethodException|ClassNotFoundException e) {
            System.err.println("Failed to find main method in patched jar");
            e.printStackTrace();
            System.exit(1);
            throw new InternalError();
        }
    }
}
