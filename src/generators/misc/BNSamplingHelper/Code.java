package generators.misc.BNSamplingHelper;

import algoanim.animalscript.AnimalScript;
import algoanim.primitives.SourceCode;
import algoanim.primitives.generators.Language;
import algoanim.properties.AnimationPropertiesKeys;
import algoanim.properties.SourceCodeProperties;
import algoanim.util.Coordinates;

import java.awt.*;

import algoanim.util.Offset;
import translator.Translator;

public class Code {

    public final int INDENTATION_WIDTH = 2;

    private Language lang;
    private Translator translator;
    private SourceCode sc;
    private SourceCode exp;

    public Code(Language lang, Translator translator) {
        this.lang = lang;
        this.translator = translator;
    }

    public void add() {add(20, 50);}
    public void add(int x, int y) {

        SourceCodeProperties sourceCodeProps = new SourceCodeProperties();
        sourceCodeProps.set(AnimationPropertiesKeys.FONT_PROPERTY, new Font(
                Font.MONOSPACED, Font.PLAIN, 16));
        sourceCodeProps.set(AnimationPropertiesKeys.HIGHLIGHTCOLOR_PROPERTY, Color.RED);

        SourceCodeProperties expCodeProps = new SourceCodeProperties();
        expCodeProps.set(AnimationPropertiesKeys.FONT_PROPERTY, new Font(
                Font.SANS_SERIF, Font.PLAIN, 16));
        expCodeProps.set(AnimationPropertiesKeys.HIGHLIGHTCOLOR_PROPERTY, Color.RED);

        exp = lang.newSourceCode(new Coordinates(x, y), "explanation", null, sourceCodeProps);
        exp.addCodeLine("1. "+translator.translateMessage("line0"), null, 0, null);
        exp.addCodeLine("2. "+translator.translateMessage("line1"), null, 0, null);
        exp.addCodeLine("3. "+translator.translateMessage("line2"), null, 0, null);
        exp.addCodeLine("4. "+translator.translateMessage("line3"), null, 0, null);
        exp.addCodeLine("5. "+translator.translateMessage("line4"), null, 0, null);
        exp.addCodeLine("6. "+translator.translateMessage("line5"), null, 0, null);
        exp.addCodeLine("7. "+translator.translateMessage("line6"), null, 0, null);
        exp.addCodeLine("8. "+translator.translateMessage("line7"), null, 0, null);

        sc = lang.newSourceCode(new Offset(0, 30, "explanation", AnimalScript.DIRECTION_NW), "sourceCode",
                null, sourceCodeProps);

        sc.addCodeLine("for i = 1 to NumberOfSamples:", null, 0*INDENTATION_WIDTH, null);                         // 0
        sc.addCodeLine("for each Var in NonevidenceVars:", null, 1*INDENTATION_WIDTH, null);                      // 1
        sc.addCodeLine("p = P( Var | parents(Var) )", null, 2*INDENTATION_WIDTH, null);                           // 2
        sc.addCodeLine("for each ChildVar in children(Var):", null, 2*INDENTATION_WIDTH, null);                   // 3
        sc.addCodeLine("p = p * P( ChildVar | parents(ChildVar) )", null, 3*INDENTATION_WIDTH, null);             // 4
        sc.addCodeLine("sampleValue = createValueGivenProbability(p)", null, 2*INDENTATION_WIDTH, null);          // 5
        sc.addCodeLine("increaseSampleCount(Var, sampleValue)", null, 2*INDENTATION_WIDTH, null);                 // 6
        sc.addCodeLine("return normalize(Samples)", null, 0*INDENTATION_WIDTH, null);                             // 7
    }

    public void highlight(int lineNo) {

        sc.highlight(lineNo);
        exp.highlight(lineNo);
    }

    public void unhighlight(int lineNo) {

        sc.unhighlight(lineNo);
        exp.unhighlight(lineNo);
    }
}
