package callow.clientagent.patch;

import javassist.*;
import org.json.JSONObject;
import callow.common.IClassPatcher;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class HandshakePatcher implements IClassPatcher {
    @Override
    public boolean patch(ClassPool pool, CtClass ctClass) {
        String prefix;
        if (ctClass.getName().equals("cpw.mods.fml.common.network.handshake.FMLHandshakeCodec"))
            prefix = "cpw.mods";
        else if (ctClass.getName().equals("net.minecraftforge.fml.common.network.handshake.FMLHandshakeCodec"))
            prefix = "net.minecraftforge";
        else
            return false;

        try {
            CtMethod method = ctClass.getDeclaredMethod("encodeInto");

            method.insertBefore(String.format(
                    "if ($2.getClass().equals(%s.fml.common.network.handshake.FMLHandshakeMessage.ModList.class)) " +
                            "{ callow.clientagent.patch.HandshakePatcher.sendHandshakeModList(%s.fml.common.Loader.instance().getActiveModList(), $3, \"%s\"); return; }",
                    prefix, prefix, prefix));

            System.out.println("[+] Patcher | FMLHandshake patch created.");
        } catch (NotFoundException | CannotCompileException e) {
            System.out.println("[-] Patcher | FMLCodec patch process failed.");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void sendHandshakeModList(List<Object> modContainers, Object targetBuffer, String prefix) {
        try {
            final Class<?> ByteBufClass = Class.forName("io.netty.buffer.ByteBuf");

            final Class<?> ModContainerClass = Class.forName(prefix + ".fml.common.ModContainer");
            final Method MCGetSourceMethod = ModContainerClass.getMethod("getSource");

            final Class<?> HandshakeMessageClass = Class.forName(prefix + ".fml.common.network.handshake.FMLHandshakeMessage");
            final Class<?>[] HMNestedClasses = HandshakeMessageClass.getDeclaredClasses();
            final Optional<Class<?>> ModListMessageClassOpt = Arrays.stream(HMNestedClasses)
                    .filter(clazz -> clazz.getSimpleName().equals("ModList"))
                    .findFirst();
            if (!ModListMessageClassOpt.isPresent())
                throw new ClassNotFoundException("ModList");
            final Class<?> ModListMessageClass = ModListMessageClassOpt.get();

            final Constructor<?> MLConstructor = ModListMessageClass.getConstructor(List.class);
            final Method MLToBytesMethod = ModListMessageClass.getMethod("toBytes", ByteBufClass);
            final Method MLToAsStringMethod = ModListMessageClass.getMethod("modListAsString");

            JSONObject modeChangeList = new JSONObject(System.getenv("MODS_HANDSHAKE_EXCLUDED"));
            List<Object> newModContainers = new ArrayList<>();
            for (Object modContainer: modContainers) {
                File sourceFile = (File) MCGetSourceMethod.invoke(modContainer);
                if (!modeChangeList.has(sourceFile.getName()))
                    newModContainers.add(modContainer);
                else
                    System.out.printf("[+] Skipping '%s' file.\n", sourceFile.getName());
            }

            Object MLMessage = MLConstructor.newInstance(newModContainers);
            String modifiedMods = (String)MLToAsStringMethod.invoke(MLMessage);
            MLToBytesMethod.invoke(MLMessage, targetBuffer);

            System.out.println("[+] Mod list was successfully modified.");
            System.out.println("[+] New ModList: " + modifiedMods);

        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            System.out.printf("[-] Problem with: %s\n", e.getMessage());
        }
    }
}
