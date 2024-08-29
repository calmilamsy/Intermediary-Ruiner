package net.glasslauncher.intermediaryruiner;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.enigma.EnigmaDirReader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Dear future readers, this is possibly some of the laziest, dirtiest code I've written in *years*.
 * Don't do what I did if you're making a real program to be used by real people. It's fucking awful.
 */
public class Main {
    private static final int CLIENT = 0;
    private static final int SERVER = 1;
    // Cursed mappings keep being cursed. Yes, the client and server are reversed.
    // and yes, there's a mystery 0 index that absolutely no one I know knows what it's doing there.
    private static final int CURSED_CLIENT = 2;
    private static final int CURSED_SERVER = 1;

    private static MemoryMappingTree mappings;
    private static MemoryMappingTree cursedInt;
    private static MemoryMappingTree intermediaries;

    public static void main(String[] args) throws IOException {

        File mappingsDir = new File("mappings");
        File intermediariesFile = new File("mappings.tiny");
        // Mappings don't contain obf names, so I have to do some *cursed* shit later on.
        File cursedIntFile = new File(".gradle/minecraft/b1.7.3-intermediary-v2.tiny");
        File output = new File("output");
        File outputTiny = new File(output + ".tiny");
        File bridgeOutput = new File(output + "_bridged");
        bridgeOutput.delete();
        output.delete();
        outputTiny.delete();

        System.out.println("Loading mappings folder");
        if (!mappingsDir.exists()) {
            System.err.println("Mappings folder not found! Make sure you are running this inside your yarn folder.");
            System.out.println(2);
            return;
        }

        mappings = new MemoryMappingTree();
        MappingReader.read(mappingsDir.toPath(), MappingFormat.ENIGMA_DIR, mappings);

        System.out.println("Loading source intermediaries file");
        if (!cursedIntFile.exists()) {
            System.err.println("Source intermediaries not found. Make sure you have ran yarn at least once");
            System.out.println(2);
            return;
        }

        cursedInt = new MemoryMappingTree();
        MappingReader.read(cursedIntFile.toPath(), MappingFormat.TINY_2_FILE, cursedInt);

        System.out.println("Loading target intermediaries file");
        if (!intermediariesFile.exists()) {
            System.err.println("Target intermediaries not found. Make sure they are named \"mappings.tiny\"");
            System.out.println(2);
            return;
        }

        intermediaries = new MemoryMappingTree();
        MappingReader.read(intermediariesFile.toPath(), MappingFormat.TINY_2_FILE, intermediaries);

        System.out.println("Mappings loaded, now causing a massacre.");

        mappings.getClasses().forEach(cursedClass -> {
            MappingTree.ClassMapping cursedIntClass = cursedInt.getClass(cursedClass.getSrcName());
            MappingTree.ClassMapping intClientClass = null;
            MappingTree.ClassMapping intServerClass = null;

            if (cursedIntClass != null) {
                intClientClass = intermediaries.getClass(cursedIntClass.getDstName(CURSED_CLIENT), CLIENT);
                intServerClass = intermediaries.getClass(cursedIntClass.getDstName(CURSED_SERVER), SERVER);
            }

            if (intClientClass != null) {
                setSrcName(intClientClass.getSrcName(), cursedClass);
            }

            if (intServerClass != null) {
                setSrcName(intServerClass.getSrcName(), cursedClass);
            }

            handleMethods(cursedClass.getMethods(), intClientClass, intServerClass, cursedIntClass);
            handleFields(cursedClass.getFields(), intClientClass, intServerClass, cursedIntClass);
        });

        System.out.println("Converted, saving to \"output\"");

        mappings.accept(MappingWriter.create(output.toPath(), MappingFormat.ENIGMA_DIR));

        System.out.println("Ultra ruining, now making an ornithe compatible tiny file (hopefully.)");

        mappings = new MemoryMappingTree();
        EnigmaDirReader.read(mappingsDir.toPath(), "intermediary", "named", mappings);

        mappings.accept(MappingWriter.create(outputTiny.toPath(), MappingFormat.TINY_2_FILE));

        String file = Files.readString(outputTiny.toPath(), StandardCharsets.UTF_8);
        outputTiny.delete();
        // God this is such a fucking lazy hack.
        Files.writeString(outputTiny.toPath(), file.replaceAll("([a-z_0-9<>]+)\\t\n", "$1\t$1\n"));
        System.out.println("Done! If you have issues, good luck, you'll need it where we're going.");
    }

    record MethodRecord(String id, MappingTree.MethodMapping method) {}

    record FieldRecord(String id, MappingTree.FieldMapping field) {}

