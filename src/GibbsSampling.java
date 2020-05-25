/*
 * GibbsSampling.java
 * Moritz Schramm, 2020 for the Animal project at TU Darmstadt.
 * Copying this file for educational purposes is permitted without further authorization.
 */
//FIXME package generators.misc;

import algoanim.primitives.Graph;
import algoanim.primitives.SourceCode;
import algoanim.primitives.Text;
import algoanim.properties.AnimationPropertiesKeys;
import algoanim.properties.GraphProperties;
import algoanim.properties.SourceCodeProperties;
import algoanim.properties.TextProperties;
import generators.framework.Generator;
import generators.framework.GeneratorType;
import generators.framework.ValidatingGenerator;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import algoanim.primitives.generators.Language;
import generators.framework.properties.AnimationPropertiesContainer;
import algoanim.animalscript.AnimalScript;
import algoanim.util.*;

public class GibbsSampling implements ValidatingGenerator {

    private Language lang;

    private Random random;

    private Graph g;
    private Text header;
    private Text information;
    private SourceCode sc;
    private SourceCode exp;

    public final int INDENTATION_WIDTH = 2;

    // iteration number, increased when sample() is called
    private int iteration = 0;

    // hashtables, containing probabilities and values of random variables
    private Hashtable<String, Double> probabilities;
    private Hashtable<String, Boolean> values;

    // keys for hashtable 'values'
    private final String A = "A";
    private final String B = "B";
    private final String X = "X";
    private final String Y = "Y";

    // contains sum of sample values (sampleX[0]: samples when X is false, sampleX[1]: samples when X is true)
    private int[] samplesX;
    private int[] samplesY;
    private double[] normalizedSamplesX;
    private double[] normalizedSamplesY;

    public void init() {
        lang = new AnimalScript("Gibbs Sampling", "Moritz Schramm", 800, 600);
        lang.setStepMode(true);

        random = new Random();

        probabilities = new Hashtable<>();
        values = new Hashtable<>();
        values.put(A, false);
        values.put(B, false);
        values.put(X, false);
        values.put(Y, false);
        samplesX = new int[2];
        samplesY = new int[2];
        normalizedSamplesX = new double[2];
        normalizedSamplesY = new double[2];

    }

    public String generate(AnimationPropertiesContainer props, Hashtable<String, Object> primitives) {

        // set seed
        random.setSeed((int) primitives.get("Seed"));

        // init probabilities
        probabilities.put("P(Y)", (double) primitives.get("P(Y)"));
        probabilities.put("P(X | Y=true)", (double) primitives.get("P(X | Y=true)"));
        probabilities.put("P(X | Y=false)", (double) primitives.get("P(X | Y=false)"));
        probabilities.put("P(A | Y=true)", (double) primitives.get("P(A | Y=true)"));
        probabilities.put("P(A | Y=false)", (double) primitives.get("P(A | Y=false)"));
        probabilities.put("P(B | A=true, X=true)", (double) primitives.get("P(B | A=true, X=true)"));
        probabilities.put("P(B | A=true, X=false)", (double) primitives.get("P(B | A=true, X=false)"));
        probabilities.put("P(B | A=false, X=true)", (double) primitives.get("P(B | A=false, X=true)"));
        probabilities.put("P(B | A=false, X=false)", (double) primitives.get("P(B | A=false, X=false)"));

        // init values
        values.put(A, (boolean) primitives.get(A));
        values.put(B, (boolean) primitives.get(B));


        // init graph
        int[][] adjacencyMatrix = new int[4][4];
        for(int i = 0; i < adjacencyMatrix.length; i++)
            for(int j = 0; j < adjacencyMatrix.length; j++)
                adjacencyMatrix[i][j] = 0;

        adjacencyMatrix[0][1] = 1;
        adjacencyMatrix[0][2] = 1;
        adjacencyMatrix[1][3] = 1;
        adjacencyMatrix[2][3] = 1;

        Node[] nodes = new Node[4];
        nodes[0] = new Coordinates(150, 100);
        nodes[1] = new Coordinates(50, 150);
        nodes[2] = new Coordinates(250, 150);
        nodes[3] = new Coordinates(150, 200);

        GraphProperties graphProps = new GraphProperties();
        graphProps.set(AnimationPropertiesKeys.DIRECTED_PROPERTY, true);
        graphProps.set(AnimationPropertiesKeys.FILLED_PROPERTY, false);
        graphProps.set(AnimationPropertiesKeys.FILL_PROPERTY, Color.WHITE);
        graphProps.set(AnimationPropertiesKeys.EDGECOLOR_PROPERTY, Color.BLACK);
        graphProps.set(AnimationPropertiesKeys.ELEMHIGHLIGHT_PROPERTY, Color.BLACK);
        graphProps.set(AnimationPropertiesKeys.HIGHLIGHTCOLOR_PROPERTY, Color.GREEN);
        graphProps.set(AnimationPropertiesKeys.NODECOLOR_PROPERTY, Color.BLACK);
        graphProps.set(AnimationPropertiesKeys.WEIGHTED_PROPERTY, false);


        // header creation
        TextProperties headerProps = new TextProperties();
        headerProps.set(AnimationPropertiesKeys.FONT_PROPERTY, new Font(
                Font.SANS_SERIF, Font.BOLD, 24));
        header = lang.newText(new Coordinates(20, 30), "Gibbs Sampling",
                "header", null, headerProps);

        // show introduction text (creates new step)
        showIntro();

        // graph creation
        g = lang.newGraph("bn", adjacencyMatrix, nodes, new String[]{Y, A, X, B}, null, graphProps);

        // show additional information
        addInformation();

        // highlight evidence vars (value won't be changing, highlight color will stay the same)
        highlightNode(A, values.get(A) ? Color.GREEN : Color.RED);
        highlightNode(B, values.get(B) ? Color.GREEN : Color.RED);

        // add source code (unhighlighted)
        addSourceCode();

        lang.nextStep();

        highlightStep(0);


        lang.nextStep();

        sample();

        for(int i = 0; i < 10; i++) {


            highlightStep(0);
            lang.nextStep();
            sample();
        }

        unhighlightStep(6);
        highlightStep(7);


        showOutro();


        return lang.toString();
    }

