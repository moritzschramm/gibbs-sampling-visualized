/*
 * GibbsSampling.java
 * Moritz Schramm, Moritz Andres, 2020 for the Animal project at TU Darmstadt.
 * Copying this file for educational purposes is permitted without further authorization.
 */
package generators.misc;

import algoanim.exceptions.NotEnoughNodesException;
import algoanim.primitives.*;
import algoanim.primitives.Point;
import algoanim.primitives.Polygon;
import algoanim.primitives.vhdl.AndGate;
import algoanim.primitives.vhdl.VHDLPin;
import generators.misc.BNSamplingHelper.*;
import algoanim.properties.*;
import generators.framework.Generator;
import generators.framework.GeneratorType;
import generators.framework.ValidatingGenerator;

import java.awt.*;
import java.util.*;
import java.util.List;

import algoanim.primitives.generators.Language;
import generators.framework.properties.AnimationPropertiesContainer;
import algoanim.animalscript.AnimalScript;
import algoanim.util.*;

import interactionsupport.models.*;
import translator.Translator;

public class GibbsSampling implements ValidatingGenerator {

    private Language lang;

    private Translator translator;
    private String resourceName;
    private Locale locale;

    private Random random;

    private Text header;

    private Code code;
    private BayesNet bn;
    private InformationDisplay info;

    // iteration number, increased when sample() is called
    private int iteration = 0;
    private int numberOfIterations = 10;

    // variables and their sample counts
    private String[] vars;
    private String[] sampleVars;
    private Hashtable<String, Integer> samples;
    private Hashtable<String, Double> normalizedSamples;

    // for questions
    private String WRONG_ASW;
    private String RIGHT_ASW;

    public GibbsSampling(String resourceName, Locale locale) {
        this.resourceName = resourceName;
        this.locale = locale;

        translator = new Translator(resourceName, locale);
    }

    public void init() {
        lang = new AnimalScript("Gibbs Sampling", "Moritz Schramm, Moritz Andres", 800, 600);
        lang.setStepMode(true);
        lang.setInteractionType(Language.INTERACTION_TYPE_AVINTERACTION);

        random = new Random();

        iteration = 0;

        samples = new Hashtable<>();
        normalizedSamples = new Hashtable<>();

        code = new Code(lang, translator);
        bn = new BayesNet(lang);
        info = new InformationDisplay(lang, bn, samples, normalizedSamples);


        RIGHT_ASW = translator.translateMessage("right_asw");
        WRONG_ASW = translator.translateMessage("wrong_asw");
    }

    /* methods used to create animation */
    public String generate(AnimationPropertiesContainer props, Hashtable<String, Object> primitives) {

        // init vars and sample arrays
        vars = (String []) primitives.get("Variables");
        sampleVars = (String []) primitives.get("Non-evidence variables");

        // set seed
        random.setSeed((int) primitives.get("Seed"));

        // set number of iterations
        numberOfIterations = (int) primitives.get("NumberOfSamples");

        // init graph, probabilities and values
        GraphProperties graphProps = (GraphProperties) props.getPropertiesByName("graphProps");
        bn.init(primitives, graphProps, vars, sampleVars);


        // header creation
        TextProperties headerProps = new TextProperties();
        headerProps.set(AnimationPropertiesKeys.FONT_PROPERTY, new Font(
                Font.SANS_SERIF, Font.BOLD | Font.ITALIC, 24));
        header = lang.newText(new Coordinates(20, 30), "Gibbs Sampling",
                "header", null, headerProps);

        lang.nextStep(translator.translateMessage("introTOC"));

        // show introduction text (creates new step)
        showIntro();

        // add source code (unhighlighted)
        SourceCodeProperties sourceCodeProps = new SourceCodeProperties();
        sourceCodeProps.set(AnimationPropertiesKeys.FONT_PROPERTY, new Font(
                Font.MONOSPACED, Font.PLAIN, 16));
        sourceCodeProps.set(AnimationPropertiesKeys.HIGHLIGHTCOLOR_PROPERTY, Color.RED);
        sourceCodeProps.set(AnimationPropertiesKeys.NAME, "sourceCode");
        code.init((SourceCodeProperties) props.getPropertiesByName("sourceCode"));
        //code.init(sourceCodeProps);
        code.add();

        // show additional information
        info.init(sampleVars, translator, primitives);
        info.add();

        // graph creation
        bn.add();

        iteration++;
        info.updateInformation(iteration);
        code.highlight(0);

        MultipleChoiceQuestionModel question1 = new MultipleChoiceQuestionModel("q1");
        String feedback_q1 = translator.translateMessage("q1_fb");
        question1.setPrompt(translator.translateMessage("q1_text"));
        question1.addAnswer(translator.translateMessage("q1_asw1"), 0, WRONG_ASW + feedback_q1);
        question1.addAnswer(translator.translateMessage("q1_asw2"), 0, WRONG_ASW + feedback_q1);
        question1.addAnswer(translator.translateMessage("q1_asw3"), 1, RIGHT_ASW + feedback_q1);
        lang.addMCQuestion(question1);

        lang.nextStep("1. Iteration");

        sample();

        for(int i = 0; i < numberOfIterations - 1; i++) {

            code.highlight(0);
            iteration++;
            info.updateInformation(iteration);
            lang.nextStep(iteration + ". Iteration");
            sample();
        }

        code.highlight(6);

        lang.nextStep();

        code.unhighlight(6);
        code.highlight(7);

        lang.nextStep(translator.translateMessage("outroTOC"));

        showOutro();


        lang.finalizeGeneration();

        return lang.toString();
    }

