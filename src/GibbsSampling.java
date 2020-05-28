/*
 * GibbsSampling.java
 * Moritz Schramm, 2020 for the Animal project at TU Darmstadt.
 * Copying this file for educational purposes is permitted without further authorization.
 */
//FIXME package generators.misc;

import algoanim.primitives.Text;
import algoanim.properties.*;
import generators.framework.Generator;
import generators.framework.GeneratorType;
import generators.framework.ValidatingGenerator;

import java.awt.*;
import java.util.*;
import algoanim.primitives.generators.Language;
import generators.framework.properties.AnimationPropertiesContainer;
import algoanim.animalscript.AnimalScript;
import algoanim.util.*;

public class GibbsSampling implements ValidatingGenerator {

    private Language lang;

    private Random random;

    private Text header;

    private Code code;
    private BayesNet bn;
    private InformationDisplay info;

    // iteration number, increased when sample() is called
    private int iteration = 0;

    // contains sum of sample values (sampleX[0]: samples when X is false, sampleX[1]: samples when X is true)
    private int[] samplesX;
    private int[] samplesY;
    private double[] normalizedSamplesX;
    private double[] normalizedSamplesY;


    public void init() {
        lang = new AnimalScript("Gibbs Sampling", "Moritz Schramm", 800, 600);
        lang.setStepMode(true);
        lang.setInteractionType(Language.INTERACTION_TYPE_AVINTERACTION);

        random = new Random();

        iteration = 0;
        samplesX = new int[2];
        samplesY = new int[2];
        normalizedSamplesX = new double[2];
        normalizedSamplesY = new double[2];

        code = new Code(lang);
        bn = new BayesNet(lang);
        info = new InformationDisplay(lang, bn, samplesX, samplesY, normalizedSamplesX, normalizedSamplesY);
    }

    /* methods used to create animation */
    public String generate(AnimationPropertiesContainer props, Hashtable<String, Object> primitives) {

        // set seed
        random.setSeed((int) primitives.get("Seed"));

        // init probabilities and values
        bn.setProbabilitiesAndValues(primitives);


        // header creation
        TextProperties headerProps = new TextProperties();
        headerProps.set(AnimationPropertiesKeys.FONT_PROPERTY, new Font(
                Font.SANS_SERIF, Font.BOLD, 24));
        header = lang.newText(new Coordinates(20, 30), "Gibbs Sampling",
                "header", null, headerProps);

        lang.nextStep("Einleitung");

        // show introduction text (creates new step)
        showIntro();

        // graph creation
        bn.add();

        // show additional information
        info.add();

        // highlight evidence vars (value won't be changing, highlight color will stay the same)
        bn.highlightNode(BayesNet.A, bn.values.get(BayesNet.A) ? Color.GREEN : Color.RED);
        bn.highlightNode(BayesNet.B, bn.values.get(BayesNet.B) ? Color.GREEN : Color.RED);

        // add source code (unhighlighted)
        code.add();

        lang.nextStep("1. Iteration");

        code.highlight(0);


        lang.nextStep();

        sample();

        /*MultipleChoiceQuestionModel m = new MultipleChoiceQuestionModel("samples");
        m.setPrompt("Wie oft soll noch gesamplet werden?");
        m.setNumberOfTries(1);
        m.addAnswer("10", 10, "OK");
        m.addAnswer("100", 100, "OK");
        m.addAnswer("1000", 1000, "OK");

        lang.addMCQuestion(m);*/


        int SAMPLES = 9;

        for(int i = 0; i < SAMPLES; i++) {


            code.highlight(0);
            lang.nextStep();
            sample();
        }

        code.highlight(6);
        code.highlight(7);

        lang.nextStep("Zusammenfassung");

        showOutro();


        lang.finalizeGeneration();

        return lang.toString();
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



    /* algorithm */

    private void sample() {

        iteration++;
        info.updateInformation(iteration);

        for(String var: new String[]{BayesNet.Y, BayesNet.X}) {

            code.unhighlight(0);
            code.unhighlight(6);
            code.highlight(1);

            bn.highlightNode(var, Color.GRAY);
            info.updateVars(var, null, null);

            lang.nextStep();

            double p = bn.probabilities.get(bn.key(var, bn.parents(var)));

            info.updateVars(var, null, p);

            code.unhighlight(1);
            code.highlight(2);

            lang.nextStep();

            for(String child: bn.children(var)) {

                code.unhighlight(2);
                code.unhighlight(4);
                code.highlight(3);

                bn.highlightNode(child, Color.LIGHT_GRAY);

                info.updateVars(var, child, p);

                lang.nextStep();

                p *= bn.probabilities.get(bn.key(child, bn.parents(child)));

                info.updateVars(var, child, p);

                code.unhighlight(3);
                code.highlight(4);

                bn.highlightNode(child, bn.values.get(child) ? Color.GREEN : Color.RED);

                lang.nextStep();
            }

            code.unhighlight(4);
            code.highlight(5);

            lang.nextStep();

            boolean value = createSampleValue(p);

            bn.values.put(var, value);

            bn.highlightNode(var, value ? Color.GREEN : Color.RED);

            code.unhighlight(5);
            code.highlight(6);

            lang.nextStep();

            increaseSampleCount(var, value);
            info.updateInformation(iteration);

            code.unhighlight(6);
        }

        info.updateVars(null, null, null);
    }

    private boolean createSampleValue(double p) {

        return random.nextDouble() <= p;
    }

    private void increaseSampleCount(String var, boolean value) {

        if(var.equals(BayesNet.X)) {

            samplesX[value ? 1 : 0]++;
            double sum = samplesX[0] + samplesX[1];
            normalizedSamplesX[0] = samplesX[0] / sum;
            normalizedSamplesX[1] = samplesX[1] / sum;

        } else if (var.equals(BayesNet.Y)) {

            samplesY[value ? 1 : 0]++;
            double sum = samplesY[0] + samplesY[1];
            normalizedSamplesY[0] = samplesY[0] / sum;
            normalizedSamplesY[1] = samplesY[1] / sum;
        }
    }



    /* Interface methods */
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
        return "Bayessche Netze werden dazu genutzt, um Abhängigkeiten zwischen Zufallsvariablen zu modellieren und Wahrscheinlichkeiten von Ereignissen zu berechnen. Exakte Inferenz, d.h. die Bestimmung einer bedingten Wahrscheinlichkeit, ist in solchen Netzen allerdings ein NP-hartes Problem, weswegen man mit Sampling Methoden zumindest eine annähernd exakte Inferenz erreichen will.<br>Hier wird Gibbs Sampling genutzt, ein Markov Chain Monte Carlo Algorithmus. Dieser beginnt in einem willkürlichen Zustand und erzeugt jede Iteration einen neuen Zustand, indem ein Wert durch ein zufälliges Sample einer Zufallsvariable erzeugt wird. Die Wahrscheinlichkeit einen bestimmten Wert zu samplen hängt dabei von den vorher festgeletgten bedingten Wahrscheinlichkeiten der Zufallsvariablen ab.";
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

            if(key.equals("A") || key.equals("B") || key.equals("Seed")) continue;

            double v = (double) primitives.get(key);

            if(v < 0.0 || v > 1.0) return false;
        }

        return true;
    }

    public static void main(String[] args) {

        Generator generator = new GibbsSampling();
        generator.init();
        //animal.main.Animal.startGeneratorWindow(generator);



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
