package de.foorcee.mappings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Ressource {
    private String className;
    private String internalName;
    private byte[] data;
    private boolean classFile;

    public boolean isMinecraftServer(){
        return className.startsWith("net/minecraft/server/v1_16_R2/");
    }

    public String getSimpleName(){
        int index = internalName.lastIndexOf("/");
        return internalName.substring(index +1);
    }
}
