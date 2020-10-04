package de.foorcee.mappings;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
public class Ressource {
    private String fileName;
    private String internalName;
    @Setter
    private byte[] data;
    private boolean classFile;

    public boolean isMinecraftServer(){
        return fileName.startsWith("net/minecraft/server/v1_16_R2/");
    }

    public String getSimpleName(){
        int index = internalName.lastIndexOf("/");
        return internalName.substring(index +1);
    }
}