    private Text iterationDisplay;
    Text sampleXDisplay;
    private Text sampleYDisplay;
    private Text normalizedSampleXDisplay;
    private Text normalizedSampleYDisplay;
    private Text varDisplay;
    private Text childVarDisplay;
    private Text probabilityDisplay;
    private void addInformation() {

        TextProperties props = new TextProperties();
        props.set(AnimationPropertiesKeys.FONT_PROPERTY, new Font(
                Font.SANS_SERIF, Font.PLAIN, 16));

        iterationDisplay = lang.newText(new Coordinates(350, 70), "Iteration: " + iteration, "iterationDisplay", null, props);
        sampleXDisplay = lang.newText(new Offset(0, 25, "iterationDisplay", AnimalScript.DIRECTION_NW), "Sample X: " + samplesX[1], "sampleXDisplay", null, props);
        sampleYDisplay = lang.newText(new Offset(0, 25, "sampleXDisplay", AnimalScript.DIRECTION_NW), "Sample Y: " + samplesY[1], "sampleYDisplay", null, props);

        normalizedSampleXDisplay =
                lang.newText(new Offset(0, 25, "sampleYDisplay", AnimalScript.DIRECTION_NW), "Normalisierter X Wert: " + normalizedSamplesX[1], "normalizedSampleXDisplay", null, props);
        normalizedSampleYDisplay =
                lang.newText(new Offset(0, 25, "normalizedSampleXDisplay", AnimalScript.DIRECTION_NW), "Normalisierter Y Wert: " + normalizedSamplesY[1], "normalizedSampleYDisplay", null, props);

        varDisplay = lang.newText(new Offset(0, 50, "normalizedSampleYDisplay", AnimalScript.DIRECTION_NW), "", "varDisplay", null, props);
        probabilityDisplay = lang.newText(new Offset(0, 25, "varDisplay", AnimalScript.DIRECTION_NW), "", "probabilityDisplay", null, props);
        childVarDisplay = lang.newText(new Offset(0, 25, "probabilityDisplay", AnimalScript.DIRECTION_NW), "", "childVarDisplay", null, props);
    }

    private void updateInformation() {

        DecimalFormat df = new DecimalFormat("0.0##", new DecimalFormatSymbols(Locale.ENGLISH));

        iterationDisplay.setText("Iteration: " + iteration, null, null);
        sampleXDisplay.setText("Sample X: " + samplesX[1], null, null);
        sampleYDisplay.setText("Sample Y: " + samplesY[1], null, null);
        normalizedSampleXDisplay.setText("Normalisierter X Wert: "+ df.format(normalizedSamplesX[1]), null, null);
        normalizedSampleYDisplay.setText("Normalisierter Y Wert: "+ df.format(normalizedSamplesY[1]), null, null);
    }

    private void updateVars(String var, String childVar, Double probability) {

        DecimalFormat df = new DecimalFormat("0.0###", new DecimalFormatSymbols(Locale.ENGLISH));

        if(var != null ) varDisplay.setText("Var = " + var, null, null);
        else varDisplay.setText("", null, null);

        if(childVar != null) childVarDisplay.setText("ChildVar = " + childVar, null, null);
        else childVarDisplay.setText("", null, null);

        if(probability != null) probabilityDisplay.setText("p = " + df.format(probability), null, null);
        else probabilityDisplay.setText("", null, null);
    }

