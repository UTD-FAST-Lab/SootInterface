package edu.utdallas.amordahl;

import soot.PackManager;
import soot.Transform;
import edu.utdallas.amordahl.transformers.CallgraphPrinter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String output = "callgraph.tsv";
        ArrayList<String> args_as_list = new ArrayList<String>(Arrays.asList(args));
        if (args_as_list.contains("--callgraph-output")) {
            int index = args_as_list.indexOf("--callgraph-output");
            output = args_as_list.get(index + 1);
            args_as_list.remove(index);
            args_as_list.remove(index);
            args = args_as_list.toArray(new String[] {});
        }
        PackManager.v().getPack("wjtp").add(
                new Transform("wjtp.print-callgraph", new CallgraphPrinter(output))
        );
        soot.Main.main(args);
    }
}
