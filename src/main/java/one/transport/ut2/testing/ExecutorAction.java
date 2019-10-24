package one.transport.ut2.testing;

import java.util.concurrent.Executor;

/**
 * @author Bulychev Ivan
 * @date 14.08.19.
 */
public class ExecutorAction implements Runnable {
    private final Executor executor;
    private final Runnable action;

    public ExecutorAction(Executor executor, Runnable action) {
        this.executor = executor;
        this.action = action;
    }

    public void execute() {
        executor.execute(this);
    }

    @Override
    public void run() {
        action.run();
    }
}
