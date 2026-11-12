import com.mc.plugin.LoadedPlugin;
import java.io.File;
import java.lang.reflect.Method;
public class Dbg2 {
  public static void main(String[] a) throws Exception {
    com.mc.util.Log.init(null);
    LoadedPlugin lp = new LoadedPlugin(new File("run/plugins/Vault-34315.jar"));
    lp.loadMetadata();
    lp.instantiate(Dbg2.class.getClassLoader());
    Object obj = lp.getInstance();
    Class<?> c = obj.getClass();
    while (c != null) {
      System.out.println("class: " + c.getName());
      for (Method m : c.getDeclaredMethods()) {
        if (m.getName().startsWith("set")) {
          System.out.println("   " + m.getName() + "(" + java.util.Arrays.toString(m.getParameterTypes()) + ")");
        }
      }
      c = c.getSuperclass();
    }
  }
}
