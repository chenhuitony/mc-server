import com.mc.server.McServer;
import com.mc.plugin.LoadedPlugin;
import java.io.File;
public class Dbg {
  public static void main(String[] a) throws Exception {
    com.mc.util.Log.init(null);
    com.mc.util.JulBridge.install();
    File jar = new File("run/plugins/Vault-34315.jar");
    LoadedPlugin lp = new LoadedPlugin(jar);
    System.out.println("metadata ok=" + lp.loadMetadata() + " errors=" + lp.getErrors());
    System.out.println("desc=" + lp.getDescription());
    lp.instantiate(Dbg.class.getClassLoader());
    System.out.println("instance=" + lp.getInstance());
    System.out.println("errors=" + lp.getErrors());
    if (lp.getInstance() != null) {
      System.out.println("before inject getDescription=" + lp.getInstance().getDescription());
      lp.inject(null, new File("run/plugins"));
      System.out.println("after inject getDescription=" + lp.getInstance().getDescription());
    }
  }
}
