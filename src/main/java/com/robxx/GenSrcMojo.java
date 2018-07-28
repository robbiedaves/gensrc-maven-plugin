package com.robxx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.squareup.javapoet.*;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "gensrc", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenSrcMojo extends AbstractMojo {

    @Parameter(property = "msg",defaultValue = "message")
    private String msg;

    /**
     * Project the plugin is called from.
     */
    @Parameter(property = "project", defaultValue = "${project}", required = true)
    protected MavenProject project;

    @Parameter(property = "modelDirectory", defaultValue = "model", required = true)
    protected String modelDirectory;

    /**
     * Represents the base package used for generated java classes.
     */
    @Parameter(property = "basePackage", defaultValue = "com.robxx", required = true)
    protected String basePackage;

    /**
     * Directory to write generated code to.
     */
    @Parameter(property = "outputDirectory")
    protected File outputDirectory;

    private List<File> allDataFiles;

    private List<Model> models = new ArrayList<>();

    public GenSrcMojo() {

    }

    public void execute()
            throws MojoExecutionException {


        project.addCompileSourceRoot(generatedSourcesDir());

        getLog().info("msg: " + msg);

        // Get all the files in the resource folder
        List<Resource> res = project.getBuild().getResources();
        String resource = "";
        if (res != null && res.size() >= 1) {
            resource =  res.get(0).getDirectory();
        }
        resource += "/" + modelDirectory;
        this.allDataFiles = getResourceFiles(new File(resource));

        for (File f : this.allDataFiles) {
            getLog().info("File = " + f.getPath() + "     " + f.getName());
        }

        for (File f : this.allDataFiles) {
            this.models.add(this.loadModel(f));
        }

        generate();
    }

    public String getMsg() {
        return msg;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }


    String generatedSourcesDir() {
        return (outputDirectory != null) ? outputDirectory.getAbsolutePath() :
                project.getBuild().getDirectory() + File.separator + "generated-sources";
    }

    private Model loadModel(File modelFile) {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Model model = null;
        try {
            //Model model = mapper.readValue(new File("user.yaml"), Model.class);
            model = mapper.readValue(modelFile, Model.class);
            System.out.println(ReflectionToStringBuilder.toString(model, ToStringStyle.MULTI_LINE_STYLE));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return model;
    }

    private List<File> getResourceFiles(File workingDir) {
        List<File> dataFile = new ArrayList<File>();
        if (workingDir.exists()) {
            File[] files = workingDir.listFiles();
            for (File eachFile : files) {
                if (eachFile.isDirectory()) {
                    dataFile.addAll(getResourceFiles(eachFile));
                } else if (eachFile.getName().endsWith(".yaml") || eachFile.getName().endsWith(".yml")) {
                    dataFile.add(eachFile);
                }
            }
        }
        return dataFile;
    }

    private void generate() {
        generateSourceForModel(this.models.get(0));
       // for (Model m : this.models) {
       //     this.generateSourceForModel(m);
       // }
    }

    private void generateSourceForModel(Model model){
        CodeBlock sumOfTenImpl = CodeBlock
                .builder()
                .addStatement("int sum = 0")
                .beginControlFlow("for (int i = 0; i <= 10; i++)")
                .addStatement("sum += i")
                .endControlFlow()
                .build();

        MethodSpec sumOfTen = MethodSpec
                .methodBuilder("sumOfTen")
                .addCode(sumOfTenImpl)
                .build();

        FieldSpec name = FieldSpec
                .builder(String.class, "name")
                .addModifiers(Modifier.PRIVATE)
                .build();

        TypeSpec person = TypeSpec
                .classBuilder("Person")
                .addModifiers(Modifier.PUBLIC)
                .addField(name)
                .addMethod(MethodSpec
                        .methodBuilder("getName")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(String.class)
                        .addStatement("return this.name")
                        .build())
                .addMethod(MethodSpec
                        .methodBuilder("setName")
                        .addParameter(String.class, "name")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeName.VOID)
                        .addStatement("this.name = name")
                        .build())
                .addMethod(sumOfTen)
                .build();

        JavaFile javaFile = JavaFile
                .builder(basePackage, person)
                .indent("    ")
                .build();

        try {
            javaFile.writeTo(System.out);

        } catch (IOException e) {
            e.printStackTrace();
        }

        Path path = Paths.get(generatedSourcesDir());
        try {
            javaFile.writeTo(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
