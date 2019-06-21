package Others;
/**
 * 
 */


import java.util.ArrayList;
import java.util.List;
import com.yahoo.labs.samoa.instances.Instance;
import moa.core.DoubleVector;
import java.io.Serializable;

/**
 * @author hd168
 *
 */
public class DuEvaluateModel implements Serializable{
	
	protected int instanceSize;
    protected int conceptPosition;
    protected boolean slideWindow;
    protected int slideWindowSize;
    
    protected int count;
    protected int correctTimes;
    protected int numOfExamplesReceived;
    
    protected List<Double> evaAccuracy;
    protected List<Double> evaSlideAccuracy;
    protected List<Boolean> results;
    
	protected double tp = 0.0;
	protected double fn = 0.0;
	protected double fp = 0.0;
	protected double tn = 0.0;
	protected double g_mean = 0.0;
    
    public DuEvaluateModel() {
    	
    }
    
    
    public void evaluateInitialize(int instanceSize, int conceptPosition, boolean slideWindow, int slideWindowSize) {
		this.instanceSize = instanceSize;
		this.conceptPosition = conceptPosition;
		this.slideWindow = slideWindow;
		this.slideWindowSize = slideWindowSize;
		
		this.count = 0;
		this.correctTimes = 0;
		this.numOfExamplesReceived = 0;
		this.evaAccuracy = new ArrayList<Double>();
		this.evaSlideAccuracy = new ArrayList<Double>();
		this.results = new ArrayList<Boolean>();
	}

    
    public void evaluation(Instance inst, DoubleVector prediction) {
    	this.numOfExamplesReceived++;
    	//Calculate G-Mean
    	//this.evaluationWithGMean(inst, prediction);
    	//++++++++=
    	if(this.slideWindow)
    		this.evaluationWithSlideWindow(inst, prediction);
    	else
    		this.evaluationWithAccuracy(inst, prediction);
    }
    
    protected void evaluationWithGMean(Instance inst, DoubleVector prediction) {
    	if(inst.classValue() == 0.0) {
    		if(prediction.maxIndex() == (int)inst.classValue())
    			this.tp++;
    		else
    			this.fn++;
    		
    	}else {
    		if(prediction.maxIndex() == (int)inst.classValue())
    			this.tn++;
    		else
    			this.fp++; 		
    	}
    	
    	this.g_mean = Math.sqrt((tp/(tp+fn))*(tn/(tn+fp)));

    }
    
    protected void evaluationWithAccuracy(Instance inst, DoubleVector prediction) {
		this.count++;
		//if(this.count == this.conceptPosition) {
		if(this.numOfExamplesReceived % this.conceptPosition == 0) {
			this.correctTimes = 0;
			this.count = 1;
		}		
		if(prediction.maxIndex() == (int)inst.classValue()) {
			this.correctTimes++;
		}		
		this.evaAccuracy.add((double)this.correctTimes/(double)this.count);
		//System.out.println((double)this.correctTimes/(double)this.count);
	}
    
    
    protected void evaluationWithSlideWindow(Instance inst, DoubleVector prediction) {   	
    	if(prediction.maxIndex() == (int)inst.classValue())
			this.results.add(true);
		else
			this.results.add(false);
    	
    	int cTimes = 0;
		if(this.results.size() < this.slideWindowSize) {
			for(boolean p: this.results) {
				if(p)
					cTimes++;
			}
			this.evaSlideAccuracy.add((double)cTimes/(double)this.results.size());
		}else {
			List<Boolean> tempResults = this.results.subList(this.results.size() - this.slideWindowSize + 1, this.results.size() - 1);
			for(boolean p: tempResults) {
				if(p)
					cTimes++;
			}
			this.evaSlideAccuracy.add((double)cTimes/(double)this.slideWindowSize);
		}
		//System.out.println(this.evaSlideAccuracy.get(this.evaSlideAccuracy.size() - 1));
	}
    
    public List<Double> getResults(){
    	if(this.slideWindow)
    		return this.getEvaSlideAccuracy();
    	else
    		return this.getEvaAccuracy();
    }
    
    public double getGMean() {
    	return this.g_mean;
    }
    
    public List<Double> getEvaAccuracy(){
    	return this.evaAccuracy;
    }
    
    public List<Double> getEvaSlideAccuracy(){
    	return this.evaSlideAccuracy;
    }
    
    public int getNumOfExamplesReceived() {
    	return this.numOfExamplesReceived;
    }
    
    public void addNumOfExamplesReceived() {
    	this.numOfExamplesReceived++;
    }
    
    public double[] getMeasurement() {
    	double[] results = new double[2];
    	results[0] = 0;
    	results[1] = 0;
    	List<Double> accuracy = this.getResults();
    	
    	
    	for(double r: accuracy) {
    		results[0] += r; 
    	}
    	
    	results[0] = results[0]/(accuracy.size() - 1);
    	//System.out.println(results[0]);
    	
    	for(double r: accuracy) {
    		results[1] += Math.pow(r - results[0], 2); 
    	}
    	
    	results[1] = Math.sqrt(results[1]/(accuracy.size() - 1));
    	//System.out.println(results[1]);
    	
    	return results;    	
    }

}
