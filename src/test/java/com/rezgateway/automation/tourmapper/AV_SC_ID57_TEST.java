package com.rezgateway.automation.tourmapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.rezgateway.automation.JavaHttpHandler;
import com.rezgateway.automation.builder.request.AvailabilityRequestBuilder;
import com.rezgateway.automation.enu.ConfirmationType;
import com.rezgateway.automation.pojo.AvailabilityRequest;
import com.rezgateway.automation.pojo.AvailabilityResponse;
import com.rezgateway.automation.pojo.BookingPolicy;
import com.rezgateway.automation.pojo.DailyRates;
import com.rezgateway.automation.pojo.Hotel;
import com.rezgateway.automation.pojo.HttpResponse;
import com.rezgateway.automation.pojo.RateplansInfo;
import com.rezgateway.automation.pojo.Room;
import com.rezgateway.automation.reader.response.AvailabilityResponseReader;
import com.rezgateway.automation.xmlout.utill.DataLoader;
import com.rezgateway.automation.xmlout.utill.ExcelDataSingleton;

public class AV_SC_ID57_TEST {

	AvailabilityResponse AvailabilityResponse = new AvailabilityResponse();
	AvailabilityRequest AviRequest = new AvailabilityRequest();

	// Hotel id Search - Applying Cancellation Policy

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
		result.setAttribute("Expected", "Hotel name Should be Available in the Response ");