    // Ultra lazy method, cause I didn't feel like remembering how equals worked.
    public static String generateSemiUniqueID(MappingTree.MemberMapping member, int side) {
        return member.getOwner().getName(side) + "\t" + member.getName(side) + "\t" + member.getDesc(side);
    }

    public static void handleMethods(Collection<? extends MappingTree.MethodMapping> methods, MappingTree.ClassMapping intClientClass, MappingTree.ClassMapping intServerClass, MappingTree.ClassMapping cursedIntClass) {
        Map<String, MappingTree.MethodMapping> clientMethodIndex = intClientClass == null ? null : intClientClass.getMethods().stream().filter(methodMapping -> methodMapping.getName(CLIENT) != null).map(methodMapping -> new MethodRecord(generateSemiUniqueID(methodMapping, CLIENT), methodMapping)).collect(Collectors.toMap(record -> record.id, record -> record.method));
        Map<String, MappingTree.MethodMapping> serverMethodIndex = intServerClass == null ? null : intServerClass.getMethods().stream().filter(methodMapping -> methodMapping.getName(SERVER) != null).map(methodMapping -> new MethodRecord(generateSemiUniqueID(methodMapping, SERVER), methodMapping)).collect(Collectors.toMap(record -> record.id, record -> record.method));
        methods.forEach(cursedMethod -> {
            MappingTree.MethodMapping cursedIntClientMethod = cursedIntClass.getMethod(cursedMethod.getSrcName(), cursedMethod.getSrcDesc());
            MappingTree.MethodMapping cursedIntServerMethod = cursedIntClass.getMethod(cursedMethod.getSrcName(), cursedMethod.getSrcDesc());
            MappingTree.MethodMapping intClientMethod = clientMethodIndex == null || cursedIntClientMethod == null ? null : clientMethodIndex.get(generateSemiUniqueID(cursedIntClientMethod, CLIENT));
            MappingTree.MethodMapping intServerMethod = serverMethodIndex == null || cursedIntServerMethod == null ? null : serverMethodIndex.get(generateSemiUniqueID(cursedIntServerMethod, SERVER));

            if (intClientMethod != null) {
                setSrcName(intClientMethod.getSrcName(), cursedMethod);
            }

            if (intServerMethod != null) {
                setSrcName(intServerMethod.getSrcName(), cursedMethod);
            }
        });
    }

    // 99% copypaste of above, if you read above, you've read this.
    public static void handleFields(Collection<? extends MappingTree.FieldMapping> fields, MappingTree.ClassMapping intClientClass, MappingTree.ClassMapping intServerClass, MappingTree.ClassMapping cursedIntClass) {
        Map<String, MappingTree.FieldMapping> clientFieldIndex = intClientClass == null ? null : intClientClass.getFields().stream().filter(fieldMapping -> fieldMapping.getName(CLIENT) != null).map(fieldMapping -> new FieldRecord(generateSemiUniqueID(fieldMapping, CLIENT), fieldMapping)).collect(Collectors.toMap(record -> record.id, record -> record.field));
        Map<String, MappingTree.FieldMapping> serverFieldIndex = intServerClass == null ? null : intServerClass.getFields().stream().filter(fieldMapping -> fieldMapping.getName(SERVER) != null).map(fieldMapping -> new FieldRecord(generateSemiUniqueID(fieldMapping, SERVER), fieldMapping)).collect(Collectors.toMap(record -> record.id, record -> record.field));
        fields.forEach(cursedField -> {
            MappingTree.FieldMapping cursedIntClientField = cursedIntClass.getField(cursedField.getSrcName(), cursedField.getSrcDesc());
            MappingTree.FieldMapping cursedIntServerField = cursedIntClass.getField(cursedField.getSrcName(), cursedField.getSrcDesc());
            MappingTree.FieldMapping intClientField = clientFieldIndex == null || cursedIntClientField == null ? null : clientFieldIndex.get(generateSemiUniqueID(cursedIntClientField, CLIENT));
            MappingTree.FieldMapping intServerField = serverFieldIndex == null || cursedIntServerField == null ? null : serverFieldIndex.get(generateSemiUniqueID(cursedIntServerField, SERVER));

            if (intClientField != null) {
                setSrcName(intClientField.getSrcName(), cursedField);
            }

            if (intServerField != null) {
                setSrcName(intServerField.getSrcName(), cursedField);
            }
        });
    }


    private static Field srcName;

    /**
     * haha reflection go brrrr, your privacy is an illusion
     */
    public static void setSrcName(String name, MappingTree.ElementMapping elementMapping) {
        if (srcName == null) {
            try {
                srcName = elementMapping.getClass().getSuperclass().getDeclaredField("srcName");
                srcName.setAccessible(true);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }

        try {
            srcName.set(elementMapping, name);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
