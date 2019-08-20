package com.rezgateway.automation.tourmapper;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.Parameters;

import com.rezgateway.automation.JavaHttpHandler;
import com.rezgateway.automation.builder.request.AvailabilityRequestBuilder;
import com.rezgateway.automation.enu.ConfirmationType;
import com.rezgateway.automation.pojo.AvailabilityRequest;
import com.rezgateway.automation.pojo.AvailabilityResponse;
import com.rezgateway.automation.pojo.DailyRates;
import com.rezgateway.automation.pojo.Hotel;
import com.rezgateway.automation.pojo.HttpResponse;
import com.rezgateway.automation.pojo.RateplansInfo;
import com.rezgateway.automation.pojo.Room;
import com.rezgateway.automation.reader.response.AvailabilityResponseReader;
import com.rezgateway.automation.reports.ExtentTestNGReportBuilderExt;
import com.rezgateway.automation.xmlout.utill.DataLoader;
import com.rezgateway.automation.xmlout.utill.ExcelDataSingleton;

public class AV_SC_ID34_TEST extends ExtentTestNGReportBuilderExt{

	AvailabilityResponse AvailabilityResponse = new AvailabilityResponse();
	AvailabilityRequest AviRequest = new AvailabilityRequest();

	@Parameters("TestUrl")
	@Test(priority = 0)
	public synchronized void availbilityTest(String TestUrl) throws Exception {

		getAvailabilityData();
		
		String Scenario     = AviRequest.getScenarioID();
		String HotelCode    = Arrays.toString(AviRequest.getCode());
		String SearchString = AviRequest.getCheckin()+"|"+AviRequest.getCheckout()+" |"+AviRequest.getNoOfRooms()+"R| "+AviRequest.getUserName()+" |"+AviRequest.getPassword();
				
		ITestResult result = Reporter.getCurrentTestResult();
		String testname = "Test Scenario:" + Scenario + " : Search By : " + AviRequest.getSearchType() + " Code : " + HotelCode+ "Criteria : "+SearchString;
		result.setAttribute("TestName", testname);
		result.setAttribute("Expected", "Results Should be available");

		JavaHttpHandler handler = new JavaHttpHandler();
		HttpResponse Response = handler.sendPOST(TestUrl, "xml=" + new AvailabilityRequestBuilder().buildRequest("Resources/RegressionSuite_AvailRequest_senarioID_" + AviRequest.getScenarioID() + ".xml", AviRequest));

		if (Response.getRESPONSE_CODE() == 200) {

			AvailabilityResponse = new AvailabilityResponseReader().getResponse(Response.getRESPONSE());

			if (AvailabilityResponse.getHotelCount() > 0) {
				result.setAttribute("Actual", "Results Available, Hotel Count :" + AvailabilityResponse.getHotelCount());
			} else {
				result.setAttribute("Actual", "Results not available Error Code :" + AvailabilityResponse.getErrorCode() + " Error Desc :" + AvailabilityResponse.getErrorDescription());
				Assert.fail("No Results Error Code :" + AvailabilityResponse.getErrorCode());
			}
		} else {
			result.setAttribute("Actual", "No Response recieved Code :" + Response.getRESPONSE_CODE());
			Assert.fail("Invalid Response Code :" + Response.getRESPONSE_CODE() + " ,No Response received");
		}
	}

	@Test(dependsOnMethods = "availbilityTest")
	public synchronized void isAvailabileHotelName() {

		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing Availability of the Hotel name in the response ");
		result.setAttribute("Expected", "Hotel Hotel name Should be Available in the Response ");

		if (!AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue().getName().isEmpty()) {
			result.setAttribute("Actual", "Hotel name is  : " + AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue().getName());
		} else {
			result.setAttribute("Actual", "Hotel Short Description is not Available in the Response : " + AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue().getName());
			Assert.fail("Hotel name is not Available in the Response");
		}

	}

	@Test( enabled = false)
	public synchronized void isAvailableShortDescription() {

		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing Availability of the Hotel Short Description in the response ");
		result.setAttribute("Expected", "Hotel Short Description Should be Available in the Response ");

		if (!AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue().getShortDescription().isEmpty()) {
			result.setAttribute("Actual", "Hotel Short Description is  : " + AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue().getShortDescription());
		} else {
			result.setAttribute("Actual", "Hotel Short Description is not Available in the Response  : \n" + AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue().getShortDescription());
			Assert.fail("Hotel Short Description is not Available in the Response");
		}

	}

	@Test(dependsOnMethods = "availbilityTest")
	public synchronized void isAvailableThumbNailUrl() {

		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing Availability of the Hotel thumbNail Url in the response ");
		result.setAttribute("Expected", "Hotel thumbNail Url Should be Available in the Response ");

		if (!AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue().getThumbNailUrl().isEmpty()) {
			result.setAttribute("Actual", "Hotel thumbNail Url is  " + AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue().getThumbNailUrl());
		} else {
			result.setAttribute("Actual", "Hotel thumbNail Url is not Available in the Response" + AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue().getThumbNailUrl());
			Assert.fail("Hotel thumbNail Url is not Available in the Response");
		}

	}

