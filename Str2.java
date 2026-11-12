import java.util.jar.*;
import java.util.*;
import java.io.*;
public class Str2 {
  public static void main(String[] a) throws Exception {
    String[] jars = {"run/plugins/LuckPerms-Bukkit-5.5.71.jar"};
    for (String jp : jars) {
      try (JarFile jar = new JarFile(jp)) {
        Enumeration<JarEntry> es = jar.entries();
        while (es.hasMoreElements()) {
          JarEntry e = es.nextElement();
          if (!e.getName().endsWith(".class")) continue;
          try (InputStream in = jar.getInputStream(e)) {
            byte[] b = in.readAllBytes();
            String s = new String(b, "ISO-8859-1");
            if (s.contains("not compatible") || s.contains("1.8.8")) {
              System.out.println("FOUND: " + e.getName());
            }
          }
        }
      }
    }
  }
}
