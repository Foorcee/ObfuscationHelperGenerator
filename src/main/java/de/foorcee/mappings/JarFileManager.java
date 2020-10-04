package de.foorcee.mappings;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class JarFileManager {

    public static List<Resource> loadResources(File file) throws IOException {
        List<Resource> ressourceList = new ArrayList<>();
        JarFile jarFile = new JarFile(file);
        Enumeration<JarEntry> enumeration = jarFile.entries();
        while (enumeration.hasMoreElements()) {
            JarEntry entry = enumeration.nextElement();
            ressourceList.add(open(jarFile, entry));
        }
        jarFile.close();
        return ressourceList;
    }

    private static Resource open(JarFile jarFile, JarEntry jarEntry) {
        String name = jarEntry.getName();
        try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
            boolean classFile = name.endsWith(".class");
            String internalName = name;
            if (classFile) {
                internalName = name.substring(0, name.length() - ".class".length());
            }

            byte[] data = IOUtils.toByteArray(inputStream);
            return new Resource(name, internalName, data, classFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void saveResources(List<Resource> ressourceList, File file) throws IOException {
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(file));

        for (Resource ressource : ressourceList) {
            byte[] data = ressource.getData();
            JarEntry entry = new JarEntry(ressource.getFileName());
            entry.setTime(0L);
            entry.setSize(data.length);
            jarOutputStream.putNextEntry(entry);
            jarOutputStream.write(data);
            jarOutputStream.closeEntry();
        }

        jarOutputStream.close();
    }

}
