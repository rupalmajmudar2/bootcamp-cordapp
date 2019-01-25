package org.vloyalty.client;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.swingViewer.Viewer;

import java.util.Iterator;

public class GraphExplorer {
        public static void main(String args[]) {
            new GraphExplorer();
        }

        public GraphExplorer() {
            SingleGraph graph = new SingleGraph("GraphStreamTest");

            graph.addAttribute("ui.stylesheet", styleSheet);
            graph.setAutoCreate(true);
            graph.setStrict(false);
            Viewer viewer= graph.display();

            graph.addNode("A");
            Node e1=graph.getNode("A");
            e1.addAttribute("ui.style", "shape:circle;fill-color: yellow;size: 50px;");
            e1.addAttribute("ui.label", "Node A");

            graph.addNode("B");
            graph.addNode("C");
            graph.addNode("D");
            graph.addNode("E");
            graph.addNode("F");
            graph.addNode(1+"");
            graph.addNode(2+"");
            graph.addNode("Dummy");
            graph.addEdge("AB", "A", "B",true);
            //graph.addEdge(1+""+2+"", 1+"", 2+"",true);

            graph.addEdge("BC", "B", "C",true);
            Edge bc= graph.getEdge("BC");
            bc.addAttribute("ui.label", "Edge BC");

            graph.addEdge("CA", "C", "A",true);
            graph.addEdge("CD", "C", "D",true);
            graph.addEdge("DF", "D", "F",true);

            graph.addEdge("DD", "1", "1",true);
            graph.addEdge("Ddum", "D", "Dummy", true);
            graph.addEdge("Ddum2", "Dummy", "D", true);

            graph.addEdge("EF", "E", "F",true);
            graph.addEdge("DE", "D", "E",true);

            Node e4=graph.getNode("D");
            e4.addAttribute("ui.style", "shape:circle;fill-color: yellow;size: 90px; text-alignment: center;");
            e4.addAttribute("ui.label", "node D");

            for (Node node : graph) {
                node.addAttribute("ui.label", node.getId());
            }

            SpriteManager sman = new SpriteManager(graph);
            Sprite sa = sman.addSprite("SA");
            sa.setPosition(2, 1, 0);
            sa.attachToNode("A");

            explore(graph.getNode("A"));

            //graph.display();
        }

    public void explore(Node source) {
        Iterator<? extends Node> k = source.getBreadthFirstIterator();

        while (k.hasNext()) {
            Node next = k.next();
            next.setAttribute("ui.class", "marked");
            sleep();
        }
    }

    protected void sleep() {
        try { Thread.sleep(1000); } catch (Exception e) {}
    }

    protected String styleSheet =
            "node {" +
                    "	fill-color: black;" +
                    "}" +
                    "node.marked {" +
                    "	fill-color: red;" +
                    "}";
}
