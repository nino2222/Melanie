package Others;
/**
 * 
 */


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import com.github.javacliparser.AbstractOption;

/**
 * @author hd168
 *
 */
public class Output implements Serializable{
	
	protected List<Double> results;
	protected String fileName;
	protected int rowNum;
	protected int colNum;
	
	public Output() {
		this.rowNum = 0;
		this.colNum = 0;
		
	}

	public void toText(List<Double> results, String fileName, int runTimes) throws IOException {
    	String file_name = fileName;
    	List<Double> pastData = new ArrayList<Double>(); 

    	File file = new File(file_name);
    	if(!file.exists()) {
    		file.createNewFile();
    	}
    	
    	FileWriter out = new FileWriter(file_name, true); 
    	
    	/*for(double r: results) {
    		out.write(r + ",\n");
    	}*/
    	out.write(results.get(results.size() - 1) + ",");
    	
    	out.flush();
    	out.close();
    }
	
	public void toXLS(List<Double> results, String fileName, int runTimes) throws IOException {
		this.colNum = runTimes - 1;
		File file = new File(fileName);
		HSSFWorkbook wb = new HSSFWorkbook();
		if(!file.exists()) {
    		file.createNewFile();
    		HSSFSheet sheet = wb.createSheet("sheet0");
    		FileOutputStream output=new FileOutputStream(fileName);
    		wb.write(output);	
    	}
		
		POIFSFileSystem poifsFileSystem = new POIFSFileSystem(new FileInputStream(file));
		wb = new HSSFWorkbook(poifsFileSystem);
		HSSFSheet sheet = wb.getSheet("sheet0");
		HSSFRow row;
		if(this.colNum == 0) {
			row = sheet.createRow(this.rowNum);
		}else {
			row = sheet.getRow(this.rowNum);
		}
		
		this.rowNum++;
		row.createCell(this.colNum).setCellValue(results.get(results.size() - 1));
		
		FileOutputStream output=new FileOutputStream(fileName);
		wb.write(output);
		output.flush();
	}
	
	public void fullToXls(List<Double> results, String fileName, int runTimes) throws IOException {
		this.colNum = runTimes - 1;
		File file = new File(fileName);
		HSSFWorkbook wb = new HSSFWorkbook();
		if(!file.exists()) {
    		file.createNewFile();
    		HSSFSheet sheet = wb.createSheet("sheet0");
    		FileOutputStream output=new FileOutputStream(fileName);
    		wb.write(output);	
    	}
		
		POIFSFileSystem poifsFileSystem = new POIFSFileSystem(new FileInputStream(file));
		wb = new HSSFWorkbook(poifsFileSystem);
		HSSFSheet sheet = wb.getSheet("sheet0");
		HSSFRow row;
		if(this.colNum == 0) {
			for(int i = 0; i < results.size(); i++) {
				row = sheet.createRow(i);
				row.createCell(this.colNum).setCellValue(results.get(i));
			}
		}else {
			for(int i = 0; i < results.size(); i++) {
				row = sheet.getRow(i);
				row.createCell(this.colNum).setCellValue(results.get(i));
			}
		}
		
		FileOutputStream output=new FileOutputStream(fileName);
		wb.write(output);
		output.flush();	
	}
	
	
	public void MeasurementToTxt(double[] results, String fileName, List<AbstractOption> options) throws IOException {
		String file_name = fileName + ".txt";
    	
    	File file = new File(file_name);
    	
    	if(!file.exists()) {
    		file.createNewFile();
    	}
    	
    	FileWriter out = new FileWriter(file_name, true); 
    	for(AbstractOption option: options) {
    		out.write(option.getName() + ": " + option.getValueAsCLIString() + " ");
    	}	
    	out.write(" Mean: " + String.valueOf(results[0]));
    	out.write(" Sqrt: " + String.valueOf(results[1]));
    	out.write("\n");
    	
    	out.flush();
    	out.close();
	}
	
	public void MeasurementGMeanToTxt(String fileName, double g_mean) throws IOException {
		String file_name = fileName + ".txt";
    	
    	File file = new File(file_name);
    	
    	if(!file.exists()) {
    		file.createNewFile();
    	}
    	
    	FileWriter out = new FileWriter(file_name, true); 
    	out.write(" G-MEAN: " + String.valueOf(g_mean));
    	out.write("\n");
    	
    	out.flush();
    	out.close();
	
	}
}