    private void showIntro() {

        TextProperties props = new TextProperties();
        props.set(AnimationPropertiesKeys.FONT_PROPERTY, new Font(
                Font.SANS_SERIF, Font.PLAIN, 16));

        String text = translator.translateMessage("intro");

        final int lineBreakSize = 16 + 3;
        String[] parts = text.split("\n");
        Text[] intro_ts = new Text[parts.length];

        int lineCounter = 0;
        for(String textPart : parts){
            int yOffset = lineBreakSize * lineCounter;
            Text intro = lang.newText(new Coordinates(20, 70 + yOffset), textPart, null, null, props);
            intro_ts[lineCounter] = intro;
            lineCounter++;
        }

        lang.nextStep();

        for(Text intro : intro_ts)
            intro.hide();
    }

    private void showOutro() {

        lang.hideAllPrimitives();
        header.show();

        TextProperties props = new TextProperties();
        props.set(AnimationPropertiesKeys.FONT_PROPERTY, new Font(
                Font.SANS_SERIF, Font.PLAIN, 16));

        String text = translator.translateMessage("outro");


        final int lineBreakSize = 16 + 3;  // font size + gap
        String[] parts = text.split("\n");
        Text[] outro_ts = new Text[parts.length];

        int lineCounter = 0;
        for(String textPart : parts){
            int yOffset = lineBreakSize * lineCounter;
            Text outro = lang.newText(new Coordinates(20, 70 + yOffset), textPart, "outroline"+lineCounter, null, props);
            outro_ts[lineCounter] = outro;
            lineCounter++;
        }

        Text iterationDisplay = lang.newText(new Offset(0, 30, "outroline"+(lineCounter-1), AnimalScript.DIRECTION_NW), "Iteration: "+iteration,
                "iterationDisplayOutro", null, props);

        Text propTrueDisplay = lang.newText(new Offset(0, 30, "iterationDisplayOutro", AnimalScript.DIRECTION_NW),
                info.getSampleCount("Sample (true, false) " + translator.translateMessage("of") + " "),
                "propTrueDisplayOutro", null, props);

        Text propFalseDisplay = lang.newText(new Offset(0, 30, "propTrueDisplayOutro", AnimalScript.DIRECTION_NW),
                info.getNormalizedSampleCount(translator.translateMessage("normValue")+" (true, false) " + translator.translateMessage("of") + " "),
                "propFalseDisplayOutro", null, props);

        lang.nextStep();
    }



    /* algorithm */
    private void sample() {

        for(String var: sampleVars) {

            code.unhighlight(0);
            code.unhighlight(6);
            code.highlight(1);

            bn.highlightNode(var, BayesNet.HIGHLIGHT_COLOR);
            info.updateVars(var, null, null, null);

            lang.nextStep();

            double p = bn.probabilities.get(bn.key(var, bn.parents(var)));

            info.updateVars(var, null, p, null);

            code.unhighlight(1);
            code.highlight(2);

            lang.nextStep();

            for(String child: bn.children(var)) {

                code.unhighlight(2);
                code.unhighlight(4);
                code.highlight(3);

                bn.highlightNode(child, BayesNet.SELECT_COLOR);

                info.updateVars(var, child, p, -1.0);

                lang.nextStep();

                double prob = bn.probabilities.get(bn.key(child, bn.parents(child)));

                p *= prob;

                info.updateVars(var, child, p, prob);

                code.unhighlight(3);
                code.highlight(4);

                bn.highlightNode(child, bn.values.get(child) ? BayesNet.TRUE_COLOR : BayesNet.FALSE_COLOR);

                lang.nextStep();
            }

            code.unhighlight(4);
            code.highlight(5);

            boolean value = createSampleValue(p);

            bn.values.put(var, value);

            bn.highlightNode(var, value ? Color.GREEN : Color.RED);

            lang.nextStep();

            code.unhighlight(5);
            code.highlight(6);


            increaseSampleCount(var, value);

            info.updateInformation(iteration);

            lang.nextStep();

            code.unhighlight(6);
        }

        info.updateVars(null, null, null, null);
    }

