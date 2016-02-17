package cz.malyzajic.coney.bsh;

import bsh.EvalError;
import bsh.Interpreter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

public class BshInterpreterWrapper {

    private final File bshScript;

    private long modified;

    private Interpreter bshInterpreter;

    private final AtomicLong parameterCounter = new AtomicLong();

    public BshInterpreterWrapper(File bshScript) {
        this.bshScript = bshScript;
    }

    public boolean canBeUsed() {
        return getInterpreter() != null;
    }

    public synchronized Interpreter getInterpreter() {
        if (bshScript.lastModified() != modified || bshInterpreter == null) {
            try {
                bshInterpreter = new Interpreter();
                bshInterpreter.source(bshScript.getPath());
                //bshInterpreter.set("SCRIPT_PATH", bshScript.getPath());
                //bshInterpreter.set("SCRIPT_DIR", bshScript.getParentFile().getPath());
                modified = bshScript.lastModified();
            } catch (IOException | EvalError ex) {
                bshInterpreter = null;
            }
        }
        return bshInterpreter;

    }

    public String createUniqueParamName() {
        return "_p" + parameterCounter.incrementAndGet();
    }

    public String createUniqueParamNameAndSet(Object valueToSet) throws EvalError {
        String param = createUniqueParamName();
        set(param, valueToSet);
        return param;
    }

    public Object eval(String... statements) throws EvalError {
        return eval(Object.class, statements);
    }

    public <T> T eval(Class<? extends T> returnClass, String... statements) throws EvalError {
        if (statements.length == 1) {
            return (T) bshInterpreter.eval(statements[0]);
        } else {
            StringBuilder buff = new StringBuilder();
            for (String statement : statements) {
                buff.append(statement);
            }
            return (T) bshInterpreter.eval(buff.toString());
        }
    }

    public Object get(String paramName) throws EvalError {
        return bshInterpreter.get(paramName);
    }

    public BshInterpreterWrapper set(String paramName, Object o) throws EvalError {
        if (paramName != null) {
            getInterpreter();
            bshInterpreter.set(paramName, o);
        }
        return this;
    }

    public BshInterpreterWrapper unset(String paramName) {
        try {
            if (paramName != null) {
                bshInterpreter.unset(paramName);
            }
        } catch (Exception ex) {
        }
        return this;
    }

    public BshInterpreterWrapper unsetAll(Collection<String> paramNames) {
        try {
            for (String paramName : paramNames) {
                unset(paramName);
            }
        } catch (Exception ex) {

        }
        return this;
    }

    public BshInterpreterWrapper unsetAll(String... paramNames) {
        for (String paramName : paramNames) {
            unset(paramName);
        }
        return this;
    }

}