    private void showIntro() {

        TextProperties props = new TextProperties();
        props.set(AnimationPropertiesKeys.FONT_PROPERTY, new Font(
                Font.SANS_SERIF, Font.PLAIN, 16));

        String text = getDescription();

        Text intro = lang.newText(new Coordinates(20, 80), text, null, null, props);

        lang.nextStep();

        intro.hide();
    }

    private void showOutro() {

        lang.hideAllPrimitives();
        header.show();

        TextProperties props = new TextProperties();
        props.set(AnimationPropertiesKeys.FONT_PROPERTY, new Font(
                Font.SANS_SERIF, Font.PLAIN, 16));

        String text = "outro";       // TODO add summary (StringBuilder,..)

        Text outro = lang.newText(new Coordinates(20, 80), text, null, null, props);

        lang.nextStep();
    }

    private void addSourceCode() {

        SourceCodeProperties sourceCodeProps = new SourceCodeProperties();
        sourceCodeProps.set(AnimationPropertiesKeys.FONT_PROPERTY, new Font(
                Font.MONOSPACED, Font.PLAIN, 16));
        sourceCodeProps.set(AnimationPropertiesKeys.HIGHLIGHTCOLOR_PROPERTY, Color.RED);

        sc = lang.newSourceCode(new Coordinates(0, 350), "sourceCode",
                null, sourceCodeProps);

        exp = lang.newSourceCode(new Coordinates(550, 350), "explanation", null, sourceCodeProps);

        sc.addCodeLine("for i = 1 to NumberOfSamples:", null, 0*INDENTATION_WIDTH, null);                         // 0
        exp.addCodeLine("// starte neue Iteration", null, 0, null);
        sc.addCodeLine("for each Var in NonevidenceVars:", null, 1*INDENTATION_WIDTH, null);                      // 1
        exp.addCodeLine("// wähle Zufallsvariable, deren Wert nicht bekannt ist, aus", null, 0, null);
        sc.addCodeLine("p = P( Var | parents(Var) )", null, 2*INDENTATION_WIDTH, null);                           // 2
        exp.addCodeLine("// Die Wahrscheinlichkeit P( Var | markov-blanket(Var) ) = P(Var | parents(Var) ) * ...", null, 0, null);
        sc.addCodeLine("for each ChildVar in children(Var):", null, 2*INDENTATION_WIDTH, null);                   // 3
        exp.addCodeLine("// ... * P( ChildVar | parents(ChildVar) )  für jeden Kindknoten ...", null, 0, null);
        sc.addCodeLine("p = p * P( ChildVar | parents(ChildVar) )", null, 3*INDENTATION_WIDTH, null);             // 4
        exp.addCodeLine("// ... wobei diese bedingten Wahrscheinlichkeiten bekannt sind", null, 0, null);
        sc.addCodeLine("sampleValue = createValueGivenProbability(p)", null, 2*INDENTATION_WIDTH, null);          // 5
        exp.addCodeLine("// basierend auf der berechneten Wahrscheinlichkeit 'p', erzeuge einen Wert für die ...", null, 0, null);
        sc.addCodeLine("increaseSampleCount(Var, sampleValue)", null, 2*INDENTATION_WIDTH, null);                 // 6
        exp.addCodeLine("// ... gewählte Zufallsvariable und speichere den Wert in einer Liste", null, 0, null);
        sc.addCodeLine("return normalize(Samples)", null, 0*INDENTATION_WIDTH, null);                             // 7
        exp.addCodeLine("// normalisiere die Liste und gib das Ergebnis zurück", null, 0, null);
    }

    private void sample() {

        iteration++;
        updateInformation();

        for(String var: new String[]{Y, X}) {

            unhighlightStep(0);
            unhighlightStep(6);
            highlightStep(1);

            highlightNode(var, Color.GRAY);
            updateVars(var, null, null);

            lang.nextStep();

            double p = probabilities.get(key(var, parents(var)));

            updateVars(var, null, p);

            unhighlightStep(1);
            highlightStep(2);

            lang.nextStep();

            for(String child: children(var)) {

                unhighlightStep(2);
                unhighlightStep(4);
                highlightStep(3);

                highlightNode(child, Color.LIGHT_GRAY);

                updateVars(var, child, p);

                lang.nextStep();

                p *= probabilities.get(key(child, parents(child)));

                updateVars(var, child, p);

                unhighlightStep(3);
                highlightStep(4);

                highlightNode(child, values.get(child) ? Color.GREEN : Color.RED);

                lang.nextStep();
            }

            unhighlightStep(4);
            highlightStep(5);

            lang.nextStep();

            boolean value = createSampleValue(p);

            values.put(var, value);

            highlightNode(var, value ? Color.GREEN : Color.RED);

            unhighlightStep(5);
            highlightStep(6);

            lang.nextStep();

            increaseSampleCount(var, value);
            updateInformation();

            unhighlightStep(6);
        }

        updateVars(null, null, null);
    }

