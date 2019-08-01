package com.rezgateway.automation.tourmapper;

import org.testng.annotations.Test;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.Parameters;

import com.rezgateway.automation.JavaHttpHandler;
import com.rezgateway.automation.builder.request.AvailabilityRequestBuilder;
import com.rezgateway.automation.pojo.AvailabilityRequest;
import com.rezgateway.automation.pojo.AvailabilityResponse;
import com.rezgateway.automation.pojo.HttpResponse;
import com.rezgateway.automation.reader.response.AvailabilityResponseReader;
import com.rezgateway.automation.reports.ExtentTestNGReportBuilderExt;
import com.rezgateway.automation.xmlout.utill.DataLoader;
import com.rezgateway.automation.xmlout.utill.ExcelDataSingleton;

public class AV_SC_ID48_TEST extends ExtentTestNGReportBuilderExt{

	AvailabilityResponse AvailabilityResponse = new AvailabilityResponse();
	AvailabilityRequest AviRequest = new AvailabilityRequest();

	
	
	//Hotel id Search for No Arrival date

	@Parameters("TestUrl")
	@Test(priority = 0)
	public synchronized void availbilityTest(String TestUrl) throws Exception {

		getAvailabilityData();
		
		String Scenario     = AviRequest.getScenarioID();
		String HotelCode    = Arrays.toString(AviRequest.getCode());
		String SearchString = AviRequest.getCheckin()+"|"+AviRequest.getCheckout()+" |"+AviRequest.getNoOfRooms()+"R| "+AviRequest.getUserName()+" |"+AviRequest.getPassword();
		
		ITestResult result = Reporter.getCurrentTestResult();
		String testname = "Test Scenario:" + Scenario + " : Search By : " + AviRequest.getSearchType() + " Code : " + HotelCode+ "Criteria : "+SearchString;
		result.setAttribute("TestName", " Testing the Blackout is Working correctly for this hotel " + testname );
		result.setAttribute("Expected", " Results Should not be available Due to No Arrival date ");

		JavaHttpHandler handler = new JavaHttpHandler();
		HttpResponse Response = handler.sendPOST(TestUrl, "xml=" + new AvailabilityRequestBuilder().buildRequest("Resources/RegressionSuite_AvailRequest_senarioID_" + AviRequest.getScenarioID() + ".xml", AviRequest));

		if (Response.getRESPONSE_CODE() == 200) {

			AvailabilityResponse = new AvailabilityResponseReader().getResponse(Response.getRESPONSE());

			if (AvailabilityResponse.getHotelCount() > 0) {
				result.setAttribute("Actual", "Results Available then Test is fail , Hotel Count :" + AvailabilityResponse.getHotelCount());
				Assert.fail("Results are availble then Test is fail	");
			} else {
				result.setAttribute("Actual", "Results not available Error Code then Test is Pass  :" + AvailabilityResponse.getErrorCode() + " Error Desc :" + AvailabilityResponse.getErrorDescription());
				
			}
		} else {
			result.setAttribute("Actual", "No Response recieved Code :" + Response.getRESPONSE_CODE());
			Assert.fail("Invalid Response Code :" + Response.getRESPONSE_CODE() + " ,No Response received");
		}
	}


	public AvailabilityRequest getAvailabilityData() throws Exception {
		
		DataLoader loader = new DataLoader();
		AviRequest = loader.getReservationReqObjList(ExcelDataSingleton.getInstance("Resources/Full Regression Testing Checklist.xls", "Scenario").getDataHolder())[47][0];
		return AviRequest;

	}

}
