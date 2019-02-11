package plugins.dbrasseur.hybridquantization;

import icy.gui.dialog.MessageDialog;
import icy.image.IcyBufferedImage;
import icy.image.colorspace.IcyColorSpace;
import icy.plugin.abstract_.PluginActionable;
import icy.sequence.Sequence;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import icy.util.Random;
import plugins.adufour.ezplug.*;

/**
 * Implementation of "HYBRID COLOR QUANTIZATION ALGORITHM INCORPORATING A HUMAN VISUAL PERCEPTION MODEL" by Schaefer and Nolle
 * @author Dylan Brasseur
 * @version 0.1
 *
 */
public class HybridQuantization extends EzPlug {

	private EzVarSequence	EzinputSeq;				//Image Sequence
	private EzVarInteger	EznbOfColors;			//Number of colors to be used
	private EzVarBoolean    EzUniformization;       //Uniform fake palette from the individual palettes
	
	//General optimization parameters
	private EzVarInteger	EzpopulationSize;		//Population size (not supported yet)
	private EzVarInteger	Ezimax;					//Max number of iterations
	private EzVarDouble		Ezdelta;				//Penalty constant

	//Temperature
	private EzVarDouble		EzT0;					//Initial temperature
	private EzVarInteger	EziTc;					//Number of iterations per temperature
	private EzVarDouble		Ezalpha;				//Cooling coefficient
	
	//Step size parameters
	private EzVarDouble		Ezs0;					//Initial Max Step Width
	private EzVarDouble		Ezbeta;					//Adaptation constant

	//S-CIELAB Visual Settings
	private EzVarInteger    Ezdpi;                  //Dots per inch of the monitor
	private EzVarDouble     EzViewingDistance;      //Viewing distance in cm
	private EzVarEnum       EzWhitePoint;           //Whitepoint
	
	
	@Override
	public void clean() {
		// TODO Auto-generated by Icy4Eclipse
	}

	@Override
	protected void execute() {
		quantization(EzUniformization.getValue(), EzinputSeq.getValue(), EznbOfColors.getValue(), EzpopulationSize.getValue(), Ezimax.getValue(), Ezdelta.getValue(), EzT0.getValue(), EziTc.getValue(), Ezalpha.getValue(), Ezs0.getValue(), Ezbeta.getValue(), Ezdpi.getValue(), EzViewingDistance.getValue(), (ScielabProcessor.Whitepoint) EzWhitePoint.getValue());
	}

	private void quantization(Boolean uniform, Sequence seq, Integer nbOfColors, Integer population, Integer imax, Double delta, Double T0, Integer iTc, Double alpha, Double s0, Double beta, Integer dpi, Double viewingDistance, ScielabProcessor.Whitepoint whitepoint) {
		IcyBufferedImage im = seq.getFirstImage();
		ScielabProcessor scielabProcessor = new ScielabProcessor(dpi, viewingDistance, whitepoint);
		double[][] scImg = scielabProcessor.imageToScielab(im.getDataXYCAsDouble(), im.getSizeX());
		// Test visuel
		scImg = scielabProcessor.LabTosRGB(scImg);

		Sequence seqOut = new Sequence();

		IcyBufferedImage imageOut =new IcyBufferedImage(im.getSizeX(), im.getSizeY(), im.getSizeC(), im.getDataType_());
		// Copie du tableau vers la sequence
		imageOut.setDataXY(0, Array1DUtil.doubleArrayToArray(scImg[0], imageOut.getDataXY(0)));
		imageOut.setDataXY(1, Array1DUtil.doubleArrayToArray(scImg[1], imageOut.getDataXY(1)));
		imageOut.setDataXY(2, Array1DUtil.doubleArrayToArray(scImg[2], imageOut.getDataXY(2)));

		seqOut.addImage(imageOut);
		seqOut.setName("End");

		// Affichage
		addSequence(seqOut);
	}


