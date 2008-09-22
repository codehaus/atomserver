package org.atomserver.utils.spring;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.TargetError;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;

public class StartupConstraints implements ApplicationContextAware {
    private static final Log log = LogFactory.getLog(StartupConstraints.class);

    private Interpreter interpreter = new Interpreter();
    private String constraints;

    public StartupConstraints() throws EvalError {
        interpreter.eval("reject(m) {throw new RuntimeException(m);}");
    }

    public void setConstraints(String constraints) {
        this.constraints = constraints;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        try {
            log.debug("setting context!");
            interpreter.set("context", applicationContext);

            log.debug("evaluating constraints:\n----------\n" + constraints + "\n----------");
            try {
                interpreter.eval(constraints);
            } catch (TargetError e) {
                throw e.getTarget() instanceof RuntimeException ? 
                      (RuntimeException) e.getTarget() : new RuntimeException(e.getTarget());
            }
        } catch (EvalError evalError) {
            throw new RuntimeException(evalError);
        }
    }
}
