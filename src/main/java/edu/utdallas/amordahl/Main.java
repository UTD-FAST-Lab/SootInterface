package edu.utdallas.amordahl;

import soot.PackManager;
import soot.Transform;
import edu.utdallas.amordahl.transformers.CallgraphPrinter;

public class Main {
    public static void main(String[] args) {
        PackManager.v().getPack("wjtp").add(
                new Transform("wjtp.callgraphPrinter", new CallgraphPrinter())
        );
        soot.Main.main(args);
    }
}
