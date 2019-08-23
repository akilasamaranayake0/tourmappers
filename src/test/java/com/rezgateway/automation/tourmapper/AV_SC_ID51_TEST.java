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

public class AV_SC_ID51_TEST extends ExtentTestNGReportBuilderExt{

	AvailabilityResponse AvailabilityResponse = new AvailabilityResponse();
	AvailabilityRequest AviRequest = new AvailabilityRequest();
	
	//Hotel id Search – Applying Promotions (Free Night)

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
	public synchronized void testPrmotionCodeAvailability() {

		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing the availability of Free night promotion code in the Response ");
		result.setAttribute("Expected", "Respone should contain a free night promotion code");
		boolean isPromoAvail = false;
		
		String expRoomType = "1 Bdrm City View";
		String expRatePlan ="with CONT with 5 pax";
		String expPromocode = "Stay 3 pay 2";
		
		try {

			Hotel hotelInResponse = AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue();
			
			Map<String, ArrayList<Room>> rooms = hotelInResponse.getRoomInfo();
			
			for (Room room : rooms.entrySet().iterator().next().getValue()) {
				Map<String, RateplansInfo> RatesPlanInfos = room.getRatesPlanInfo();
				
				String roomType = room.getRoomType();
				String ratePlan = RatesPlanInfos.entrySet().iterator().next().getValue().getRatePlan();
				String promoCode = room.getPromotionCode();
				
				if(expRoomType.equals(roomType) && expRatePlan.equals(ratePlan) && expPromocode.equals(promoCode)) {
					isPromoAvail = true;
				}
			}

			if (isPromoAvail) {
				result.setAttribute("Actual", "Free Night promotion code : " + expPromocode + " is availble for room type : " + expRoomType + " ~ Rate Plan : " + expRatePlan);
			} else {
				result.setAttribute("Actual", "Free Night promotion code : " + expPromocode + " is not availble for room type : " + expRoomType + " ~ Rate Plan : " + expRatePlan);
				Assert.fail("Fail : Free Night promotion is not availble for the specific room");
			}

		} catch (Exception e) {
			System.out.println(e);
			result.setAttribute("Actual", e);
			Assert.fail("This testPrmotionCodeAvailability is Failed due to :", e);
		}

	}
	
	@Test(dependsOnMethods = "testPrmotionCodeAvailability")
	public synchronized void testFreeNightsAvailability() {

		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing the Maximum No of Free nights condition in the Response ");
		result.setAttribute("Expected", "Respone should contain correct number of free nights");
		ArrayList<String> flag = new ArrayList<String>();
		boolean isTCPass = false;
		
		String expRoomType = "1 Bdrm City View";
		String expRatePlan ="with CONT with 5 pax";
		String expBedType = "Single";
		//int maxNights = 2;
		int noOfnights = 0;
		
		try {

			Hotel hotelInResponse = AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue();
			
			Map<String, ArrayList<Room>> rooms = hotelInResponse.getRoomInfo();
			TreeMap<String, DailyRates> dailyRates = new TreeMap<String, DailyRates>();
			
			for (Room room : rooms.entrySet().iterator().next().getValue()) {
				Map<String, RateplansInfo> RatesPlanInfos = room.getRatesPlanInfo();
				
				String roomType = room.getRoomType();
				String ratePlan = RatesPlanInfos.entrySet().iterator().next().getValue().getRatePlan();
				String bedType = room.getBedType();
				
				if(expRoomType.equals(roomType) && expRatePlan.equals(ratePlan) && expBedType.equals(bedType)) {
					dailyRates = RatesPlanInfos.entrySet().iterator().next().getValue().getDailyRates();
					noOfnights = dailyRates.size();
					
					for(Map.Entry<String,DailyRates> dailyRate : dailyRates.entrySet()) {
						DailyRates dailyRatesObject = dailyRate.getValue();
						double total = dailyRatesObject.getTotal();
										
						if(total == 0) {
							flag.add("FN");
						}
						
					}
				}
			}

			if(noOfnights < 3) {
				if (flag.size() == 0) {
					isTCPass = true;
				}
			}if(noOfnights >= 3 && noOfnights < 6) {
				if (flag.contains("FN") && flag.size() == 1) {
					isTCPass = true;
				}
			}else if (noOfnights >= 6) {
				if (flag.contains("FN") && flag.size() == 2) {
					isTCPass = true;
				}
			}
				
			
			if (isTCPass) {
				result.setAttribute("Actual", "No of Free Nights availble for room type : " + expRoomType + " ~ Rate Plan : " + expRatePlan + "~ BedType : " + expBedType + " ~ for Number of Nights : " + noOfnights + " is : " + flag.size());
			} else {
				result.setAttribute("Actual", "No of Free Nights availble for room type : " + expRoomType + " ~ Rate Plan : " + expRatePlan + "~ BedType : " + expBedType + " ~ for Number of Nights : " + noOfnights + " is : " + flag.size());
				Assert.fail("Fail : Free Nights are not availble for the specific room");
			}

		} catch (Exception e) {
			System.out.println(e);
			result.setAttribute("Actual", e);
			Assert.fail("This testFreeNightsAvailability is Failed due to :", e);
		}

	}
	
	
	public AvailabilityRequest getAvailabilityData() throws Exception {

		DataLoader loader = new DataLoader();
		AviRequest = loader.getReservationReqObjList(ExcelDataSingleton.getInstance("Resources/Full Regression Testing Checklist.xls", "Scenario").getDataHolder())[50][0];
		return AviRequest;

	}

}