    private boolean createSampleValue(double p) {

        return random.nextDouble() <= p;
    }

    private void increaseSampleCount(String var, boolean value) {

        if(var.equals(X)) {

            samplesX[value ? 1 : 0]++;
            normalizedSamplesX = normalize(samplesX);

        } else if (var.equals(Y)) {

            samplesY[value ? 1 : 0]++;
            normalizedSamplesY = normalize(samplesY);
        }
    }

    private String[] parents(String var) {

        switch (var) {
            case A: return new String[]{Y};
            case B: return new String[]{A,X};
            case X: return new String[]{Y};
            case Y: return new String[]{};
            default: return new String[]{};
        }
    }

    private String[] children(String var) {

        switch (var) {
            case A: return new String[]{B};
            case B: return new String[]{};
            case X: return new String[]{B};
            case Y: return new String[]{A,X};
            default: return new String[]{};
        }
    }

    private double[] normalize(int [] input) {

        if(input.length != 2) return null;

        double sum = input[0] + input[1];

        return new double[] {input[0] / sum, input[1] / sum};
    }


    private void highlightStep(int lineNo) {

        sc.highlight(lineNo);
        exp.highlight(lineNo);
    }

    private void unhighlightStep(int lineNo) {

        sc.unhighlight(lineNo);
        exp.unhighlight(lineNo);
    }

    private void highlightNode(String node, Color color) {

        g.setNodeHighlightFillColor(node2int(node), color, null, null);
        g.highlightNode(node2int(node), null, null);
    }

    private void unhighlightNode(String node) {

        g.unhighlightNode(node2int(node), null, null);
    }

    private int node2int(String node) {
        switch (node) {
            case A: return 1;
            case B: return 3;
            case X: return 2;
            case Y: return 0;
            default: return -1;
        }
    }

    public String key(final String var) { return key(var, new String[]{}); }
    public String key(final String var, final String... evidence) {

        StringBuilder sb = new StringBuilder();
        sb.append("P(");
        sb.append(var);

        if(evidence.length > 0) {
            sb.append(" | ");
        }

        Arrays.sort(evidence);

        for(int i = 0; i < evidence.length; i++) {

            if(i != 0) sb.append(", ");

            sb.append(evidence[i]);
            sb.append("=");
            sb.append(values.get(evidence[i]));
        }

        sb.append(")");

        return sb.toString();
    }

    public String getName() {
        return "Gibbs Sampling";
    }

    public String getAlgorithmName() {
        return "Gibbs Sampling";
    }

    public String getAnimationAuthor() {
        return "Moritz Schramm";
    }

    public String getDescription(){
        return "desc";
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
        return Locale.GERMAN;
    }

    public GeneratorType getGeneratorType() {
        return new GeneratorType(GeneratorType.GENERATOR_TYPE_MORE);
    }

    public String getOutputLanguage() {
        return Generator.PSEUDO_CODE_OUTPUT;
    }

    public boolean validateInput(AnimationPropertiesContainer props, Hashtable<String, Object> primitives) {

        for(String key: primitives.keySet()) {

            if(key.equals("A") || key.equals("B")) continue;

            double v = (double) primitives.get(key);

            if(v < 0.0 || v > 1.0) return false;
        }

        return true;
    }

    public static void main(String[] args) {

        Generator generator = new GibbsSampling();
        //animal.main.Animal.startGeneratorWindow(generator);

        generator.init();

        Hashtable<String, Object> primitives = new Hashtable<>();
        primitives.put("Seed", 1234);

        primitives.put("P(Y)", 0.8);
        primitives.put("P(X | Y=true)", 0.4);
        primitives.put("P(X | Y=false)", 0.7);
        primitives.put("P(A | Y=true)", 0.1);
        primitives.put("P(A | Y=false)", 0.2);
        primitives.put("P(B | A=true, X=true)", 0.9);
        primitives.put("P(B | A=true, X=false)", 0.99);
        primitives.put("P(B | A=false, X=true)", 0.3);
        primitives.put("P(B | A=false, X=false)", 0.6);

        primitives.put("A", false);
        primitives.put("B", true);

        System.out.println(generator.generate(null, primitives));
    }
}
