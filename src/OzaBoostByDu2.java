/*
 *    OzaBoost.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */


import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;


import com.yahoo.labs.samoa.instances.Instance;

import Others.DuEvaluateModel;
import Others.Output;
import moa.classifiers.MultiClassClassifier;

import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.MiscUtils;
import moa.core.Utils;
import moa.options.ClassOption;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.github.javacliparser.AbstractOption;
import com.github.javacliparser.FileOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;

/**
 * Incremental on-line boosting of Oza and Russell.
 *
 * <p>See details in:<br /> N. Oza and S. Russell. Online bagging and boosting.
 * In Artiﬁcial Intelligence and Statistics 2001, pages 105–112. Morgan
 * Kaufmann, 2001.</p> <p>For the boosting method, Oza and Russell note that the
 * weighting procedure of AdaBoost actually divides the total example weight
 * into two halves – half of the weight is assigned to the correctly classiﬁed
 * examples, and the other half goes to the misclassiﬁed examples. They use the
 * Poisson distribution for deciding the random probability that an example is
 * used for training, only this time the parameter changes according to the
 * boosting weight of the example as it is passed through each model in
 * sequence.</p>
 *
 * <p>Parameters:</p> <ul> <li>-l : Classiﬁer to train</li> <li>-s : The number
 * of models to boost</li> <li>-p : Boost with weights only; no poisson</li>
 * </ul>
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class OzaBoostByDu2 extends AbstractClassifier implements MultiClassClassifier {

    private static final long serialVersionUID = 1L;

    @Override
    public String getPurposeString() {
        return "Incremental on-line boosting of Oza and Russell.";
    }

    public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
            "Classifier to train.", Classifier.class, "trees.HoeffdingTree");

    public IntOption ensembleSizeOption = new IntOption("ensembleSize", 'e',
            "The number of models to boost.", 10, 1, Integer.MAX_VALUE);

    public FlagOption pureBoostOption = new FlagOption("pureBoost", 'p',
            "Boost with weights only; no poisson.");
    
    //random seed for generating poisson distribution
    public IntOption randomSeedOption = new IntOption("randomSeed", 'r',
            "The random seed for poisson.", 1, 1, Integer.MAX_VALUE);
    
    
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
    
    

    protected Classifier[] ensemble;

    protected double[] scms;

    protected double[] swms;

    @Override
    public void resetLearningImpl() {
        this.ensemble = new Classifier[this.ensembleSizeOption.getValue()];
        Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
        baseLearner.resetLearning();
        for (int i = 0; i < this.ensemble.length; i++) {
            this.ensemble[i] = baseLearner.copy();
        }
        this.scms = new double[this.ensemble.length];
        this.swms = new double[this.ensemble.length];
        //set random seed
        this.classifierRandom.setSeed(this.randomSeedOption.getValue());
        
        //******************************************************
        this.evaluateModel.evaluateInitialize(this.instanceSizeOption.getValue(), this.concpetOption.getValue(), this.slideWindowOption.isSet(), this.slidwindowsizeOption.getValue());
        //******************************************************
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        double lambda_d = 1.0;
        for (int i = 0; i < this.ensemble.length; i++) {
            double k = this.pureBoostOption.isSet() ? lambda_d : MiscUtils.poisson(lambda_d, this.classifierRandom);
            if (k > 0.0) {
                Instance weightedInst = (Instance) inst.copy();
                weightedInst.setWeight(inst.weight() * k);
                this.ensemble[i].trainOnInstance(weightedInst);
            }
            if (this.ensemble[i].correctlyClassifies(inst)) {
                this.scms[i] += lambda_d;
                lambda_d *= this.trainingWeightSeenByModel / (2 * this.scms[i]);
            } else {
                this.swms[i] += lambda_d;
                lambda_d *= this.trainingWeightSeenByModel / (2 * this.swms[i]);
            }
        }
    }

    protected double getEnsembleMemberWeight(int i) {
        double em = this.swms[i] / (this.scms[i] + this.swms[i]);
        if ((em == 0.0) || (em > 0.5)) {
            return 0.0;
        }
        
        
        double Bm = em / (1.0 - em);
        return Math.log(1.0 / Bm);
    }

    public double[] getVotesForInstance(Instance inst) {
        DoubleVector combinedVote = new DoubleVector();
        for (int i = 0; i < this.ensemble.length; i++) {
            double memberWeight = getEnsembleMemberWeight(i);
            if (memberWeight > 0.0) {
                DoubleVector vote = new DoubleVector(this.ensemble[i].getVotesForInstance(inst));
                if (vote.sumOfValues() > 0.0) {
                    vote.normalize();
                    vote.scaleValues(memberWeight);
                    combinedVote.addValues(vote);
                }
            } else {
                break;
            }
        }
        
        //****************************************************************
        //Evaluation Only		
        this.evaluateModel.evaluation(inst, combinedVote);
        if(this.evaluateModel.getNumOfExamplesReceived() == this.instanceSizeOption.getValue()) {
        	try {
        		this.output.fullToXls(this.evaluateModel.getResults(), this.outputFileOption.getValue(), this.runTimesOption.getValue());
        	} catch (IOException e) {
        		// TODO Auto-generated catch block
        		e.printStackTrace();
        	}
        	
        	List<AbstractOption> options = new ArrayList<AbstractOption>();
			options.add(this.ensembleSizeOption);
			
			try {
				this.output.MeasurementToTxt(this.evaluateModel.getMeasurement(), this.outputFileOption.getValue(), options);
				//this.output.MeasurementGMeanToTxt(this.outputFileOption.getValue(), this.evaluateModel.getGMean());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        //****************************************************************
        
        return combinedVote.getArrayRef();
    }

    public boolean isRandomizable() {
        return true;
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[]{new Measurement("ensemble size",
                    this.ensemble != null ? this.ensemble.length : 0)};
    }

    @Override
    public Classifier[] getSubClassifiers() {
        return this.ensemble.clone();
    }
}