    private boolean createSampleValue(double p) {

        return random.nextDouble() <= p;
    }

    private void increaseSampleCount(String var, boolean value) {

        String key = var + (value ? "=true" : "=false");
        samples.put(key, (samples.get(key) == null ? 0 : samples.get(key)) + 1);

        double trueVal = samples.get(var+"=true") == null ? 0 : samples.get(var+"=true");
        double falseVal = samples.get(var+"=false") == null ? 0 : samples.get(var+"=false");
        double sum = trueVal + falseVal;
        sum = sum == 0 ? 1 : sum;
        normalizedSamples.put(var+"=true", trueVal / sum);
        normalizedSamples.put(var+"=false", falseVal / sum);
    }



    /* Interface methods */
    public String getName() {
        return "Gibbs Sampling";
    }

    public String getAlgorithmName() {
        return "Gibbs Sampling";
    }

    public String getAnimationAuthor() {
        return "Moritz Schramm, Moritz Andres";
    }

    public String getDescription() {
        return translator.translateMessage("description");
    }

    public String getCodeExample(){
        return "for i = 1 to NumberOfSamples:"
                +"\n"
                +"    for each Var in NonevidenceVars:"
                +"\n"
                +"        p = P( Var | parents(Var) )"
                +"\n"
                +"        for each ChildVar in children(Var):"
                +"\n"
                +"            p = p * P( ChildVar | parents(ChildVar) )"
                +"\n"
                +"        sampleValue = createValueGivenProbability(p)"
                +"\n"
                +"        increaseSampleCount(Var, sampleValue)"
                +"\n"
                +"\n"
                +"return normalize(Samples)";
    }

    public String getFileExtension(){
        return "asu";
    }

    public Locale getContentLocale() {
        return locale;
    }

    public GeneratorType getGeneratorType() {
        return new GeneratorType(GeneratorType.GENERATOR_TYPE_MORE);
    }

    public String getOutputLanguage() {
        return Generator.PSEUDO_CODE_OUTPUT;
    }

    public boolean validateInput(AnimationPropertiesContainer props, Hashtable<String, Object> primitives) {

        int seed = (int) primitives.get("Seed");

        int numberOfIterations = (int) primitives.get("NumberOfSamples");

        if(seed <= 0)
            throw new IllegalArgumentException("Seed must be greater than 0.");
        if(numberOfIterations <= 0)
            throw new IllegalArgumentException("NumberOfSamples must be greater than 0.");

        String[] vars = (String []) primitives.get("Variables");
        String[] sampleVars = (String []) primitives.get("Non-evidence variables");
        String[] values = (String []) primitives.get("Values");

        if(vars.length > 4)
            throw new IllegalArgumentException("Length of Variables must not be greater than 4.");

        if(sampleVars.length > vars.length)
            throw new IllegalArgumentException("Length of Non-evidence variables cannot be greater than length of Variables");

        if(sampleVars.length < 1)
            throw new IllegalArgumentException("There must be at least one Non-evidence variable");

        if(vars.length - sampleVars.length != values.length)
            throw new IllegalArgumentException("Array 'Values' has an invalid form");

        int[][] p = (int[][]) primitives.get("Probabilities");

        for(int i = 0; i < p.length; i++) {
            for(int j = 0; j < p[i].length; j++) {
                if(p[i][j] < 0 || p[i][j] > 100)
                    throw new IllegalArgumentException("Probabilities table has invalid entries (< 0 or > 100).");
            }
        }

        BayesNet bn;
        try {
            bn = new BayesNet(new AnimalScript("Gibbs Sampling", "Moritz Schramm, Moritz Andres", 800, 600));
            bn.init(primitives, null, vars, sampleVars);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Probability table for given graph");
        }

        if(bn.graph.getNodes().length != vars.length)
            throw new IllegalArgumentException("Variables do not match graph nodes");

        for(int i = 0; i < bn.graph.getSize(); i++) {
            String label = bn.graph.getNodeLabel(i);
            if( ! Arrays.asList(vars).contains(label))
                throw new IllegalArgumentException("Invalid Variables for given graph");
        }

        for(String var: sampleVars) {
            if( ! Arrays.asList(vars).contains(var))
                throw new IllegalArgumentException("Invalid Sample Variables");
        }

        for(String var: vars) {
            int parents = bn.parents(var).length;
            int children = bn.children(var).length;

            if(parents == 0 && children == 0)
                throw new IllegalArgumentException("Invalid graph");
        }

        return true;
    }

