package repair.vis;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.LinkSource;
import guru.nidi.graphviz.model.Node;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import repair.ast.MoNode;
import repair.ast.code.expression.MoExpression;
import repair.ast.code.expression.MoName;
import repair.ast.parser.NodeParser;
import repair.ast.visitor.DeepScanner;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static guru.nidi.graphviz.model.Factory.graph;
import static guru.nidi.graphviz.model.Factory.node;
import static guru.nidi.graphviz.model.Link.to;
import static repair.common.JDTUtils.genASTFromFile;
import static repair.common.JDTUtils.getOnlyMethodDeclaration;

public class ASTVisualizer {
    private static ASTVisualizer astVisualizer;

    private ASTVisualizer() {}

    public static synchronized ASTVisualizer getInstance() {
        if (astVisualizer == null) {
            astVisualizer = new ASTVisualizer();
        }
        return astVisualizer;
    }

    public static void main(String[] args) {
        Path path = Path.of("C:\\Users\\frexv\\Desktop\\codenavi\\CodeNavi-Generation\\08example\\code\\correct.java");
        CompilationUnit compilationUnit = genASTFromFile(path);
        Optional<MethodDeclaration> onlyMethodDeclarationOpt = getOnlyMethodDeclaration(compilationUnit);
        if (onlyMethodDeclarationOpt.isEmpty()) {
            System.out.println("No method declaration found");
            System.exit(1);
        }

        NodeParser nodeParser = new NodeParser(path, compilationUnit);
        MoNode moNode = nodeParser.process(onlyMethodDeclarationOpt.get());

        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Headless! No gui will be displayed");
            System.exit(1);
        } else {
            ASTVisualizer.getInstance().visualize(moNode);
        }
    }

    public void visualize(MoNode moNode) {
        Graph graph = buildAST(moNode);
        BufferedImage image = Graphviz.fromGraph(graph).render(Format.SVG).toImage();

        // 显示图像
        JFrame frame = new JFrame("Graph Visualization");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new JLabel(new ImageIcon(image)));
        frame.pack();
        frame.setVisible(true);
    }

    private Graph buildAST(MoNode moNode) {
        Graph graph = graph("AST").directed();
        Map<MoNode, Node> nodeMap = new HashMap<>();

        List<LinkSource> linkSources = new ArrayList<>();

        new DeepScanner() {
            @Override
            protected void enter(MoNode moNode) {
                Node node = buildNode(moNode);
                nodeMap.put(moNode, node);
            }
        }.scan(moNode);

        new DeepScanner() {
            @Override
            protected void enter(MoNode moNode) {
                if (moNode.getParent() != null) {
                    Node source = nodeMap.get(moNode.getParent());
                    Node target = nodeMap.get(moNode);
                    linkSources.add(source.link(to(target)).with(Style.BOLD));
                }
            }
        }.scan(moNode);

        graph = graph.with(linkSources);
        return graph;
    }

    private Node buildNode(MoNode moNode) {
        StringBuilder stringBuilder = new StringBuilder();
        String id = "id: " + moNode.getId();
        stringBuilder.append(id).append("\n")
                .append("type: ").append(moNode.getMoNodeType().name().substring(4));

        if (moNode instanceof MoExpression expression) {
            stringBuilder.append("\nexprType: ").append(expression.getExprTypeStr());
        }

        if (moNode instanceof MoName name) {
            stringBuilder.append("\nvalue: ").append(name.getIdentifier());
            return node(stringBuilder.toString()).with(Color.RED1);
        }

        return node(stringBuilder.toString());
    }


}
