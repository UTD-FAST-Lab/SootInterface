package edu.utdallas.amordahl;

import soot.PackManager;
import soot.Transform;
import edu.utdallas.amordahl.transformers.CallgraphPrinter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        String output = "callgraph.json";
        ArrayList<String> args_as_list = new ArrayList<>(Arrays.asList(args));
        if (args_as_list.contains("--callgraph-output")) {
            int index = args_as_list.indexOf("--callgraph-output");
            output = args_as_list.get(index + 1);
            args_as_list.remove(index);
            args_as_list.remove(index);
            args = args_as_list.toArray(new String[] {});
        }
        long timeout = 7200000; // 2 hours
        if (args_as_list.contains("--timeout")) {
            int index = args_as_list.indexOf("--timeout");
            timeout = Long.parseLong(args_as_list.get(index + 1));
            args_as_list.remove(index);
            args_as_list.remove(index);
            args = args_as_list.toArray(new String[] {});
        }
        PackManager.v().getPack("wjtp").add(
                new Transform("wjtp.print-callgraph", new CallgraphPrinter(output))
        );
        //System.err.println("Args is " + List.of(args));
        ExecutorService service = Executors.newSingleThreadExecutor();
        String[] finalArgs = args;
        Future task = service.submit(new Runnable() {
            @Override
            public void run() {
                soot.Main.main(finalArgs);
            }
        });
        try {
            task.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException te) {
            System.err.println("Timed out!");
            task.cancel(true);
        } finally {
            System.err.println("Shutting down.");
            service.shutdownNow();
            System.exit(1);
        }
    }
}
