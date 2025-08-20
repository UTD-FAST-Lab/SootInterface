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
import soot.Unit;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;
import soot.options.Options; 
import soot.SootMethod;


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

    public static class CallSiteInfo {
        public String targetMethod;
        public int lineNumber = -1;
        public int bytecodeOffset = -1;

        public CallSiteInfo(String targetMethod) {
            this.targetMethod = targetMethod;
        }
    }

    protected void internalTransform(String s, Map<String, String> map) {
        // Use the new CallSiteInfo class for structured data
        System.err.println("DEBUG: Is keep_offset enabled? " + Options.v().keep_offset());
        Map<String, List<CallSiteInfo>> keyValuesMap = new HashMap<>();

        // We will collect all methods we've already scanned to avoid redundant printing
        Set<SootMethod> scannedMethods = new HashSet<>();

        CallGraph cg = Scene.v().getCallGraph();
        cg.forEach(edge -> {
            SootMethod srcMethod = edge.getSrc();

            // --- START OF EXPERIMENT ---
            // Only scan each method once.
            if (!scannedMethods.contains(srcMethod)) {
                System.out.println("\nScanning method: " + srcMethod.getSignature());
                if (srcMethod.hasActiveBody()) {
                    boolean foundAnyOffset = false;
                    for (Unit unit : srcMethod.getActiveBody().getUnits()) {
                        BytecodeOffsetTag bcTag = (BytecodeOffsetTag) unit.getTag("BytecodeOffsetTag");
                        if (bcTag != null) {
                            System.out.println("  -> Found offset " + bcTag.getBytecodeOffset() + " on unit: " + unit);
                            foundAnyOffset = true;
                        }
                    }
                    if (!foundAnyOffset) {
                        System.out.println("  -> No BytecodeOffsetTags found in this method's body.");
                    }
                } else {
                    System.out.println("  -> Method has no active body.");
                }
                scannedMethods.add(srcMethod);
            }
            // --- END OF EXPERIMENT ---
            try {
                String srcMethodstr = edge.getSrc().toString();
                String tgtMethodstr = edge.getTgt().toString();

                // Create a new info object for this call site
                CallSiteInfo callSite = new CallSiteInfo(tgtMethodstr);

                Unit srcUnit = edge.srcUnit();
                if (srcUnit != null) {
                    // Get the line number tag
                    LineNumberTag lnTag = (LineNumberTag) srcUnit.getTag("LineNumberTag");
                    if (lnTag != null) {
                        callSite.lineNumber = lnTag.getLineNumber();
                    }

                    // Get the bytecode offset tag (this is the PC)
                    BytecodeOffsetTag bcTag = (BytecodeOffsetTag) srcUnit.getTag("BytecodeOffsetTag");
                    if (bcTag != null) {
                        callSite.bytecodeOffset = bcTag.getBytecodeOffset();
                    }
                }

                // Add the structured information to the map
                keyValuesMap.computeIfAbsent(srcMethodstr, k -> new ArrayList<>()).add(callSite);

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

    private static void convertHashMapToJson(Map<String, List<CallSiteInfo>> map, String output) {
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
