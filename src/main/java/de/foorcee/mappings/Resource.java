package de.foorcee.mappings;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
public class Resource {
    private final String fileName;
    private final String internalName;
    @Setter
    private byte[] data;
    private final boolean classFile;

    public boolean isMinecraftServer() {
        return fileName.startsWith("net/minecraft/server/v1_16_R2/");
    }

    public String getSimpleName() {
        int index = internalName.lastIndexOf("/");
        return internalName.substring(index + 1);
    }
}
