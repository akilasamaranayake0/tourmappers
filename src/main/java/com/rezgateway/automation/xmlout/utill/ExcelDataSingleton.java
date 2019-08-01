package com.rezgateway.automation.xmlout.utill;

import com.rezgateway.automation.input.ExcelReader;


public class ExcelDataSingleton {

	
    private static ExcelDataSingleton instance;
    private String[][] DataHolder ;
    private ExcelDataSingleton(){}
    
    public static synchronized ExcelDataSingleton getInstance(String FileNme,String Sheet) throws Exception{
        if(instance == null){
            instance = new ExcelDataSingleton();
            instance.setDataHolder(new ExcelReader().getExcelData(FileNme,Sheet));
        }
        return instance;
    }

	/**
	 * @return the dataHolder
	 */
	public String[][] getDataHolder() {
		return DataHolder;
	}

	/**
	 * @param dataHolder the dataHolder to set
	 */
	public void setDataHolder(String[][] dataHolder) {
		DataHolder = dataHolder;
	}
    
    
    
    
    
    
}