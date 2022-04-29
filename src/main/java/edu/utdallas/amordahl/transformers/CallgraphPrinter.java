package edu.utdallas.amordahl.transformers;

import polyglot.ast.Call;
import soot.Scene;
import soot.SceneTransformer;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JNewArrayExpr;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.spark.geom.dataRep.CgEdge;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

public class CallgraphPrinter extends SceneTransformer {
    private final String output;

    public CallgraphPrinter(String output) {
        this.output = output;
    }

    protected void internalTransform(String s, Map<String, String> map) {
        try {
            FileWriter fw = new FileWriter(this.output);
            Scene.v().getCallGraph().forEach(edge -> {
                try {
                    fw.write(String.format("%s\t%s\t%s\t%s\t%s",
                            edge.src() == null ? "null" : edge.src().toString(),
                            edge.srcUnit() == null ? "null" : edge.srcUnit().toString(),
                            edge.src().context() == null ? "null": edge.src().context().toString(),
                            edge.tgt() == null ? "null": edge.tgt().toString(),
                            edge.tgt().context() == null ? "null": edge.tgt().context()).replace("\n", "") + "\n");
                } catch (IOException | NullPointerException e) {
                    System.err.println("Could not process edge " + edge.toString());
                }
            });
            System.out.println("Wrote callgraph to " + this.output);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getMethodCall(Edge edge) {
        if (edge.srcUnit() == null) {
            return "";
        }
        if (edge.srcUnit() instanceof JInvokeStmt) {
            JInvokeStmt jis = (JInvokeStmt) edge.srcUnit();
            return String.format("%s.%s", jis.getInvokeExpr().getMethod().getDeclaringClass().toString(),
                    jis.getInvokeExpr().getMethod().getName());
        } else if (edge.srcUnit() instanceof JAssignStmt) {
            JAssignStmt jas = (JAssignStmt) edge.srcUnit();
            if (jas.getRightOp() instanceof JStaticInvokeExpr) {
                return String.format("%s.%s",
                        ((JStaticInvokeExpr) jas.getRightOp()).getMethod().getDeclaringClass().toString(),
                        ((JStaticInvokeExpr) jas.getRightOp()).getMethod().getName());
            } else {
                return jas.getRightOpBox().getValue().toString();
            }
        } else {
            throw new RuntimeException("Could not figure out what the type of unit " + edge.srcUnit());
        }
    }
}
