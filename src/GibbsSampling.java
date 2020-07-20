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

    // contains sum of sample values (sampleX[0]: samples when X is false, sampleX[1]: samples when X is true)
    private int[] samplesX;
    private int[] samplesY;
    private double[] normalizedSamplesX;
    private double[] normalizedSamplesY;

    // for questions
    private String WRONG_ASW;
    private String RIGHT_ASW;

    public GibbsSampling(String resourceName, Locale locale) {
        this.resourceName = resourceName;
        this.locale = locale;
    }

    public void init() {
        lang = new AnimalScript("Gibbs Sampling", "Moritz Schramm, Moritz Andres", 800, 600);
        lang.setStepMode(true);
        lang.setInteractionType(Language.INTERACTION_TYPE_AVINTERACTION);


        translator = new Translator(resourceName, locale);

        random = new Random();

        iteration = 0;
        samplesX = new int[2];
        samplesY = new int[2];
        normalizedSamplesX = new double[2];
        normalizedSamplesY = new double[2];

        code = new Code(lang, translator);
        bn = new BayesNet(lang);
        info = new InformationDisplay(lang, bn, samplesX, samplesY, normalizedSamplesX, normalizedSamplesY);


        RIGHT_ASW = translator.translateMessage("right_asw");
        WRONG_ASW = translator.translateMessage("wrong_asw");
    }

    /* methods used to create animation */
    public String generate(AnimationPropertiesContainer props, Hashtable<String, Object> primitives) {

        // set seed
        random.setSeed((int) primitives.get("Seed"));

        // set number of iterations
        numberOfIterations = (int) primitives.get("Anzahl Iterationen");

        // init probabilities and values
        bn.setProbabilitiesAndValues(primitives);


        // header creation
        TextProperties headerProps = new TextProperties();
        headerProps.set(AnimationPropertiesKeys.FONT_PROPERTY, new Font(
                Font.SANS_SERIF, Font.BOLD, 24));
        header = lang.newText(new Coordinates(20, 30), "Gibbs Sampling",
                "header", null, headerProps);

        lang.nextStep(translator.translateMessage("introTOC"));

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

        lang.nextStep(translator.translateMessage("firstIterationTOC"));

        code.highlight(0);

        MultipleChoiceQuestionModel question1 = new MultipleChoiceQuestionModel("q1");
        String feedback_q1 = translator.translateMessage("q1_fb");
        question1.setPrompt(translator.translateMessage("q1_text"));
        question1.addAnswer(translator.translateMessage("q1_asw1"), 0, WRONG_ASW + feedback_q1);
        question1.addAnswer(translator.translateMessage("q1_asw2"), 0, WRONG_ASW + feedback_q1);
        question1.addAnswer(translator.translateMessage("q1_asw3"), 1, RIGHT_ASW + feedback_q1);
        lang.addMCQuestion(question1);

        lang.nextStep();

        sample();

        /*MultipleChoiceQuestionModel m = new MultipleChoiceQuestionModel("samples");
        m.setPrompt("Wie oft soll noch gesamplet werden?");
        m.setNumberOfTries(1);
        m.addAnswer("10", 10, "OK");
        m.addAnswer("100", 100, "OK");
        m.addAnswer("1000", 1000, "OK");

        lang.addMCQuestion(m);*/

        for(int i = 0; i < numberOfIterations - 1; i++) {


            code.highlight(0);
            lang.nextStep();
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

        /* TODO add content:
        - wofür brauchen wir gibbs sampling
        - Beispielnetzwerk erklären (Abhängigkeiten)
        - verwendete Farben erklären
        - posterior probability erklären
         */

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


        // TODO add summary total number of iterations, sample count, (posterior probability (true/false)
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
                "P( X=true | A="+bn.values.get(bn.A)+", B="+bn.values.get(bn.B)+" ) = "+normalizedSamplesX[1],
                "propTrueDisplayOutro", null, props);

        Text propFalseDisplay = lang.newText(new Offset(0, 30, "propTrueDisplayOutro", AnimalScript.DIRECTION_NW),
                "P( X=false | A="+bn.values.get(bn.A)+", B="+bn.values.get(bn.B)+" ) = "+normalizedSamplesX[0],
                "propFalseDisplayOutro", null, props);

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

            if (key.equals("A") || key.equals("B")) continue;

            if (key.equals("Seed") || key.equals("Anzahl Iterationen")) {
                int i = (int) primitives.get(key);
                if (i <= 0) return false;
                continue;
            }

            double v = (double) primitives.get(key);

            if (v < 0.0 || v > 1.0) return false;
        }

        return true;
    }

    public static void main(String[] args) {

        Generator generator = new GibbsSampling("resources/gibbssampling", Locale.GERMANY);
        generator.init();
        animal.main.Animal.startGeneratorWindow(generator);



        /*Hashtable<String, Object> primitives = new Hashtable<>();
        primitives.put("Seed", 1234);

        primitives.put("Anzahl Iterationen", 10);

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

        System.out.println(generator.generate(null, primitives));*/
    }
}
