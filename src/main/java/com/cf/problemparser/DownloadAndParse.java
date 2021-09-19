package com.cf.problemparser;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class DownloadAndParse extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        System.out.println("Base Path: " + project.getBasePath());
        System.out.println("Project File Path: " + project.getProjectFilePath());
        System.out.println("Project File: " + project.getProjectFile());
        System.out.println("Name: " + project.getName());
        System.out.println("Location Hash: " + project.getLocationHash());
        System.out.println("Presentable Url: " + project.getPresentableUrl());

        String url = Messages.showInputDialog("Enter URL", "Download CF Problem", null);
        try {
            Problem problem = parseProblem(url);
            System.out.println(problem);
            System.out.println("Creating module");
            Application application = ApplicationManager.getApplication();
            application.runWriteAction(new Runnable() {
                @Override
                public void run() {
                    try {
                        createModule(project, problem);
                    }
                    catch ( Exception ex ) {
                        ex.printStackTrace();
                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void createModule(Project project, Problem problem) throws Exception
    {
        ModuleManager moduleManager = ModuleManager.getInstance(project);

        String moduleDir = project.getBasePath() + "/" + problem.getModuleName();
        String moduleLocation = moduleDir + "/" + problem.getModuleName() + ".iml";
        String moduleType = ModuleTypeId.JAVA_MODULE;
        com.intellij.openapi.module.Module module = moduleManager.newModule(moduleLocation, moduleType);

        String contentRoot = VfsUtil.pathToUrl(moduleDir);
        ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
        ContentEntry entry = rootModel.addContentEntry(contentRoot);
        Path srcDir = createMainFile(moduleDir);
        entry.addSourceFolder(VfsUtil.pathToUrl(srcDir.toString()), JavaSourceRootType.SOURCE);

        Sdk[] sdks = ProjectJdkTable.getInstance().getAllJdks();
        Sdk sdk = sdks[sdks.length-1];
        rootModel.setSdk(sdk);

        rootModel.getModuleExtension(LanguageLevelModuleExtension.class).setLanguageLevel(LanguageLevel.JDK_1_8);
        rootModel.commit();

        ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
        projectRootManager.setProjectSdk(sdk);

        project.save();

        openFile(project, srcDir + "/Main.java");
        createTestFiles(moduleDir, problem.getInputs(), problem.getOutputs());
    }

    public static void openFile(@NotNull Project project, @NotNull String mainFile) {
        VirtualFile file = VfsUtil.findFile(Paths.get(mainFile), true);
        FileEditorManager.getInstance(project).openFile(file, true);
    }

    public Path createMainFile(String moduleDir) throws Exception {
        Path srcDir = Paths.get(moduleDir, "src");
        Path mainClass = Paths.get(srcDir.toString(), "Main.java");
        Files.createDirectories(srcDir);
        Files.writeString(mainClass, getMainText());
        return srcDir;
    }

    public void createTestFiles(String moduleDir, String[] inputs, String[] outputs) throws Exception {
        Path testDir = Paths.get(moduleDir, "tests");
        Files.createDirectories(testDir);

        for(int i=0; i<inputs.length; ++i) {
            String inputFilename = "input-" + i + ".in";
            String outputFilename = "output-" + i + ".out";
            createTestFile(testDir, inputFilename, inputs[i]);
            createTestFile(testDir, outputFilename, outputs[i]);
        }
    }

    public Path createTestFile(Path testDir, String name, String text) throws Exception {
        Path testFile = Paths.get(testDir.toString(), name);
        Files.writeString(testFile, text);
        return testFile;
    }

    public String getMainText() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("/Main.template");
        StringBuilder builder = new StringBuilder();
        for(int ch; (ch = inputStream.read()) != -1; ) {
            builder.append((char)ch);
        }
        return builder.toString();
    }

    public static Problem parseProblem(String url) throws IOException {
        Problem problem = new Problem();
        org.jsoup.nodes.Document doc = Jsoup.connect(url).get();
        System.out.println(doc.title());

        problem.setProblemNo(getProblemNo(url));
        problem.setProblemTitle(cleanTitle(text(doc, "#pageContent .problem-statement .header .title")));
        problem.setInputs(getAllTexts(doc, "#pageContent .problem-statement .sample-test .input pre"));
        problem.setOutputs(getAllTexts(doc, "#pageContent .problem-statement .sample-test .output pre"));
        problem.setNoOfTests(problem.getInputs().length);

        return problem;
    }

    public static String getProblemNo(String url) {
        String[] parts = url.split("/");
        return parts[parts.length-2];
    }

    public static String cleanTitle(String title) {
        title = title.replace('.', '-');
        return title.replaceAll(" ", "");
    }

    public static String text(Document doc, String cssQuery) {
        Element ele = doc.select(cssQuery).get(0);
        return ele.text();
    }

    public static String[] getAllTexts(Document doc, String cssQuery) {
        ArrayList<String> texts = new ArrayList<>();
        Elements elements = doc.select(cssQuery);
        for(Element element: elements) {
            String text = element.html().replaceAll("<br>", "\n").trim();
            texts.add(text);
        }

        return texts.toArray(new String[texts.size()]);
    }
}
