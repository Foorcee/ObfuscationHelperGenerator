package de.foorcee.mappings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class ServerFileSaver {

    public static void save(List<Ressource> ressourceList) throws IOException {
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(new File("mojang_server.jar")));

        for (Ressource ressource : ressourceList) {
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
