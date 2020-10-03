package de.foorcee.mappings;

import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import de.foorcee.mappings.data.mojang.MojangMappings;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException, MappingParseException {
        File outputDir = new File("test/");
        if(outputDir.exists()) FileUtils.deleteDirectory(outputDir);
        MojangMappings.load();
        for (Ressource ressource : ServerFileReader.load()) {
            if(ressource.isMinecraftServer() && ressource.isClassFile()){
                System.out.println(ressource.getSimpleName());
                ClassTransformer transformer = new ClassTransformer(ressource, MojangMappings.classMethodList.get(ressource.getSimpleName()));

            }
        }
    }

}
