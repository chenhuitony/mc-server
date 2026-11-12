import java.util.jar.*;
import java.util.*;
import java.io.*;
public class Str3 {
  public static void main(String[] a) throws Exception {
    try (JarFile jar = new JarFile("run/plugins/LuckPerms-Bukkit-5.5.71.jar")) {
      Enumeration<JarEntry> es = jar.entries();
      while (es.hasMoreElements()) {
        JarEntry e = es.nextElement();
        if (!e.getName().endsWith(".class")) continue;
        try (InputStream in = jar.getInputStream(e)) {
          byte[] b = in.readAllBytes();
          StringBuilder sb = new StringBuilder();
          for (byte x : b) {
            if (x >= 32 && x < 127) sb.append((char)x);
            else sb.append('\n');
          }
          String s = sb.toString();
          if (s.contains("compatible with this build") || s.contains("Bukkit-Legacy")) {
            System.out.println("FOUND: " + e.getName());
          }
          if (s.contains("getBukkitVersion") || s.contains("Bukkit.getVersion")) {
            System.out.println("VERSION-REF: " + e.getName());
          }
        }
      }
    }
  }
}
