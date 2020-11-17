package main.mcagent;

import javassist.*;
import javassist.bytecode.Descriptor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.regex.Pattern;

public class ClassTransformer implements ClassFileTransformer {

    private final ClassPool pool;
    ClassTransformer() {
        pool = ClassPool.getDefault();
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        className = className.replace('/', '.');
        byte[] classBytes = new byte[0];
        if (Pattern.matches("^o.[^.]+$", className)){
            if (PropertiesFields.debug)
                System.out.println("[Debug] Class loaded: " + className);

            // Catching classes
            CtClass currentClass;
            try {
                currentClass = pool.get(className);
            } catch (NotFoundException e) {
                if (PropertiesFields.debug)
                    System.out.println("[Debug] Failed load class: " + className);
                return classBytes;
            }

            // Main class
            switch (className) {
                case "o.aux":
                    try {
                        // Method start running after program initialization
                        CtMethod method = currentClass.getDeclaredMethod("start");

                        // Insert in begin method our callback method
                        method.insertBefore("main.mcagent.CustomMethods.onStart();");

                        classBytes = currentClass.toBytecode();
                        System.out.println("[+] Launcher | aux.start(): Callback was installed.");

                    } catch (NotFoundException | IOException | CannotCompileException e) {
                        System.out.println("[-] Launcher | aux.start(): Callback install failed.");
                    }
                    break;

                // File updating class
                case "o.Com4":
                    // Method aux used for every check file
                    // final Path path, final Prn prn, final InputStream inputStream
                    // aux(final String s, final long n, final long n2)

                    // Param for method
                    try {
                        // Update callback
                        CtClass[] paramTypes = {
                                pool.get("java.nio.file.Path"),
                                pool.get("o.Prn"),
                                pool.get("java.io.InputStream"),
                        };

                        CtMethod method = currentClass.getMethod("aux",
                                Descriptor.ofMethod(CtPrimitiveType.voidType, paramTypes));
                        method.insertBefore("if (!main.mcagent.CustomMethods.onUpdateFile($1)) {return;}");

                        // Delete callback
                        // aux(final Path path, final NUl nUl, final boolean b)
                        paramTypes = new CtClass[]{
                                pool.get("java.nio.file.Path"),
                                pool.get("o.NUl"),
                                CtPrimitiveType.booleanType,
                        };

                        method = currentClass.getMethod("aux",
                                Descriptor.ofMethod(CtPrimitiveType.voidType, paramTypes));
                        method.setBody("main.mcagent.CustomMethods.onDeleteFiles($1, $2, $3);");

                        System.out.println("[+] Update | Com4.aux(): Callback was installed.");

                        classBytes = currentClass.toBytecode();
                    } catch (NotFoundException | CannotCompileException | IOException e) {
                        System.out.println("[-] Update | Com4.aux(): Callback install failed.");
                        e.printStackTrace();
                    }
                    break;

                // Minecraft Client launch
                case "o.AUx":

                    // Minecraft Client launch method
                    // launch
                    try {
                        CtClass[] paramTypes = new CtClass[]{
                                pool.get("o.AUX"),
                                pool.get("o.aUX"),
                        };

                        CtMethod method = currentClass.getDeclaredMethod("launch");

                        method.insertBefore("main.mcagent.CustomMethods.onClientLaunch($6);");

                        System.out.println("[+] ClientLaunch | AUx.launch(): Callback was installed.");

                        classBytes = currentClass.toBytecode();

                    } catch (NotFoundException | CannotCompileException | IOException e) {
                        System.out.println("[-] ClientLaunch | AUx.launch(): Callback install failed.");
                        e.printStackTrace();
                    }


                    break;
            }
        }
        return classBytes;
    }
}
