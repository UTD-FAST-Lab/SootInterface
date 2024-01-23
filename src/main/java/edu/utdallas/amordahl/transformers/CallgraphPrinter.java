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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CallgraphPrinter extends SceneTransformer {
    private final String output;

    public CallgraphPrinter(String output) {
        this.output = output;
    }

    protected void internalTransform(String s, Map<String, String> map) {
        Map<String, List<String>> keyValuesMap = new HashMap<>();

        Scene.v().getCallGraph().forEach(edge -> {
            try {
                addValue(keyValuesMap, edge.getSrc().toString(), edge.getTgt().toString());
//                System.out.println(edge.getSrc());
//                System.out.println(edge.getTgt());
            } catch (NullPointerException e) {
                System.err.println("Could not process edge " + edge.toString());
            }
        });

        convertHashMapToJson(keyValuesMap, this.output);
        System.out.println("Wrote callgraph to " + this.output);
    }

    private static void addValue(Map<String, List<String>> map, String key, String value) {
        // If the key is not present, create a new list
        map.putIfAbsent(key, new ArrayList<>());
        // Add the value to the list associated with the key
        map.get(key).add(value);
    }

    private static void convertHashMapToJson(Map<String, List<String>> map, String output) {
        try {
            // Create an ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();

            // Convert the HashMap to JSON string
            String jsonString = objectMapper.writeValueAsString(map);

            // Convert JSON string to a JSON object (JsonNode)
            Object jsonNode = objectMapper.readValue(jsonString, Object.class);
            objectMapper.writeValue(new File(output), jsonNode);

        } catch (IOException e) {
            // Handle exception if necessary
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
