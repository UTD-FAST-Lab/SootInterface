package edu.utdallas.amordahl.transformers;

import polyglot.ast.Call;
import soot.Scene;
import soot.SceneTransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

public class CallgraphPrinter extends SceneTransformer {
    private final String output;

    private final boolean srcLineNumbers;

    public CallgraphPrinter(String output, boolean srcLineNumbers) {
        this.output = output;
        this.srcLineNumbers = srcLineNumbers;
    }
    protected void internalTransform(String s, Map<String, String> map) {
        try {
            FileWriter fw = new FileWriter(this.output);
            fw.write("caller\tcallsite\tcaller_context\ttarget\ttarget_context\n");
            Scene.v().getCallGraph().forEach(edge -> {
                try {
                    fw.write(String.format("%s\t%s\t%s\t%s\t%s\n",
                        edge.src(),
                        this.srcLineNumbers ?
                                String.format("%s:%d", edge.src().method().getDeclaringClass().toString().replace(".","/"),
                                        edge.srcUnit().getJavaSourceStartLineNumber()) :
                                edge.srcUnit().toString(),
                        edge.src().context(),
                        edge.tgt().method(),
                        edge.tgt().context()));
                } catch (IOException | NullPointerException e) {
                    e.printStackTrace();
                }
            });
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
