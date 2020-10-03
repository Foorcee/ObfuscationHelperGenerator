package de.foorcee.mappings.data.mojang;

import cuchaz.enigma.EnigmaProfile;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import de.foorcee.mappings.Main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class MojangMappings {

    public static Map<String, String> mojangToBukkitClassNames = new HashMap<>();
    public static Map<String, List<EntryTreeNode<EntryMapping>>> classMethodList = new HashMap<>();

    public static void load() throws IOException, MappingParseException {
        File file = new File("server1_16_3.txt");
        EntryTree<EntryMapping> mappings =  MappingFormat.PROGUARD.getReader().read(file.toPath(), new ProgressListener() {
            @Override
            public void init(int i, String s) {
            }

            @Override
            public void step(int i, String s) {
                System.out.println(s);
            }
        }, EnigmaProfile.EMPTY.getMappingSaveParameters());

        Map<String, String> mojangClassMappings = new HashMap<>();
        Map<String, List<EntryTreeNode<EntryMapping>>> methodMap = new HashMap<>();

        mappings.getRootNodes().forEach(treeNode -> {
            if(treeNode.getEntry() instanceof ClassEntry){
                loadMojangClassNames(treeNode, mojangClassMappings);
                methodMap.putAll(loadMojangMethodNames(treeNode, treeNode.getValue().getTargetName()));
            }
        });

        Scanner scanner = new Scanner(new FileInputStream(new File("bukkit-1.16.3-class.csrg")));
        while (scanner.hasNextLine()){
            String line = scanner.nextLine();
            if(line.startsWith("#")) continue;
            String[] splitt = line.split(" ");
            String mojangClass = mojangClassMappings.get(splitt[0]);
            System.out.println(splitt[1] + " -> " + mojangClass);
            mojangToBukkitClassNames.put(mojangClass, splitt[1]);
            classMethodList.put(splitt[1], methodMap.get(mojangClass));
        }

        mojangToBukkitClassNames.forEach((mojang, bukkit) -> {
            System.out.println("-> " + bukkit);
            for (EntryTreeNode<EntryMapping> node : methodMap.get(mojang)) {
                MethodEntry entry = (MethodEntry) node.getEntry();
                System.out.println("+ " + node.getValue().getTargetName());
                for (TypeDescriptor desc : entry.getDesc().getArgumentDescs()) {
//                    System.out.println(desc.toString());
                }
            }
        });
    }

    private static void loadMojangClassNames(EntryTreeNode<EntryMapping> node, Map<String, String> mappings){
        if(node.getEntry() instanceof ClassEntry){
            String obfuscated = node.getEntry().getName();
            String deobfuscated = node.getValue().getTargetName();
            mappings.put(obfuscated, deobfuscated);
            mappings.putAll(loadInnerClassMappings(node, obfuscated, deobfuscated));
        }
    }

    private static Map<String, String> loadInnerClassMappings(EntryTreeNode<EntryMapping> node, String obfuscatedName, String deobfuscatedName){
        Map<String, String> mappings = new HashMap<>();
        for (EntryTreeNode<EntryMapping> childNode : node.getChildNodes()) {
            if(childNode.getEntry() instanceof ClassEntry){
                String obfuscatedChild = obfuscatedName +"$" + childNode.getEntry().getName();
                String deobfuscatedChild = deobfuscatedName +"$" + childNode.getValue().getTargetName();
                mappings.put(obfuscatedChild, deobfuscatedChild);

                mappings.putAll(loadInnerClassMappings(childNode, obfuscatedChild, deobfuscatedChild));
            }
        }
        return mappings;
    }

    private static Map<String, List<EntryTreeNode<EntryMapping>>> loadMojangMethodNames(EntryTreeNode<EntryMapping> node, String deobfuscatedName){
        Map<String, List<EntryTreeNode<EntryMapping>>> map = new HashMap<>();
        if(node.getEntry() instanceof ClassEntry){
            List<EntryTreeNode<EntryMapping>> list = new ArrayList<>();
            for (EntryTreeNode<EntryMapping> childNode : node.getChildNodes()) {
                if(childNode.getEntry() instanceof MethodEntry){
                    list.add(childNode);
                }

                if(childNode.getEntry() instanceof ClassEntry){
                    String deobfuscatedChild = deobfuscatedName +"$" + childNode.getValue().getTargetName();
                    map.putAll(loadMojangMethodNames(childNode, deobfuscatedChild));
                }
            }
            map.put(deobfuscatedName, list);
        }
        return map;
    }

    private static Map<String, String> loadInnerClassMethods(EntryTreeNode<EntryMapping> node, String obfuscatedName, String deobfuscatedName){
        Map<String, String> mappings = new HashMap<>();
        for (EntryTreeNode<EntryMapping> childNode : node.getChildNodes()) {
            if(childNode.getEntry() instanceof ClassEntry){
                String obfuscatedChild = obfuscatedName +"$" + childNode.getEntry().getName();
                String deobfuscatedChild = deobfuscatedName +"$" + childNode.getValue().getTargetName();
                mappings.put(obfuscatedChild, deobfuscatedChild);

                mappings.putAll(loadInnerClassMappings(childNode, obfuscatedChild, deobfuscatedChild));
            }
        }
        return mappings;
    }

}
