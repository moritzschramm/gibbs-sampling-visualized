package generators.misc.BNSamplingHelper;

import algoanim.animalscript.AnimalScript;
import algoanim.primitives.Text;
import algoanim.primitives.generators.Language;
import algoanim.properties.AnimationPropertiesKeys;
import algoanim.properties.TextProperties;
import algoanim.util.Coordinates;
import algoanim.util.Offset;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Hashtable;
import java.util.Locale;

public class InformationDisplay {

    private Language lang;
    private BayesNet bn;
    private String[] sampleVars;
    private String of;
    private String normValue;

    private Text iterationDisplay;
    private Text sampleDisplay;
    private Text normalizedSampleDisplay;
    private Text varDisplay;
    private Text childVarDisplay;
    private Text probabilityDisplay;

    private Hashtable<String, Integer> samples;
    private Hashtable<String, Double> normalizedSamples;


    public InformationDisplay(Language lang, BayesNet bn,
                              Hashtable<String, Integer> samples, Hashtable<String, Double> normalizedSamples) {

        this.lang = lang;
        this.bn = bn;
        this.samples = samples;
        this.normalizedSamples = normalizedSamples;
    }

    public void init(String[] sampleVars, String of, String normValue) {

        this.sampleVars = sampleVars;
        this.of = of;
        this.normValue = normValue;
    }

    public void add() {

        TextProperties props = new TextProperties();
        props.set(AnimationPropertiesKeys.FONT_PROPERTY, new Font(
                Font.SANS_SERIF, Font.PLAIN, 16));

        iterationDisplay = lang.newText(new Offset(50, 0, "explanation", AnimalScript.DIRECTION_NE), "Iteration: 0",
                "iterationDisplay", null, props);
        sampleDisplay = lang.newText(new Offset(0, 25, "iterationDisplay",
                AnimalScript.DIRECTION_NW), getSampleCount("Sample (true, false) " + of + " "),
                "sampleXDisplay", null, props);

        normalizedSampleDisplay =
                lang.newText(new Offset(0, 25, "sampleXDisplay",
                        AnimalScript.DIRECTION_NW),
                        getSampleCount(normValue+" (true, false) " + of + " "),
                        "normalizedSampleXDisplay", null, props);

        props.set(AnimationPropertiesKeys.FONT_PROPERTY, new Font(Font.SANS_SERIF, Font.BOLD, 16));

        varDisplay = lang.newText(new Offset(0, 50, "normalizedSampleXDisplay",
                AnimalScript.DIRECTION_NW), "", "varDisplay", null, props);
        probabilityDisplay = lang.newText(new Offset(0, 25, "varDisplay",
                AnimalScript.DIRECTION_NW), "", "probabilityDisplay", null, props);
        childVarDisplay = lang.newText(new Offset(0, 25, "probabilityDisplay",
                AnimalScript.DIRECTION_NW), "", "childVarDisplay", null, props);

    }

    public void updateInformation(int iteration) {

        iterationDisplay.setText("Iteration: " + iteration, null, null);
        sampleDisplay.setText(getSampleCount("Sample (true, false) " + of + " "), null, null);
        normalizedSampleDisplay.setText(getNormalizedSampleCount(normValue+" (true, false) " + of + " "), null, null);
    }

    public void updateVars(String var, String childVar, Double probability) {

        DecimalFormat df = new DecimalFormat("0.0###", new DecimalFormatSymbols(Locale.ENGLISH));

        if(var != null ) varDisplay.setText("Var = " + var, null, null);
        else varDisplay.setText("", null, null);

        if(childVar != null) childVarDisplay.setText("ChildVar = " + childVar, null, null);
        else childVarDisplay.setText("", null, null);

        if(probability != null) probabilityDisplay.setText("p = " + df.format(probability), null, null);
        else probabilityDisplay.setText("", null, null);
    }

    public String getSampleCount(final String prefix) {

        StringBuilder sb = new StringBuilder();
        sb.append(prefix);

        String conc = "";
        for(String var: sampleVars) {
            sb.append(conc);
            conc = ", ";
            sb.append(var);
        }

        sb.append(": ");

        conc = "";
        for(String var: sampleVars) {
            sb.append(conc);
            conc = ", ";

            sb.append("(");
            sb.append(samples.get(var+"=true") == null ? 0 : samples.get(var+"=true"));
            sb.append(", ");
            sb.append(samples.get(var+"=false") == null ? 0 : samples.get(var+"=false"));
            sb.append(")");
        }

        return sb.toString();
    }

    public String getNormalizedSampleCount(final String prefix) {

        DecimalFormat df = new DecimalFormat("0.0##", new DecimalFormatSymbols(Locale.ENGLISH));
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);

        String conc = "";
        for(String var: sampleVars) {
            sb.append(conc);
            conc = ", ";
            sb.append(var);
        }

        sb.append(": ");

        conc = "";
        for(String var: sampleVars) {
            sb.append(conc);
            conc = ", ";

            sb.append("(");
            sb.append(normalizedSamples.get(var+"=true") == null ? 0 : df.format(normalizedSamples.get(var+"=true")));
            sb.append(", ");
            sb.append(normalizedSamples.get(var+"=false") == null ? 0 : df.format(normalizedSamples.get(var+"=false")));
            sb.append(")");
        }

        return sb.toString();
    }
}
