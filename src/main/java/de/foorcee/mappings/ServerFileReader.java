package de.foorcee.mappings;

import jdk.internal.org.objectweb.asm.ClassWriter;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ServerFileReader {

    public static List<Ressource> load() throws IOException {
        List<Ressource> ressourceList = new ArrayList<>();
        File file = new File("server.jar");
        JarFile jarFile = new JarFile(file);
        Enumeration<JarEntry> enumeration = jarFile.entries();
        while (enumeration.hasMoreElements()){
            JarEntry entry = enumeration.nextElement();
            ressourceList.add(open(jarFile, entry));
        }
        jarFile.close();
        return ressourceList;
    }

    private static Ressource open(JarFile jarFile, JarEntry jarEntry){
        String name = jarEntry.getName();
        try (InputStream inputStream = jarFile.getInputStream(jarEntry)){
            boolean classFile = name.endsWith(".class");
            String internalName = name;
            if(classFile){
                internalName = name.substring(0, name.length() - ".class".length());
            }

            byte[] data = IOUtils.toByteArray(inputStream);
            return new Ressource(name, internalName, data, classFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
