package org.atomserver.utils.spring;

import bsh.Interpreter;
import bsh.TargetError;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;

/**
 * StartupConstraints - provide a way to programmatically make some assertions about a spring context on startup.
 *
 * This class uses BeanShell to implement a simple mechanism for programmatically checking the
 * configuration of an application context on startup, and failing to start if things do not
 * conform.  This class takes a single property - "constraints" - that contains a BeanShell script
 * to be run.  Several things are made available to the script:
 *
 * -  the function reject(message) which will throw a RuntimeException with the given message
 * -  the bean "context" which is the ApplicationContext in which this bean is instantiated.  from
 *    this bean, anything else in the context can be retrieved
 *
 * any properties configured in the PropertyPlaceholderConfigurer can be replaced in the BeanShell
 * script, so assertions can be made about the values of those properties as well.
 *
 * any beans referenced from the context should be declared in the "depends-on" attribute of the
 * bean declaration for the StartupConstraints.
 */
public class StartupConstraints implements ApplicationContextAware {
    private static final Log log = LogFactory.getLog(StartupConstraints.class);

    private String constraints;

    public void setConstraints(String constraints) {
        // this method is called BEFORE the setApplicationContext method, so we need to hang on
        // to the constraints until then.
        this.constraints = constraints;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        try {
            Interpreter interpreter = new Interpreter();

            // declare the reject method
            interpreter.eval("reject(m) {throw new RuntimeException(m);}");

            // provide the context to the BeanShell script
            interpreter.set("context", applicationContext);

            log.debug("evaluating constraints:\n----------\n" + constraints + "\n----------");
            try {
                // evaluate the provided constraints
                interpreter.eval(constraints);
            } catch (TargetError e) {
                // if the TargetError was a runtime exception (it almost always will be) just
                // re-throw it.  otherwise, wrap it in a RuntimeException and throw that.
                throw e.getTarget() instanceof RuntimeException ? 
                      (RuntimeException) e.getTarget() : new RuntimeException(e.getTarget());
            }
        } catch (Exception e) {
            // if anything else happens as the result of evaluating the constraints, throw a
            // RuntimeException  
            throw new RuntimeException(e);
        }
    }
}