		if (!AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue().getName().isEmpty()) {
			result.setAttribute("Actual", "Hotel name is  : " + AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue().getName());
		} else {
			result.setAttribute("Actual", "Hotel Short Description is not Available in the Response : " + AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue().getName());
			Assert.fail("Hotel name is not Available in the Response");
		}

	}

	@Test(dependsOnMethods = "availbilityTest")
	public synchronized void testHotelCode() {

		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing Hotel Code ");
		result.setAttribute("Expected", "Hotel Code should be  : " + AviRequest.getCode()[0]);

		if (AvailabilityResponse.getHotelList().containsKey(AviRequest.getCode()[0])) {
			result.setAttribute("Actual", AvailabilityResponse.getHotelList().entrySet().iterator().next().getKey());
		} else {
			result.setAttribute("Actual", "User entered Hotel Code is not in the Response : Actual Hotel Code is " + AvailabilityResponse.getHotelList().entrySet().iterator().next().getKey());
			Assert.fail("User entered Hotel Code is not in the Response:");
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
	public synchronized void isHotelOnRequested() {

		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing is this Hotel on OnRequest status For this Checkin Checkout dates ");
		result.setAttribute("Expected", " Hotel should not in the OnRequest status For this Checkin Checkout dates ");
		try {
			Hotel hotelInResponse = AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue();
			Map<String, ArrayList<Room>> rooms = hotelInResponse.getRoomInfo();
			ArrayList<String> flag = new ArrayList<String>();
			for (Room r : rooms.entrySet().iterator().next().getValue()) {
				if (ConfirmationType.CON == r.getConType()) {
					flag.add("CON");
				} else {
					flag.add("REQ");
				}
			}
			if (flag.contains("REQ")) {
				result.setAttribute("Actual", "Some Hotel rooms are On requested ");
				Assert.fail("Some Hotel rooms are On requested ");
			} else {
				result.setAttribute("Actual", "All hotel Rooms Confirmation status is CON ");
			}

		} catch (Exception e) {
			System.out.println(e);
			result.setAttribute("Actual", e);
			Assert.fail("This testNumOfRoomsAvailbility is Failed due to :", e);
		}

	}

	// currently this test doing for static data multiple cancellation policy
	@Test(dependsOnMethods = "availbilityTest")
	public synchronized void testCancellationPolicy() {
		
		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing is applying Cancellation policy");
		result.setAttribute("Expected", "System should display cnx policy in Checkin Checkout dates ");
		
		try{
			Hotel hotelInResponse = AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue();
			Map<String, ArrayList<Room>> rooms = hotelInResponse.getRoomInfo();
			ArrayList<String> flag = new ArrayList<String>();
		
			for(Room r : rooms.entrySet().iterator().next().getValue() ){
							
			if(r.getRoomType().equals("King")){
				if(r.getRoomPolicy().size()==3){
					
					BookingPolicy bookingPolicy1 = r.getRoomPolicy().get(1);
					
					if(("2019-02-14".equals(bookingPolicy1.getPolicyFrom())&&("2020-02-14".equals(bookingPolicy1.getPolicyTo())))? flag.add("PolicyFromAndTo_True") : flag.add("PolicyFromAndTo_False"));
					if("Cancel".equals(bookingPolicy1.getAmendmentType())? flag.add("AmendmentType_True") : flag.add("AmendmentType_False"));
					if("percentage".equals(bookingPolicy1.getPolicyBasedOn())? flag.add("PolicyBasedOn_True") : flag.add("PolicyBasedOn_False"));
					if("50".equals(bookingPolicy1.getPolicyBasedOnValue())? flag.add("PolicyBasedOnValue_True"):flag.add("PolicyBasedOnValue_False"));
					if("Less Than".equals(bookingPolicy1.getArrivalRange())? flag.add("ArrivalRange_True"):flag.add("ArrivalRange_False"));
					if(70==(bookingPolicy1.getArrivalRangeValue()) ? (flag.add("getArrivalRangeValue_True")):(flag.add("getArrivalRange_False")));
					if("$262.5".equals(bookingPolicy1.getPolicyFee()) ? flag.add("PolicyFee_True") : flag.add("PolicyFee_True"));
					if("$262.50".equals(bookingPolicy1.getNoShowPolicyFee()) ? flag.add("NoShowPolicyFee_True") : flag.add("NoShowPolicyFee_False")  );
					
					BookingPolicy bookingPolicy2 = r.getRoomPolicy().get(0);
					
					if(("2019-02-14".equals(bookingPolicy2.getPolicyFrom())&&("2020-02-14".equals(bookingPolicy2.getPolicyTo())))? flag.add("PolicyFromAndTo_True") : flag.add("PolicyFromAndTo_False"));
					if("Cancel".equals(bookingPolicy2.getAmendmentType())? flag.add("AmendmentType_True") : flag.add("AmendmentType_False"));
					if("percentage".equals(bookingPolicy2.getPolicyBasedOn())? flag.add("PolicyBasedOn_True") : flag.add("PolicyBasedOn_False"));
					if("100".equals(bookingPolicy2.getPolicyBasedOnValue())? flag.add("PolicyBasedOn_True"):flag.add("PolicyBasedOn_False"));
					if("Less Than".equals(bookingPolicy2.getArrivalRange())? flag.add("ArrivalRange_True"):flag.add("ArrivalRange_False"));
					if(50==(bookingPolicy2.getArrivalRangeValue()) ? (flag.add("getArrivalRangeValue_True")):(flag.add("getArrivalRange_False")));
					if("$525.0".equals(bookingPolicy2.getPolicyFee()) ? flag.add("PolicyFee_True") : flag.add("PolicyFee_True"));
					if("$525.00".equals(bookingPolicy2.getNoShowPolicyFee()) ? flag.add("NoShowPolicyFee_True") : flag.add("NoShowPolicyFee_False")  );
				
					BookingPolicy bookingPolicy3 = r.getRoomPolicy().get(2);
					
					if(("2019-02-14".equals(bookingPolicy3.getPolicyFrom())&&("2020-02-14".equals(bookingPolicy3.getPolicyTo())))? flag.add("PolicyFromAndTo_True") : flag.add("PolicyFromAndTo_False"));
					if("Cancel".equals(bookingPolicy3.getAmendmentType())? flag.add("AmendmentType_True") : flag.add("AmendmentType_False"));
					if("percentage".equals(bookingPolicy3.getPolicyBasedOn())? flag.add("PolicyBasedOn_True") : flag.add("PolicyBasedOn_False"));
					if("80".equals(bookingPolicy3.getPolicyBasedOnValue())? flag.add("PolicyBasedOnValue_True"):flag.add("PolicyBasedOnValue_False"));
					if("Less Than".equals(bookingPolicy3.getArrivalRange())? flag.add("ArrivalRange_True"):flag.add("ArrivalRange_False"));
					if(60==(bookingPolicy3.getArrivalRangeValue()) ? (flag.add("getArrivalRangeValue_True")):(flag.add("getArrivalRange_False")));
					if("$420.0".equals(bookingPolicy3.getPolicyFee()) ? flag.add("PolicyFee_True") : flag.add("PolicyFee_True"));
					if("$420.00".equals(bookingPolicy3.getNoShowPolicyFee()) ? flag.add("NoShowPolicyFee_True") : flag.add("NoShowPolicyFee_False")  );
					
					
					
				    
				}
				else {
					result.setAttribute("Actual", " multiple policy not found for roomtype1");
					flag.add("False");
					Assert.fail("Cancellation policy not found ");
				}
					
			}
			
			
			else if(r.getRoomType().equals("HarborFront")){
				if(r.getRoomPolicy().size()==2){
					
					BookingPolicy bookingPolicyr1 = r.getRoomPolicy().get(0);
					
					if(("2019-02-14".equals(bookingPolicyr1.getPolicyFrom())&&("2020-02-14".equals(bookingPolicyr1.getPolicyTo())))? flag.add("PolicyFromAndTo_True") : flag.add("PolicyFromAndTo_False"));
					if("Cancel".equals(bookingPolicyr1.getAmendmentType())? flag.add("AmendmentType_True") : flag.add("AmendmentType_False"));
					if("nights".equals(bookingPolicyr1.getPolicyBasedOn())? flag.add("PolicyBasedOn_True") : flag.add("PolicyBasedOn_False"));
					if("1".equals(bookingPolicyr1.getPolicyBasedOnValue())? flag.add("PolicyBasedOnValue_True"):flag.add("PolicyBasedOnValue_False"));
					if("Less Than".equals(bookingPolicyr1.getArrivalRange())? flag.add("ArrivalRange_True"):flag.add("ArrivalRange_False"));
					if(30==(bookingPolicyr1.getArrivalRangeValue()) ? (flag.add("getArrivalRangeValue_True")):(flag.add("getArrivalRange_False")));
					if("$315.0".equals(bookingPolicyr1.getPolicyFee()) ? flag.add("PolicyFee_True") : flag.add("PolicyFee_True"));
					if("$630.00".equals(bookingPolicyr1.getNoShowPolicyFee()) ? flag.add("NoShowPolicyFee_True") : flag.add("NoShowPolicyFee_False")  );
					
					BookingPolicy bookingPolicyr2 = r.getRoomPolicy().get(1);
					
					if(("2019-02-14".equals(bookingPolicyr2.getPolicyFrom())&&("2020-02-14".equals(bookingPolicyr2.getPolicyTo())))? flag.add("PolicyFromAndTo_True") : flag.add("PolicyFromAndTo_False"));
					if("Cancel".equals(bookingPolicyr2.getAmendmentType())? flag.add("AmendmentType_True") : flag.add("AmendmentType_False"));
					if("value".equals(bookingPolicyr2.getPolicyBasedOn())? flag.add("PolicyBasedOn_True") : flag.add("PolicyBasedOn_False"));
					if("100.00".equals(bookingPolicyr2.getPolicyBasedOnValue())? flag.add("PolicyBasedOn_True"):flag.add("PolicyBasedOn_False"));
					if("Any".equals(bookingPolicyr2.getArrivalRange())? flag.add("ArrivalRange_True"):flag.add("ArrivalRange_False"));
					if(0==(bookingPolicyr2.getArrivalRangeValue()) ? (flag.add("getArrivalRangeValue_True")):(flag.add("getArrivalRange_False")));
					if("$100.00".equals(bookingPolicyr2.getPolicyFee()) ? flag.add("PolicyFee_True") : flag.add("PolicyFee_True"));
					if("$100.00".equals(bookingPolicyr2.getNoShowPolicyFee()) ? flag.add("NoShowPolicyFee_True") : flag.add("NoShowPolicyFee_False")  );
						
					
				    
				}
				else {
					result.setAttribute("Actual", " multiple policy not found for roomtype2");
					flag.add("False");
					Assert.fail("Cancellation policy not found ");
				}
			}
			
			else {
				result.setAttribute("Actual", " no room type found ");
				flag.add("False");
				Assert.fail("Cancellation policy not found belong to this room");
			}
		
		}
			String check =flag.toString();
			System.out.println(check);
			if (check.contains("False")) {
				result.setAttribute("Actual", " Cancellation is wrong " );
				Assert.fail("Cancellation is wrong ");
			} else {
				result.setAttribute("Actual", "Cancellation is Correct ");
			}
		
		}catch(Exception e){
			System.out.println(e);
			result.setAttribute("Actual", e);
			Assert.fail("This testCancellationPolicy is Failed due to :", e);
		}
		
	}

	public AvailabilityRequest getAvailabilityData() throws Exception {

		DataLoader loader = new DataLoader();
		AviRequest = loader.getReservationReqObjList(ExcelDataSingleton.getInstance("Resources/Full Regression Testing Checklist.xls", "Scenario").getDataHolder())[56][0];
		return AviRequest;

	}

}
