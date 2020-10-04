package de.foorcee.mappings;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Properties;

@Slf4j
public class Main {

    public static boolean debug;
    public static boolean verify;

    public static void main(String[] args) throws IOException {
        debug = Boolean.getBoolean("mappings.debug");
        verify = Boolean.getBoolean("mappings.verify");
        Properties properties = new Properties();
        properties.load(Main.class.getClassLoader().getResourceAsStream("version.properties"));

        MojangMethodGenerator generator = new MojangMethodGenerator(properties);
        generator.load();
        if(!Boolean.getBoolean("mappings.patchonly")){
            generator.startServer(args);
        }
    }
}
