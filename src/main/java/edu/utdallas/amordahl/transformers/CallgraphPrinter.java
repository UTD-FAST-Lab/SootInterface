package edu.utdallas.amordahl.transformers;

import soot.Scene;
import soot.SceneTransformer;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.Map;
import java.util.function.Consumer;

public class CallgraphPrinter extends SceneTransformer {
    protected void internalTransform(String s, Map<String, String> map) {
        Scene.v().getCallGraph().forEach(edge -> System.out.println(String.format("%s\t%s\t%s\t%s\t%s",
                edge.src(),
                edge.srcUnit(),
                edge.src().context(),
                edge.tgt().method(),
                edge.tgt().context())));
    }
}
