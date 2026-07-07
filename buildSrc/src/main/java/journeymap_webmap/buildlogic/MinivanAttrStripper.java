package journeymap_webmap.buildlogic;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Removes {@code MethodParameters} and {@code Runtime[In]VisibleParameterAnnotations} from every
 * method in a jar.
 *
 * <p>minivan (via tiny-remapper / ASM) rewrites the mojmap Minecraft jar and emits these attributes
 * with a parameter count that does not match javac's expectation - e.g. an <em>empty</em>
 * {@code MethodParameters} on the 6-arg synthetic {@code ChatFormatting} enum constructor, plus a
 * parameter-annotation count of the full descriptor arity. Every javac (8 and 21) then reports
 * "bad RuntimeInvisibleParameterAnnotations attribute" and treats the class as an unreadable
 * bad-class-file, which aborts the whole compile. These attributes carry only reflective parameter
 * names/annotations that are irrelevant at compile time, so dropping them is safe and makes the jar
 * readable again.
 */
public final class MinivanAttrStripper
{
    private MinivanAttrStripper()
    {
    }

    /**
     * @return true if the jar still contains a class whose method carries one of the offending
     * attributes (cheap probe on ChatFormatting, which always has them when unstripped).
     */
    public static boolean needsStrip(File jar)
    {
        try (JarFile jf = new JarFile(jar))
        {
            JarEntry probe = jf.getJarEntry("net/minecraft/ChatFormatting.class");
            if (probe == null)
            {
                return false;
            }
            byte[] data = readAll(jf.getInputStream(probe));
            boolean[] found = {false};
            ClassReader cr = new ClassReader(data);
            cr.accept(new ClassVisitor(Opcodes.ASM9)
            {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex)
                {
                    return new MethodVisitor(Opcodes.ASM9)
                    {
                        @Override
                        public void visitParameter(String pname, int paccess)
                        {
                            found[0] = true;
                        }

                        @Override
                        public AnnotationVisitor visitParameterAnnotation(int parameter, String d, boolean visible)
                        {
                            found[0] = true;
                            return null;
                        }
                    };
                }
            }, 0);
            return found[0];
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed probing " + jar, e);
        }
    }

    /**
     * Rewrites {@code jar} in place with the offending attributes removed.
     */
    public static void stripInPlace(File jar)
    {
        File tmp = new File(jar.getParentFile(), jar.getName() + ".stripped.tmp");
        try (JarFile jf = new JarFile(jar);
             JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp.toPath()))))
        {
            Enumeration<JarEntry> en = jf.entries();
            while (en.hasMoreElements())
            {
                JarEntry e = en.nextElement();
                byte[] data = readAll(jf.getInputStream(e));
                if (e.getName().endsWith(".class"))
                {
                    data = strip(data);
                }
                JarEntry ne = new JarEntry(e.getName());
                ne.setTime(e.getTime());
                jos.putNextEntry(ne);
                jos.write(data);
                jos.closeEntry();
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed stripping " + jar, e);
        }
        try
        {
            Files.move(tmp.toPath(), jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed replacing " + jar + " with stripped copy", e);
        }
    }

    private static byte[] strip(byte[] in)
    {
        ClassReader cr = new ClassReader(in);
        ClassWriter cw = new ClassWriter(0);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw)
        {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex)
            {
                MethodVisitor mv = super.visitMethod(access, name, desc, sig, ex);
                return new MethodVisitor(Opcodes.ASM9, mv)
                {
                    @Override
                    public void visitParameter(String pname, int paccess)
                    {
                        // drop MethodParameters entries
                    }

                    @Override
                    public void visitAnnotableParameterCount(int count, boolean visible)
                    {
                        // drop the parameter-annotation count marker
                    }

                    @Override
                    public AnnotationVisitor visitParameterAnnotation(int parameter, String d, boolean visible)
                    {
                        // drop parameter annotations
                        return null;
                    }
                };
            }
        };
        cr.accept(cv, 0);
        return cw.toByteArray();
    }

    private static byte[] readAll(InputStream is) throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0)
        {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
