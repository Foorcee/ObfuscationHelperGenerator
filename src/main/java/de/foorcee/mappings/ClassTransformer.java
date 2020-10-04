package de.foorcee.mappings;

import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.*;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class ClassTransformer {

    private final Map<String, EntryTreeNode<EntryMapping>> mappings = new HashMap<>();
    private final Resource resource;
    @Getter
    private boolean modified = false;

    public ClassTransformer(Resource resource, List<EntryTreeNode<EntryMapping>> list) {
        this.resource = resource;
        if (list == null) return;

        Function<String, String> remapFunction = s -> {
            String remap = MojangMappings.mojangToBukkitClassNames.get(s);
            if (remap == null) {
                log.debug("Mapping from the class " + s + " is not available");
                return s;
            }
            return "net/minecraft/server/v1_16_R2/" + remap;
        };

        for (EntryTreeNode<EntryMapping> node : list) {
            MethodEntry entry = (MethodEntry) node.getEntry();
            entry.getDesc().getReturnDesc().remap(remapFunction);
            entry.getDesc().getArgumentDescs().forEach(typeDescriptor -> typeDescriptor.remap(remapFunction));
            String remappedDesc = entry.getDesc().remap(remapFunction).toString();
            String id = entry.getName() + remappedDesc;
            log.debug("Load id: " + id);
            mappings.put(id, node);
        }
    }


    public int addMojangMethods() throws IOException {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassNode classNode = new ClassNode();
        ClassReader reader = new ClassReader(resource.getData());
        reader.accept(classNode, 0);

        int methodCount = 0;
        for (Object object : new ArrayList<>(classNode.methods)) {
            MethodNode method = (MethodNode) object;
            String id = method.name + method.desc;
            EntryTreeNode<EntryMapping> node = mappings.get(id);
            if (node != null) {
                MethodEntry entry = (MethodEntry) node.getEntry();
                String deobfuscationName = node.getValue().getTargetName();
                if (method.name.equals(deobfuscationName)) continue;

                deobfuscationName = deobfuscationName + "NMS";

                if (Modifier.isAbstract(method.access) || Modifier.isStatic(method.access)) continue;

                MethodVisitor methodNode = classNode.visitMethod(method.access, deobfuscationName, method.desc, method.signature, method.exceptions.toArray(new String[0]));
                methodNode.visitCode();

                Label label1 = new Label();
                methodNode.visitLabel(label1);
                methodNode.visitVarInsn(Opcodes.ALOAD, 0);

                int index = 1;
                for (TypeDescriptor desc : entry.getDesc().getArgumentDescs()) {
                    Type type = Type.getType(desc.toString());
                    methodNode.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
                    index = index + type.getSize();
                }

                methodNode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classNode.name, method.name, method.desc, false);

                Type returnType = Type.getType(entry.getDesc().getReturnDesc().toString());
                methodNode.visitInsn(returnType.getOpcode(Opcodes.IRETURN));

                Label label3 = new Label();
                methodNode.visitLabel(label3);
                methodNode.visitLocalVariable("this", "L" + classNode.name + ";", null, label1, label3, 0);

                index = 1;
                for (TypeDescriptor desc : entry.getDesc().getArgumentDescs()) {
                    Type type = Type.getType(desc.toString());
                    methodNode.visitLocalVariable("var" + (index - 1), type.getDescriptor(), null, label1, label3, index);
                    index++;
                }
                methodNode.visitMaxs(-1, -1);
                methodNode.visitEnd();

                methodCount++;
                modified = true;
                log.debug("+ " + id + " -> " + deobfuscationName);

            } else {
                log.debug("Ignore method " + id);
            }
        }

        if (!modified) return methodCount;

        classNode.accept(classWriter);

        byte[] data = classWriter.toByteArray();

        if (Main.debug) {
            File outputDir = new File("debug/");
            File file = new File(outputDir, this.resource.getSimpleName() + ".class");
            if (file.exists()) file.delete();

            outputDir.mkdirs();

            try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file))) {
                dataOutputStream.write(data);
            }

        }
        resource.setData(data);
        return methodCount;
    }

    public void verify(PrintWriter printWriter) {
        CheckClassAdapter.verify(new ClassReader(resource.getData()), true, printWriter);
    }
}
