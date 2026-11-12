import java.util.jar.*;
import java.util.*;
public class Inspect {
  public static void main(String[] a) throws Exception {
    try (JarFile jar = new JarFile("run/plugins/LuckPerms-Bukkit-5.5.71.jar")) {
      Enumeration<JarEntry> es = jar.entries();
      while (es.hasMoreElements()) {
        String n = es.nextElement().getName();
        if (n.contains("bukkit") && n.endsWith(".class") && !n.contains("$")) {
          if (n.contains("BukkitPlatform") || n.contains("Loader") || n.contains("Server")) {
            System.out.println(n);
          }
        }
      }
    }
  }
}
