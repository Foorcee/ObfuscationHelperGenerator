package de.foorcee.mappings;

import com.sun.org.apache.bcel.internal.generic.ALOAD;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassTransformer {

    private Map<String, EntryTreeNode<EntryMapping>> mappings = new HashMap<>();

    public ClassTransformer(Ressource ressource, List<EntryTreeNode<EntryMapping>> list) throws IOException {

        if(list == null) return;
        for (EntryTreeNode<EntryMapping> node : list) {
            MethodEntry entry = (MethodEntry) node.getEntry();
            String id = entry.getName()+entry.getDesc();
            mappings.put(id, node);
        }

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassNode classNode = new ClassNode();
        ClassReader reader =  new ClassReader(ressource.getData());
        reader.accept(classNode, 0);

        boolean modifyed = false;
        for (Object object : new ArrayList<>(classNode.methods)) {
            MethodNode method = (MethodNode) object;
            String id = method.name + method.desc;
            EntryTreeNode<EntryMapping> node = mappings.get(id);
            if(node != null){
                MethodEntry entry = (MethodEntry) node.getEntry();
                String deobfuscatedName = node.getValue().getTargetName();
                if(method.name.equals(deobfuscatedName)) continue;

                MethodNode methodNode = new MethodNode(method.access,deobfuscatedName, method.desc, method.signature, method.exceptions.toArray(new String[0]));
//                if(entry.getDesc().getReturnDesc().isVoid() && entry.getDesc().getArgumentDescs().isEmpty()){
//                    methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
//                    methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, classNode.name, method.name, method.desc, false));
//                    classNode.methods.add(methodNode);
//                    modifyed = true;
//                }

                if(Modifier.isAbstract(method.access) || Modifier.isStatic(method.access)) continue;

                if(!entry.getDesc().getArgumentDescs().isEmpty()){
                    System.out.println(classNode.name +" " + method.name + " ");
                    for (TypeDescriptor desc : entry.getDesc().getArgumentDescs()) {
                        System.out.println(desc.toString());
                    }
                    for (AbstractInsnNode instruction : method.instructions) {
                        System.out.println(instruction.getClass());
                    }
                }

                if(entry.getDesc().getArgumentDescs().isEmpty()){
                    LabelNode labelNode1 = new LabelNode(new Label());
                    methodNode.instructions.add(labelNode1);
                    methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));

                    methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, classNode.name, method.name, method.desc, false));
                    LabelNode labelNode2 = new LabelNode(new Label());
                    methodNode.instructions.add(labelNode2);
                    if(entry.getDesc().getReturnDesc().isVoid()){
                        methodNode.instructions.add(new InsnNode(Opcodes.RETURN));
                    }else{
                        methodNode.instructions.add(new InsnNode(Opcodes.IRETURN));
                    }
                    LocalVariableNode variableNode = new LocalVariableNode("this", "L" + classNode.name + ";", null, labelNode1, labelNode2, 0);

                    methodNode.localVariables.add(variableNode);
                    methodNode.maxStack = 1;
                    methodNode.maxLocals = 1;
                    classNode.methods.add(methodNode);
                    modifyed = true;
                    System.out.println("+ " +id + " -> " + deobfuscatedName);

                }
            }
        }

        if(!modifyed) return;

        classNode.accept(classWriter);
        File outputDir = new File("test/");
        File file = new File(outputDir, ressource.getSimpleName() + ".class");
        if(file.exists()) file.delete();

        outputDir.mkdirs();

        DataOutputStream dataOutputStream =
                null;
        try {
            dataOutputStream = new DataOutputStream(
                    new FileOutputStream(
                            file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        dataOutputStream.write(classWriter.toByteArray());
        //return classWriter.toByteArray();
    }
}