	@Test(dependsOnMethods = "isAvailableThumbNailUrl")
	public synchronized void isThumbNailUrlHaveS3() {

		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing Availability s3.amazonaws.com ");
		result.setAttribute("Expected", "Hotel thumbNail Url Should be Available in the Response ");

		if (AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue().getThumbNailUrl().contains("http://s3.amazonaws.com")) {
			result.setAttribute("Actual", "Thumnail image is exsist in the s3.amazonaws.com");
		} else {
			result.setAttribute("Actual", "Thumnail image is not exsist in the Amazon Servers " + AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue().getThumbNailUrl());
			Assert.fail("Thumnail image is not exsist in the Amazon Servers");
		}

	}

	/*
	 * @Test(dependsOnMethods = "availbilityTest") public synchronized void
	 * testHotelCode() {
	 * 
	 * ITestResult result = Reporter.getCurrentTestResult();
	 * result.setAttribute("TestName", "Testing Hotel Code ");
	 * result.setAttribute("Expected", "Hotel Code should be  : " +
	 * AviRequest.getCode()[0]);
	 * 
	 * if (AvailabilityResponse.getHotelList().containsKey(AviRequest.getCode()[0]))
	 * { result.setAttribute("Actual",
	 * AvailabilityResponse.getHotelList().entrySet().iterator().next().getKey()); }
	 * else { result.setAttribute("Actual",
	 * "User entered Hotel Code is not in the Response : Actual Hotel Code is " +
	 * AvailabilityResponse.getHotelList().entrySet().iterator().next().getKey());
	 * Assert.fail("User entered Hotel Code is not in the Response:"); }
	 * 
	 * }
	 */

	@Test(dependsOnMethods = "availbilityTest")
	public synchronized void testDayWiseRateAvailability() {

		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing Daily Rates having for Each date in the Response ");
		result.setAttribute("Expected", "System shouls contain the  Daily Rate for each day");
		try {

			Hotel hotelInResponse = AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue();
			Map<String, ArrayList<Room>> rooms = hotelInResponse.getRoomInfo();
			TreeMap<String, DailyRates> dailyRates = new TreeMap<String, DailyRates>();
			ArrayList<String> flag = new ArrayList<String>();

			for (Room room : rooms.entrySet().iterator().next().getValue()) {

				Map<String, RateplansInfo> RatesPlanInfos = room.getRatesPlanInfo();
				dailyRates = RatesPlanInfos.entrySet().iterator().next().getValue().getDailyRates();

				if (Integer.parseInt(AviRequest.getNoofNights()) == dailyRates.size()) {
					flag.add("true");
				} else {
					flag.add("false");
				}
			}

			if (flag.contains("false")) {
				result.setAttribute("Actual", "Daily rates are not availble for Some room");
				Assert.fail("Daily rates are not availble for Some room");
			} else {
				result.setAttribute("Actual", "Daily rates are availble for All the rooms in the Response ");
			}

		} catch (Exception e) {
			System.out.println(e);
			result.setAttribute("Actual", e);
			Assert.fail("This testDayWiseRateAvailability is Failed due to :", e);
		}

	}
	
	@Test(dependsOnMethods = "availbilityTest")
	public synchronized void testNumOfRoomsAvailbility() {

		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing NumOfRooms Availbility in the Response ");
		result.setAttribute("Expected", "Response's room count should equal to requested Room count ");
		try {

			Hotel hotelInResponse = AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue();
			Map<String, ArrayList<Room>> rooms = hotelInResponse.getRoomInfo();
			if (Integer.parseInt(AviRequest.getNoOfRooms()) == rooms.entrySet().size()) {
				result.setAttribute("Actual", "No of Rooms are Correctly Exsist in the Availability Response");
			} else {
				result.setAttribute("Actual", "Response's Room count is not equal to the Reqested Room count");
				Assert.fail("Response's Room count is not equal to the Reqested Room count");
			}
		} catch (Exception e) {
			System.out.println(e);
			result.setAttribute("Actual", e);
			Assert.fail("This testNumOfRoomsAvailbility is Failed due to :", e);
		}
	}

	@Test(dependsOnMethods = "availbilityTest")
	public synchronized void isHotelOnRequested(){

		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing is this Hotel on OnRequest status For this Checkin Checkout dates ");
		result.setAttribute("Expected", "This Hotel Should not on OnRequest status For this Checkin Checkout dates ");
		try{
			Hotel hotelInResponse = AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue();
			Map<String, ArrayList<Room>> rooms = hotelInResponse.getRoomInfo();
			ArrayList<String> flag = new ArrayList<String>();
			for(Room r : rooms.entrySet().iterator().next().getValue() ){
				if(ConfirmationType.CON == r.getConType()){
					flag.add("CON");
				}else {
					flag.add("REQ");
				}
			}
			if (flag.contains("REQ")) {
				result.setAttribute("Actual", "Some Hotel rooms are On requested ");
				Assert.fail("Some Hotel rooms are On requested ");
			} else {
				result.setAttribute("Actual", "All hotel Rooms Confirmation status is CON ");
			}
			
			
		}catch(Exception e){
			System.out.println(e);
			result.setAttribute("Actual", e);
			Assert.fail("This testNumOfRoomsAvailbility is Failed due to :", e);
		}
		
		
	}

	public AvailabilityRequest getAvailabilityData() throws Exception {

		DataLoader loader = new DataLoader();
		AviRequest = loader.getReservationReqObjList(ExcelDataSingleton.getInstance("Resources/Full Regression Testing Checklist.xls", "Scenario").getDataHolder())[33][0];
		return AviRequest;

	}

}