	@Override
	protected void initialize() {
		super.setTimeDisplay(true);
		EzinputSeq = new EzVarSequence("Input");
		EzinputSeq.setToolTipText("Images to be processed");
		EznbOfColors = new EzVarInteger("Number of colors",8,1,16777216,1);
		EznbOfColors.setToolTipText("Target number of colors in the palette | Default : 8 | Range : [1, 2^24]");
		EzUniformization = new EzVarBoolean("Fake palette", false);
		EzUniformization.setToolTipText("*CURRENTLY NOT SUPPORTED* If checked, the output sequence will have fake colors to be uniform over all the sequence | Default : false");

		//General optimization parameters
		EzpopulationSize = new EzVarInteger("Population size", 1,1,Integer.MAX_VALUE,1);
		EzpopulationSize.setToolTipText("*CURRENTLY NOT SUPPORTED* Number of color palettes used to find the optimized palette. | Default : 10");
		Ezimax = new EzVarInteger("Max iterations", 5000,1, Integer.MAX_VALUE,1);
		Ezimax.setToolTipText("Maximum number of iterations. A higher value may result in a longer processing time. | Default : 5000");
		Ezdelta = new EzVarDouble("Penalty Constant", 2, 0, Double.MAX_VALUE, 1);
		Ezdelta.setToolTipText("Penalty constant for unused palette colors. | Default : 2");

		//Temperature
		EzT0 = new EzVarDouble("Initial temperature", 20, 0, Double.MAX_VALUE, 1);
		EzT0.setToolTipText("Initial temperature used in the simulated annealing process. | Default : 20");
		EziTc = new EzVarInteger("Iterations per temperature",20, 1, Integer.MAX_VALUE, 1);
		EziTc.setToolTipText("Number of iterations where the temperature is kept constant (iterations per step). Default : 20");
		Ezalpha = new EzVarDouble("Cooling coefficient", 0.9, 0.0 ,1.0,0.1);
		Ezalpha.setToolTipText("Cooling coefficient by which the temperature is changed per step. | Default : 0.9 | Range : [0,1]");
		EzGroup temperatureGroup = new EzGroup("Temperature",EzT0, EziTc, Ezalpha);
		EzGroup optimizationGroup = new EzGroup("Optimization",EzpopulationSize, Ezimax, Ezdelta, temperatureGroup);

		//Step size parameters
		Ezs0 = new EzVarDouble("Initial Step size", 100, 1, Integer.MAX_VALUE, 1);
		Ezbeta = new EzVarDouble("Adaptation constant", 5.3, 0,Double.MAX_VALUE, 0.5);
		EzGroup stepSizeGroup = new EzGroup("Step size", Ezs0, Ezbeta);

		//S-CIELAB Visual Settings
		EzLabel EzscielabWarning = new EzLabel("Keep these parameters to default for computing purposes. \nFor visual purposes, use your screen's specifications");
		Ezdpi = new EzVarInteger("Dpi", 90, 1, Integer.MAX_VALUE, 1);
		Ezdpi.setToolTipText("Screen dpi | Default : 90");
		EzViewingDistance = new EzVarDouble("Viewing distance", 70, 1, Double.MAX_VALUE, 1);
		EzViewingDistance.setToolTipText("Viewing distance from the screen in cm | Default : 70");
		EzWhitePoint = new EzVarEnum<>("White point", ScielabProcessor.Whitepoint.values(),ScielabProcessor.Whitepoint.D65);
		EzWhitePoint.setToolTipText("White point of the image | Default : D65");
		EzGroup scielabGroup = new EzGroup("S-CIELAB", EzscielabWarning, Ezdpi, EzViewingDistance, EzWhitePoint);

		super.addEzComponent(EzinputSeq);
		super.addEzComponent(EznbOfColors);
		super.addEzComponent(EzUniformization);
		super.addEzComponent(optimizationGroup);
		super.addEzComponent(stepSizeGroup);
		super.addEzComponent(scielabGroup);

		scielabGroup.setFoldedState(true);

		//Unsupported parameters
		EzpopulationSize.setEnabled(false);
		EzUniformization.setEnabled(false);
	}

	private boolean isAccepted(double deltaE, double temperature)
	{
		return deltaE <= 0 || acceptanceProbability(deltaE, temperature) > Random.nextDouble();
	}

	private double acceptanceProbability(double deltaE, double temperature)
	{
		return Math.exp(-deltaE/temperature);
	}

	private double maxStepWidth(int i, double s0, double beta, int imax)
	{
		return 2*s0/(1+Math.exp(beta*i/imax));
	}

	private double deltaE(double[] p1, double[] p2)
	{
		double d0 = p1[0]-p2[0];
		double d1 = p1[1]-p2[1];
		double d2 = p1[2]-p2[2];
		return Math.sqrt(d0*d0+d1*d1+d2*d2);
	}

	private double computeError_internal(double[][] O, double[][] Q, double penalty, int w, int h)
	{
		double error = 0;
		for(int x=0; x < w;++x)
		{
			for(int y=0; y < h; ++y)
			{
				error += deltaE(O[x+y*w], Q[x+y*w]);
			}
		}
		return error/(w*h*3) + penalty;
	}
	
}