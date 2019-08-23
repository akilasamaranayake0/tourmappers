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

public class AV_SC_ID49_TEST extends ExtentTestNGReportBuilderExt{

	AvailabilityResponse AvailabilityResponse = new AvailabilityResponse();
	AvailabilityRequest AviRequest = new AvailabilityRequest();

	
	
	//Hotel id Search – Min Nights


	@Parameters("TestUrl")
	@Test(priority = 0)
	public synchronized void availbilityTestForMinNightCondition(String TestUrl) throws Exception {

		getAvailabilityData();
		
		String Scenario     = AviRequest.getScenarioID();
		String HotelCode    = Arrays.toString(AviRequest.getCode());
		String SearchString = AviRequest.getCheckin()+"|"+AviRequest.getCheckout()+" |"+AviRequest.getNoOfRooms()+"R| "+AviRequest.getUserName()+" |"+AviRequest.getPassword();
		
		ITestResult result = Reporter.getCurrentTestResult();
		String testname = "Test Scenario:" + Scenario + " : Search By : " + AviRequest.getSearchType() + " Code : " + HotelCode+ "Criteria : "+SearchString;
		result.setAttribute("TestName", "Testing the Min night Restriction :"+ testname);
		result.setAttribute("Expected", "Results Should NOT be available due to Min night Restriction");

		JavaHttpHandler handler = new JavaHttpHandler();
		HttpResponse Response = handler.sendPOST(TestUrl, "xml=" + new AvailabilityRequestBuilder().buildRequest("Resources/RegressionSuite_AvailRequest_senarioID_" + AviRequest.getScenarioID() + ".xml", AviRequest));

		if (Response.getRESPONSE_CODE() == 200) {

			AvailabilityResponse = new AvailabilityResponseReader().getResponse(Response.getRESPONSE());

			if ("A6".equals(AvailabilityResponse.getErrorCode())) {
				result.setAttribute("Actual", "Results not available :" + AvailabilityResponse.getErrorCode() + " Error Desc :" + AvailabilityResponse.getErrorDescription());
			} else {
				result.setAttribute("Actual", " Error occured -  Minnight condition is not apply for this hotel " );
				Assert.fail(" Error occured -  Minnight condition is not apply for this hotel ");
			}
		} else {
			result.setAttribute("Actual", "No Response recieved Code :" + Response.getRESPONSE_CODE());
			Assert.fail("Invalid Response Code :" + Response.getRESPONSE_CODE() + " ,No Response received");
		}
	}

	public AvailabilityRequest getAvailabilityData() throws Exception {

		DataLoader loader = new DataLoader();
		AviRequest = loader.getReservationReqObjList(ExcelDataSingleton.getInstance("Resources/Full Regression Testing Checklist.xls", "Scenario").getDataHolder())[48][0];
		return AviRequest;

	}

}
