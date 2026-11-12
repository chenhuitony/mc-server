import java.util.jar.*;
import java.util.*;
import java.io.*;
public class Str {
  public static void main(String[] a) throws Exception {
    // 找出 LuckPerms 里包含版本检查字符串的类
    try (JarFile jar = new JarFile("run/plugins/LuckPerms-Bukkit-5.5.71.jar")) {
      Enumeration<JarEntry> es = jar.entries();
      while (es.hasMoreElements()) {
        JarEntry e = es.nextElement();
        if (!e.getName().endsWith(".class")) continue;
        try (InputStream in = jar.getInputStream(e)) {
          byte[] b = in.readAllBytes();
          String s = new String(b, "ISO-8859-1");
          if (s.contains("not compatible with this build")) {
            System.out.println("FOUND in: " + e.getName());
          }
        }
      }
    }
  }
}
