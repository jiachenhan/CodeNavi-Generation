package repair.apply.det;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.apply.match.MatchInstance;
import repair.apply.match.Matcher;
import repair.ast.declaration.MoMethodDeclaration;
import repair.ast.parser.NodeParser;
import repair.common.MethodSignature;
import repair.pattern.Pattern;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


import static org.mozilla.universalchardet.UniversalDetector.detectCharset;
import static repair.FileUtils.ensureDirectoryExists;
import static repair.common.JDTUtils.genAST;

public class Detector {
    private static final Logger logger = LoggerFactory.getLogger(Detector.class);
    private final Pattern pattern;
    private final Path repoPath;
    private final String commitID;
    private final String fileOracle;
    private final MethodSignature signatureOracle;
    private final List<DetectResult> results;

    public Detector(Pattern pattern, Path repoPath, String commitID, String fileOracle, String signatureOracle) {
        this.pattern = pattern;
        this.repoPath = repoPath;
        this.commitID = commitID;
        this.fileOracle = fileOracle;
        this.signatureOracle = MethodSignature.parseFunctionSignature(signatureOracle);
        this.results = new ArrayList<>();
    }

    /**
     *  Traverse the corresponding commit files based on jgit to avoid the impact of checkouts on concurrency
     */
    public void detect() {
        if(signatureOracle == null) {
            logger.error("Failed to parse signature");
            return;
        }
        try (Repository repository = FileRepositoryBuilder.create(repoPath.resolve(".git").toFile())) {
            Git git = new Git(repository);

            // 获取 commit 对象
            ObjectId commitObjectId = repository.resolve(this.commitID);
            if (commitObjectId == null) {
                logger.error("Failed to resolve commit: " + this.commitID);
                return;
            }

            // 使用 RevWalk 解析 commit
            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(commitObjectId);
                RevTree tree = commit.getTree();

                // 使用 TreeWalk 遍历文件树
                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(true);

                    while (treeWalk.next()) {
                        String filePath = treeWalk.getPathString();
                        if (!filePath.endsWith(".java")) {
                            continue;
                        }
                        ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
                        // 读取文件内容
                        byte[] fileData = loader.getBytes();
                        try (InputStream is = new ByteArrayInputStream(fileData)) {
                            String encoding = detectCharset(is);
                            // 如果编码为null，使用默认的UTF-8编码
                            if (encoding == null || encoding.isEmpty()) {
                                logger.warn("Failed to detect encoding for file: " + filePath + ". Defaulting to UTF-8.");
                                encoding = "UTF-8";  // 设置默认编码为UTF-8
                            }
                            String code = new String(fileData, Charset.forName(encoding));
                            detectFile(code, Path.of(filePath));
                        } catch (IOException e) {
                            logger.error("Failed to read file", e);
                            continue;
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to apply pattern", e);
        }
    }

    /**
     * 检测文件中是否存在可以应用pattern的地方
     * @param code 文件内容
     * @param codePath 文件路径（相对于git的路径，并且路径基于当时的commit）
     */
    public void detectFile(String code, Path codePath) {
        CompilationUnit beforeCompilationUnit = genAST(code, codePath);
        if (beforeCompilationUnit == null) {
            logger.error("Failed to generate AST for file: " + codePath);
            return;
        }

        List<MethodDeclaration> declarations = new ArrayList<>();
        beforeCompilationUnit.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                declarations.add(node);
                return false;
            }
        });

        declarations.forEach(decl -> {
            NodeParser beforeParser = new NodeParser(codePath, beforeCompilationUnit);
            MoMethodDeclaration moMethodDeclaration = (MoMethodDeclaration) beforeParser.process(decl);
            boolean detected = detectMethod(moMethodDeclaration);
            if(detected) {
                MethodSignature signature = MethodSignature.parseFunctionSignature(moMethodDeclaration.toString());
                DetectResult result = new DetectResult(codePath.toString(), signature);
                results.add(result);
            }
        });
    }

    public boolean detectMethod(MoMethodDeclaration moMethodDeclaration) {
        List<MatchInstance> matchInstances = Matcher.match(pattern, moMethodDeclaration).stream()
                .limit(5).toList();

        return matchInstances.stream()
                .anyMatch(MatchInstance::isLegal);
    }

    private boolean checkCorrectSignature() {
        return results.stream().anyMatch(result -> {
            return signatureOracle.equals(result.methodSignature()) &&
                    fileOracle.equals(result.filePath());
        });
    }

    public void serializeResults(Path outputPath) {
        ensureDirectoryExists(outputPath);
        JsonFactory jsonFactory = new JsonFactory();
        try (JsonGenerator jsonGenerator = jsonFactory.createGenerator(outputPath.toFile(), JsonEncoding.UTF8)) {
            jsonGenerator.writeStartObject();

            jsonGenerator.writeBooleanField("detected", checkCorrectSignature());

            jsonGenerator.writeFieldName("results");
            jsonGenerator.writeStartArray();
            for (DetectResult result : results) {

                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("file", result.filePath());
                jsonGenerator.writeStringField("signature", result.methodSignature().toString());
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();

            jsonGenerator.writeEndObject();
        } catch (IOException e) {
            logger.error("Failed to serialize results", e);
        }
    }

}
