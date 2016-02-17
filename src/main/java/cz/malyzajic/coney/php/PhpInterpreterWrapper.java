package cz.malyzajic.coney.php;

import com.caucho.quercus.script.QuercusScriptEngineFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 *
 * @author daop
 */
public class PhpInterpreterWrapper {

    private final File phpScript;

    private long modified;

    private final QuercusScriptEngineFactory factory = new QuercusScriptEngineFactory();

    private ScriptEngine engine;

    private String script;

    public PhpInterpreterWrapper(File phpScript) {
        this.phpScript = phpScript;
        getInterpreter();
    }

    public boolean canBeUsed() {
        return getInterpreter() != null;
    }

    public final synchronized ScriptEngine getInterpreter() {
        if (phpScript.lastModified() != modified || engine == null) {
            try {
                engine = this.factory.getScriptEngine();

                script = new String(Files.readAllBytes(Paths.get(phpScript.getAbsolutePath())));

                modified = phpScript.lastModified();
            } catch (Exception ex) {
                engine = null;
                script = null;
                Logger.getLogger(PhpInterpreterWrapper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return engine;

    }

    public <T> T eval(Class<? extends T> returnClass, Bindings bindings) throws ScriptException {
        getInterpreter();
        if (engine == null || script == null) {
            return null;
        }
        return (T) engine.eval(script, bindings);
    }

}