    public static void main(String[] args) {

        Generator generator = new GibbsSampling("resources/gibbssampling", Locale.GERMANY);
        generator.init();

        if(args[0].equals("generator")) {

            animal.main.Animal.startGeneratorWindow(generator);

        } else if (args[0].equals("animation")) {

            Hashtable<String, Object> primitives = new Hashtable<>();
            AnimationPropertiesContainer props = new AnimationPropertiesContainer();

            primitives.put("Seed", 1234);
            primitives.put("NumberOfSamples", 10);
            primitives.put("Variables", new String[]{"Y", "A", "X", "B"});
            primitives.put("Non-evidence variables", new String[]{"Y", "X"});
            primitives.put("Values", new String[]{"true", "false"});

            GraphProperties graphProps = new GraphProperties();
            graphProps.set(AnimationPropertiesKeys.NAME, "graphProps");
            graphProps.set(AnimationPropertiesKeys.DIRECTED_PROPERTY, true);
            graphProps.set(AnimationPropertiesKeys.FILLED_PROPERTY, false);
            graphProps.set(AnimationPropertiesKeys.FILL_PROPERTY, Color.WHITE);
            graphProps.set(AnimationPropertiesKeys.EDGECOLOR_PROPERTY, Color.BLACK);
            graphProps.set(AnimationPropertiesKeys.ELEMHIGHLIGHT_PROPERTY, Color.BLACK);
            graphProps.set(AnimationPropertiesKeys.HIGHLIGHTCOLOR_PROPERTY, Color.GREEN);
            graphProps.set(AnimationPropertiesKeys.NODECOLOR_PROPERTY, Color.BLACK);
            graphProps.set(AnimationPropertiesKeys.WEIGHTED_PROPERTY, false);

            int[][] adjacencyMatrix = new int[4][4];
            for(int i = 0; i < adjacencyMatrix.length; i++)
                for(int j = 0; j < adjacencyMatrix.length; j++)
                    adjacencyMatrix[i][j] = 0;

            adjacencyMatrix[0][1] = 1;
            adjacencyMatrix[0][2] = 1;
            adjacencyMatrix[1][3] = 1;
            adjacencyMatrix[2][3] = 1;

            Node[] nodes = new Node[4];
            int offsetX = 600; int offsetY = 180;
            nodes[0] = new Coordinates(offsetX+150, offsetY+100);
            nodes[1] = new Coordinates(offsetX+50, offsetY+150);
            nodes[2] = new Coordinates(offsetX+250, offsetY+150);
            nodes[3] = new Coordinates(offsetX+150, offsetY+200);

            Language lang = new AnimalScript("Gibbs Sampling", "Moritz Schramm, Moritz Andres", 800, 600);
            Graph graph = lang.newGraph("bn", adjacencyMatrix, nodes, new String[]{"Y", "A", "X", "B"}, null, graphProps);

            primitives.put("graph", graph);

            int[][] p = new int[4][4];

            p[0][0] = 30;   // P(Y)
            p[0][1] = 10;   // P(A|Y=true)
            p[1][1] = 20;   // P(A|Y=false)
            p[0][2] = 40;   // P(X|Y=true)
            p[1][2] = 70;   // P(X|Y=false)
            p[0][3] = 90;   // P(B | A=true, X=true)
            p[1][3] = 99;   // P(B | A=true, X=false)
            p[2][3] = 30;   // P(B | A=false, X=true)
            p[3][3] = 60;   // P(B | A=false, X=false)

            primitives.put("Probabilities", p);

            primitives.put("Highlight Color", Color.GRAY);
            primitives.put("Select Color", Color.LIGHT_GRAY);
            primitives.put("True Color", Color.GREEN);
            primitives.put("False Color", Color.RED);

            SourceCodeProperties sourceCodeProps = new SourceCodeProperties();
            sourceCodeProps.set(AnimationPropertiesKeys.FONT_PROPERTY, new Font(
                    Font.MONOSPACED, Font.PLAIN, 16));
            sourceCodeProps.set(AnimationPropertiesKeys.HIGHLIGHTCOLOR_PROPERTY, Color.RED);
            sourceCodeProps.set(AnimationPropertiesKeys.NAME, "sourceCode");

            props.add(sourceCodeProps);

            System.out.println(generator.generate(props, primitives));
        }
    }
}
