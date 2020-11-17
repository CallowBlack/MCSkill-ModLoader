package main.mcagent;

import o.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.json.*;

import javax.swing.text.html.parser.Entity;

// Callback methods class
public class CustomMethods {

    public static List<AUX> serverProfiles;

    public static void onStart() {
        Aux config = aux.getConfig();

        // Getting server profiles objects
        CoM3 launcherRequest = new CoM3(config);
        COM3 response = (COM3)launcherRequest.request();
        serverProfiles = new ArrayList<>();
        for (Object profile : response.profiles)
        {
            cOm7 profileInfo = (cOm7)profile;
            serverProfiles.add((AUX)profileInfo.object);
        }
        System.out.println("[+] Server profiles was loaded.");

        // Check mods folder
        if (Util.checkOrCreateDirectories(PropertiesFields.modsFolderPath)){
            for (AUX profile : serverProfiles){
                Path serverDir = Paths.get(profile.getDir().replace('*', '_'));
                Path customModsDir = Paths.get(PropertiesFields.modsFolderPath + "/" + serverDir);
                if (Files.isDirectory(serverDir) && !Files.isDirectory(customModsDir)){
                    try {
                        Files.createDirectory(customModsDir);
                    } catch (IOException e) {
                        System.out.println("[-] Filed to create custom mods folder \"" + customModsDir +"\"");
                    }
                }
            }
            System.out.println("[+] Custom mods folders were updated successfully.");
        }
        else {
            System.out.println("[-] Failed to update custom mods directory.");
        }

        // Check exclude mods name file
        if (Util.checkFileOrCreate(PropertiesFields.excludeModsPath)){
            try {
                File excludeFile = PropertiesFields.excludeModsPath.toFile();
                String fileContent = Util.readFile(excludeFile);
                JSONObject jsonRoot = new JSONObject(fileContent.isEmpty() ? "{}" : fileContent);
                for (AUX profile : serverProfiles) {
                    if (!jsonRoot.has(profile.getTitle())){
                        jsonRoot.put(profile.getTitle(), Collections.emptyList());
                    }
                }
                Util.writeFile(excludeFile, jsonRoot.toString(4));
                System.out.println("[+] Exclude file was updated successfully.");
            } catch (IOException e) {
                System.out.println("[-] Failed to read exclude mods file.");
            }
        }

    }

    public static boolean onUpdateFile(Path path){
        System.out.println("[INFO] Update file: " + path);
        return true;
    }

    public static void onDeleteFiles(Path folder, NUl entries, boolean flag) throws IOException {
        Map<?, ?> map = Collections.unmodifiableMap(entries.map());
        for (Map.Entry<?, ?> entry : map.entrySet()){
            Path fullPath = folder.resolve(entry.getKey().toString());
            NUL value = (NUL)entry.getValue();
            switch (value.getType().ordinal()) {
                case 1:
                    if (folder.getFileName().toString().equals("mods")) {
                        Path customModsFolder = PropertiesFields.modsFolderPath.resolve(folder.getParent().getFileName());
                        if (Files.exists(customModsFolder.resolve(fullPath.getFileName())))
                            break;
                    }
                    Files.delete(fullPath);
                    break;
                case 2:
                    onDeleteFiles(fullPath, (NUl)value, value.flag || flag);
                    break;
            }
        }
        if (flag){
            Files.delete(folder);
        }
    }

    public static void onClientLaunch() {

    }

    public static void onRequestError(final String string){
        System.out.println("[ERROR] Request error: " + string);
    }
}