/**
 * 
 */



import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.github.javacliparser.AbstractOption;
import com.github.javacliparser.FileOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;

import Others.DuEvaluateModel;
import Others.Output;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.core.driftdetection.ChangeDetector;
import moa.classifiers.meta.WEKAClassifier;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.options.ClassOption;
import moa.classifiers.MultiClassClassifier;



/**
 * @author DuHonghui
 *
 */
public class Melanie extends AbstractClassifier implements MultiClassClassifier{

	private static final long serialVersionUID = 1L;
	
	public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'b',
            "OnlineBoosting Classifier to train.", Classifier.class, "meta.OzaBoost");
	
    public ClassOption driftDetectionMethodOption = new ClassOption("driftDetectionMethod", 'd',
           "Drift detection method to use.", ChangeDetector.class, "DDM");
	
	public FloatOption thetaOption = new FloatOption("theta", 't',
            "Minimum fraction of weight per model.", 0.9, 0.0, 1.0);
    
    public FloatOption lamdaOption = new FloatOption("lamda", 'l',
            "Minimum fraction of weight per model.", 0.05, 0.0, 1.0);
    
    public FloatOption accuracyOption = new FloatOption("accuracy", 'a',
            "Minimum fraction of weight per model.", 0.5, 0.0, 1.0);
    
    
    //********************************************************************************
    //These options are for evaluation only
    //Output option
    public IntOption instanceSizeOption = new IntOption("instanceSize", 'i',
    		"The number of instances.", 1000, 1, Integer.MAX_VALUE);
    
    public FlagOption slideWindowOption = new FlagOption("slidewindowoption", 'w',
            "Boost with weights only; no poisson.");
    
    public IntOption concpetOption = new IntOption("conceptPosition", 'c',
    		"The position of concept drift.", 1000, 1, Integer.MAX_VALUE);
    
    public IntOption slidwindowsizeOption = new IntOption("slidewindowsize", 's',
    		"Slide Window Size.", 1000, 1, Integer.MAX_VALUE);
    
    public IntOption runTimesOption = new IntOption("runTimes", 'u',
    		"The times.", 1000, 1, Integer.MAX_VALUE);
    
    public FileOption outputFileOption = new FileOption("outputFile", 'f',
            "Destination ARFF file.", null, "xls", true);
    
    protected DuEvaluateModel evaluateModel = new DuEvaluateModel();
    protected Output output = new Output();
    
    //********************************************************************************
    
    protected double theta;
    protected double lamda;
    protected double preAccuracy;
    protected int targetSize;
    protected double sumOfTheta;
    
    protected List<Integer> numberOfSource;
    
    protected List<List<Classifier>> ensemble;
    protected List<Classifier> newBaseLeanerList;
    protected Classifier newBaseLeaner;
    
    protected List<ChangeDetector> driftDetectionMethodList;
    protected ChangeDetector driftDetectionMethod;
    
    protected List<List<double[]>> lossPerform;
    protected List<List<double[]>> alpha;
    protected List<List<double[]>> weight;

    protected List<Boolean> newClassifierReset;
    
    public static final int DDM_INCONTROL_LEVEL = 0;

    public static final int DDM_WARNING_LEVEL = 1;

    public static final int DDM_OUTCONTROL_LEVEL = 2;
    
    protected final int TARGETDOMAIN_INDEX = 0;
    
    @Override
    public void resetLearningImpl() {
    	this.theta = this.thetaOption.getValue();
    	this.lamda = this.lamdaOption.getValue();
    	this.preAccuracy = this.accuracyOption.getValue();
    	this.targetSize = 0;
    	
    	this.numberOfSource = new ArrayList<Integer>();
    	
    	this.ensemble = new ArrayList<List<Classifier>>();
    	this.newBaseLeanerList = new ArrayList<Classifier>();
    	this.newBaseLeaner = ((Classifier) getPreparedClassOption(this.baseLearnerOption)).copy();
        this.newBaseLeaner.resetLearning();
        this.newBaseLeanerList.add(this.newBaseLeaner.copy());   
        
        this.driftDetectionMethodList = new ArrayList<ChangeDetector>();
        this.driftDetectionMethod = ((ChangeDetector) getPreparedClassOption(this.driftDetectionMethodOption)).copy();
        this.driftDetectionMethod.resetLearning();
        
        this.lossPerform = new ArrayList<List<double[]>>();
        this.alpha = new ArrayList<List<double[]>>();
        this.weight = new ArrayList<List<double[]>>();
        this.newClassifierReset = new ArrayList<Boolean>();
        
        //******************************************************
        this.evaluateModel.evaluateInitialize(this.instanceSizeOption.getValue(), this.concpetOption.getValue(), this.slideWindowOption.isSet(), this.slidwindowsizeOption.getValue());
        //******************************************************
        
    }
    
    
    @Override
	public void trainOnInstanceImpl(Instance inst) {
    	int sn = (int) inst.value(0);
    	
    	Instance instTmp = inst.copy();
    	instTmp.deleteAttributeAt(0); //remove data source number before training and evaluating
    	instTmp.dataset().setClassIndex(instTmp.numAttributes()-1);
    	
    	if(!this.numberOfSource.contains(sn)) {
    		this.numberOfSource.add(sn);
    		this.generateNewBaseLearnerList(this.numberOfSource.indexOf(sn));	
    	}
		
    	int sourceNum = this.numberOfSource.indexOf(sn);
    		
    	switch(this.detectDrfit(sourceNum, instTmp)) {
    	case DDM_WARNING_LEVEL:
            if (this.newClassifierReset.get(sourceNum) == true) {
            	this.newBaseLeanerList.get(sourceNum).resetLearning();
                this.newClassifierReset.set(sourceNum, false);
            }
            this.newBaseLeanerList.get(sourceNum).trainOnInstance(instTmp);
            break;

        case DDM_OUTCONTROL_LEVEL:
            this.generateSubList(sourceNum);
            if (this.ensemble.get(sourceNum).get(this.ensemble.get(sourceNum).size() - 1) instanceof WEKAClassifier) {
                ((WEKAClassifier) this.ensemble.get(sourceNum).get(this.ensemble.get(sourceNum).size() - 1)).buildClassifier();
            }
            this.newBaseLeanerList.get(sourceNum).resetLearning();
            this.driftDetectionMethodList.get(sourceNum).resetLearning();
            this.targetSize = 0;
            break;

        case DDM_INCONTROL_LEVEL:
        	this.newClassifierReset.set(sourceNum, true);
            break;
        default:
        //System.out.println("ERROR!");
    	}
    	
    	this.ensemble.get(sourceNum).get(this.ensemble.get(sourceNum).size() - 1).trainOnInstance(instTmp);
    	
    	if(sn == this.TARGETDOMAIN_INDEX) {
    		this.targetSize ++;
    		this.updateWeight(instTmp);
    	}
		
	}
    
    
    protected void generateNewBaseLearnerList(int sn) {
    	Classifier baseLearner = ((Classifier) getPreparedClassOption(this.baseLearnerOption)).copy();
    	baseLearner.resetLearning();
    	List<Classifier> newBaseLearnerList = new ArrayList<Classifier>();
    	newBaseLearnerList.add(baseLearner.copy());
    	
    	List<double[]> newLossPerfomList = new ArrayList<double[]>();
    	double[] newSubLossPerform = new double[baseLearner.getSubClassifiers().length];
    	for(double i: newSubLossPerform) {
    		i = 0.0;
    	}
    	newLossPerfomList.add(newSubLossPerform);
    	
    	List<double[]> alphaList = new ArrayList<double[]>();
    	double[] newSubAlpha = new double[baseLearner.getSubClassifiers().length];
    	for(double i: newSubAlpha) {
    		i = 0.0;
    	}
    	alphaList.add(newSubAlpha);
    	
    	List<double[]> weightList = new ArrayList<double[]>();
    	double[] newSubWeight = new double[baseLearner.getSubClassifiers().length];
    	for(double i: newSubWeight) {
    		i = 0.0;
    	}
    	weightList.add(newSubWeight);
    	
    	
    	this.ensemble.add(sn, newBaseLearnerList);
    	this.newBaseLeanerList.add(sn, baseLearner.copy());
    	this.driftDetectionMethodList.add(sn, this.driftDetectionMethod.copy());
    	this.lossPerform.add(sn, newLossPerfomList);
    	this.alpha.add(sn, alphaList);
    	this.weight.add(sn, weightList);
    	this.newClassifierReset.add(sn, false);
    }
    
    
    protected void generateSubList(int sn) {
    	
    	List<double[]> newLossPerfomList = new ArrayList<double[]>();
    	double[] newSubLossPerform = new double[this.newBaseLeaner.getSubClassifiers().length];
    	for(double i: newSubLossPerform) {
    		i = 0.0;
    	}
    	newLossPerfomList.add(newSubLossPerform);
    	
    	List<double[]> alphaList = new ArrayList<double[]>();
    	double[] newSubAlpha = new double[this.newBaseLeaner.getSubClassifiers().length];
    	for(double i: newSubAlpha) {
    		i = 0.0;
    	}
    	alphaList.add(newSubAlpha);
    	
    	List<double[]> weightList = new ArrayList<double[]>();
    	double[] newSubWeight = new double[this.newBaseLeaner.getSubClassifiers().length];
    	for(double i: newSubWeight) {
    		i = 0.0;
    	}
    	weightList.add(newSubWeight);
    	
    	this.ensemble.get(sn).add(this.newBaseLeanerList.get(sn).copy());
    	this.lossPerform.get(sn).add(newSubLossPerform);
    	this.alpha.get(sn).add(newSubAlpha);
    	this.weight.get(sn).add(newSubWeight);
    	
    }
    
    
    protected int detectDrfit(int sn, Instance inst) {
    	int ddmLevel;
    	//int trueClass = (int) inst.classValue();
        boolean prediction;
        
        if(this.ensemble.get(sn).get(this.ensemble.get(sn).size() - 1).correctlyClassifies(inst)) {
        //if (Utils.maxIndex(this.ensemble.get(sn).get(this.ensemble.get(sn).size() - 1).getVotesForInstance(inst)) == trueClass) {
            prediction = true;
        } else {
            prediction = false;
        }
        
        this.driftDetectionMethodList.get(sn).input(prediction ? 0.0 : 1.0);
        ddmLevel = DDM_INCONTROL_LEVEL;
        if (this.driftDetectionMethodList.get(sn).getChange()) {
        	ddmLevel =  DDM_OUTCONTROL_LEVEL;
        }
        if (this.driftDetectionMethodList.get(sn).getWarningZone()) {
        	ddmLevel =  DDM_WARNING_LEVEL;
        }
    	
    	return ddmLevel;	
    }
    
    
    protected double argmax(double numOne, double numTwo) {
    	if(numOne > numTwo)
    		return numOne;
    	else
    		return numTwo;
    }
    
    protected void calculateLossPerform(Instance inst) {    	
    	for(List<Classifier> baseLearnerList: this.ensemble) {
    		for(Classifier baseLearner: baseLearnerList) {
    			for(int k = 0; k < baseLearner.getSubClassifiers().length; k++) {
    				int i = this.ensemble.indexOf(baseLearnerList);
					int j = baseLearnerList.indexOf(baseLearner);
					
					double loss = 0.0;
					DoubleVector vote = new DoubleVector(baseLearner.getSubClassifiers()[k].getVotesForInstance(inst));
					if(vote.sumOfValues() > 0) {
						 vote.normalize();
					}
					
					for(int v = 0; v < vote.numValues(); v++) {
						if(v != (int)inst.classValue())
							loss += this.argmax(0.0, vote.getValue(v) - vote.getValue((int)inst.classValue()) + this.preAccuracy);
					}
					
    				double lossPerfo = this.lossPerform.get(i).get(j)[k];
    				lossPerfo = this.theta*lossPerfo + (1 - loss);
    				this.lossPerform.get(i).get(j)[k] = lossPerfo;
    			}
    		}
    	}
    }
    
    protected void updateWeight(Instance inst) {
    	this.calculateLossPerform(inst);
    	this.calculateAlpha(this.getSumOfTheta());
    	
    	double sumOfAlpha = 0.0;
    	for(List<double[]> baseLearnerList: this.alpha) {
    		for(double[] baseLearner: baseLearnerList) {
    			for(double a: baseLearner) {	
					if(a > this.preAccuracy) {
						sumOfAlpha += a;
					}
    			}
    		}
    	}
    	
    	this.calculateWeight(sumOfAlpha);
    }

    
	@Override
	public boolean isRandomizable() {
		// TODO Auto-generated method stub
		return false;
	}

	protected double getSumOfTheta() {
		if(this.targetSize == 1)
			sumOfTheta = 0.0;
		
		sumOfTheta += Math.pow(this.theta, this.targetSize - 1);
		
		return sumOfTheta;
	}


	protected void calculateAlpha(double sumOfTheta) {
		for(List<double[]> baseLearnerList: this.lossPerform) {
    		for(double[] baseLearner: baseLearnerList) {
    			for(int k = 0; k < baseLearner.length; k++) {
    				int i = this.lossPerform.indexOf(baseLearnerList);
					int j = baseLearnerList.indexOf(baseLearner);
					
					this.alpha.get(i).get(j)[k] = (1/sumOfTheta)*baseLearner[k]; 
    			}
    		}
		}	
	}
	
	
	protected void calculateWeight(double sumOfAlpha) {
		for(List<double[]> baseLearnerList: this.alpha) {
    		for(double[] baseLearner: baseLearnerList) {
    			for(int k = 0; k < baseLearner.length; k++) {
    				int i = this.alpha.indexOf(baseLearnerList);
					int j = baseLearnerList.indexOf(baseLearner);
					
					if(baseLearner[k] > this.preAccuracy) {
						this.weight.get(i).get(j)[k] = (1/sumOfAlpha)*baseLearner[k]; 
					}else {
						this.weight.get(i).get(j)[k] = 0;
					}
					  				
    			}
    		}
		}	
	}
	
	
	@Override
	public double[] getVotesForInstance(Instance inst) {
		if((int)inst.value(0) != 0){
            double[] a = {0.0,0.0};
        	return a;
    	}
		
		Instance instTmp = inst.copy();
    	instTmp.deleteAttributeAt(0); //remove data source number before training and evaluating
    	instTmp.dataset().setClassIndex(instTmp.numAttributes()-1);
    	
    	
		DoubleVector combinedVote = new DoubleVector();
		for(List<Classifier> baseLearnerList: this.ensemble) {
			for(Classifier baseLearner: baseLearnerList) {
				for(int k = 0; k < baseLearner.getSubClassifiers().length; k++) {
					int i = this.ensemble.indexOf(baseLearnerList);
					int j = baseLearnerList.indexOf(baseLearner);
					
					double memberWeight = this.weight.get(i).get(j)[k];
					
					//System.out.print("i: " + i + " j: "+ j + " k: "+  k + " member weight: " + memberWeight + " ") ;
					
					if(memberWeight > 0.0) {
						DoubleVector vote = new DoubleVector(baseLearner.getSubClassifiers()[k].getVotesForInstance(instTmp));
						if (vote.sumOfValues() > 0.0) {
		                    vote.normalize();
		                    vote.scaleValues(memberWeight);
		                    combinedVote.addValues(vote);
		                }
					}
					
				}
			}

        }
		
		
		//System.out.println();
		
		//****************************************************************
		//Evaluation Only		
		this.evaluateModel.evaluation(instTmp, combinedVote);
		if(this.evaluateModel.getNumOfExamplesReceived() == this.instanceSizeOption.getValue()) {
			try {
				this.output.fullToXls(this.evaluateModel.getResults(), this.outputFileOption.getValue(), this.runTimesOption.getValue());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			List<AbstractOption> options = new ArrayList<AbstractOption>();
			options.add(this.thetaOption);
			options.add(this.lamdaOption);
			options.add(this.accuracyOption);
			
			try {
				this.output.MeasurementToTxt(this.evaluateModel.getMeasurement(), this.outputFileOption.getValue(), options);
				//this.output.MeasurementGMeanToTxt(this.outputFileOption.getValue(), this.evaluateModel.getGMean());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//****************************************************************
		
		inst.dataset().setClassIndex(inst.numAttributes() - 1);
        return combinedVote.getArrayRef();
	}
	

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		// TODO Auto-generated method stub
		
	}
}
