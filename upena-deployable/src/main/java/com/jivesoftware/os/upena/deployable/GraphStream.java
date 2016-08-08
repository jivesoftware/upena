package com.jivesoftware.os.upena.deployable;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import org.graphstream.algorithm.PageRank;
import org.graphstream.algorithm.generator.DorogovtsevMendesGenerator;
import org.graphstream.algorithm.generator.Generator;
import org.graphstream.algorithm.randomWalk.RandomWalk;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

/**
 *
 * @author jonathan.colt
 */
public class GraphStream {

    public static void main(String args[]) throws InterruptedException {
        Graph graph = new SingleGraph("Hello");
        graph.addAttribute("ui.stylesheet", styleSheet);
        graph.addAttribute("ui.quality");
        graph.addAttribute("ui.antialias");

        int nodeCount = 100;
        
        Generator gen = new DorogovtsevMendesGenerator();
        gen.addSink(graph);
        gen.begin();

        for (int i = 0; i < nodeCount; i++) {
            gen.nextEvents();
        }

        gen.end();
        System.out.println("Built graph");

        graph.display();

        PageRank pageRank = new PageRank();
        pageRank.setVerbose(true);
        pageRank.init(graph);

        for (Node node : graph) {
            double rank = pageRank.getRank(node);
            int size = (int) (15 + (rank * 1000));
            System.out.println(size + "----");
            node.setAttribute("ui.size", size);//5 + Math.sqrt(graph.getNodeCount() * rank * 20));
            node.setAttribute("ui.label", String.format("%.2f%%", rank * 100));
        }

        RandomWalk rwalk = new RandomWalk();

        rwalk.setEntityCount(graph.getNodeCount() / 2);
        rwalk.setEntityCount(graph.getNodeCount() * 2);
        rwalk.setEvaporation(0.97);
        rwalk.setEntityMemory(40);
        rwalk.init(graph);

        for (int c = 0; c < 10000; c++) {
            rwalk.compute();
        }

        rwalk.terminate();
        updateGraph(graph, rwalk, pageRank);

        List<Edge> edges = Lists.newArrayList();
        for (Edge edge : graph.getEachEdge()) {
            edges.add(edge);
        }
        Collections.sort(edges, (Edge o1, Edge o2) -> -Double.compare(rwalk.getPasses(o1), rwalk.getPasses(o2)));

        for (Edge edge : edges) {
            System.out.println("Edge " + edge.getId() + " counts " + rwalk.getPasses(edge));
        }

        //graph.addAttribute("ui.screenshot", "randomWalk.png");
    }

    static public void updateGraph(Graph graph, RandomWalk rwalk, PageRank pageRank) {
        double mine = Double.MAX_VALUE;
        double maxe = Double.MIN_VALUE;

        // Obtain the maximum and minimum passes values.
        for (Edge edge : graph.getEachEdge()) {
            double passes = rwalk.getPasses(edge);
            if (passes > maxe) {
                maxe = passes;
            }
            if (passes < mine) {
                mine = passes;
            }
        }

        // Set the colors.
        for (Edge edge : graph.getEachEdge()) {
            double passes = rwalk.getPasses(edge);
            double color = ((passes - mine) / (maxe - mine));
            edge.setAttribute("ui.color", color);
        }

        for (Node node : graph) {
            //node.addAttribute("ui.size", size);
            node.setAttribute("ui.label", "(" + node.getId() + ")");// + String.format("%.2f%%", rank * 100));
        }

    }

    protected static String styleSheet =
        "edge {"
        + "	shape: cubic-curve;"
        + "	size: 4px;"
        + "	fill-color: red, yellow, green, #444;"
        + "	fill-mode: dyn-plain;"
        + "}"
        + "node {"
        + "	shape: cubic-curve;"
        + "	size: 14px;"
        + "}";

}
