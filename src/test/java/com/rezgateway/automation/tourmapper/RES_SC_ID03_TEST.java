package com.rezgateway.automation.tourmapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.rezgateway.automation.JavaHttpHandler;
import com.rezgateway.automation.builder.request.AvailabilityRequestBuilder;
import com.rezgateway.automation.builder.request.ReservationRequestBuilder;
import com.rezgateway.automation.enu.ConfirmationType;
import com.rezgateway.automation.pojo.AvailabilityResponse;
import com.rezgateway.automation.pojo.DailyRates;
import com.rezgateway.automation.pojo.Hotel;
import com.rezgateway.automation.pojo.HttpResponse;
import com.rezgateway.automation.pojo.RateplansInfo;
import com.rezgateway.automation.pojo.ReservationRequest;
import com.rezgateway.automation.pojo.ReservationResponse;
import com.rezgateway.automation.pojo.Room;
import com.rezgateway.automation.reader.response.AvailabilityResponseReader;
import com.rezgateway.automation.reader.response.ReservationResponseReader;
import com.rezgateway.automation.reports.ExtentTestNGReportBuilderExt;
import com.rezgateway.automation.xmlout.utill.DataLoader;
import com.rezgateway.automation.xmlout.utill.ExcelDataSingleton;

public class RES_SC_ID03_TEST extends ExtentTestNGReportBuilderExt {

	AvailabilityResponse AvailabilityResponse = null;
	ReservationRequest ResRequest = null;
	ReservationResponse ResResponse = null;

	@Parameters({ "TestUrlRes", "TestUrl" })
	@Test(priority = 0)
	public synchronized void reservationTest(String TestUrlRes, String TestUrl) throws Exception {

		getAvailabilityData();
		
		String Scenario     = ResRequest.getScenarioID();
		String HotelCode    = Arrays.toString(ResRequest.getCode());
		String SearchString = ResRequest.getCheckin()+"|"+ResRequest.getCheckout()+" |"+ResRequest.getNoOfRooms()+"R| "+ResRequest.getUserName()+" |"+ResRequest.getPassword();
		
		ITestResult results = Reporter.getCurrentTestResult();
		String testname = "Test Scenario:" + Scenario + " : Search By : " + ResRequest.getSearchType() + " Code : " + HotelCode+ "Criteria : "+SearchString;
		results.setAttribute("TestName", testname);
		results.setAttribute("Expected", "System should be booked this Hotel");
		// http://192.168.1.62:8380/bonotelapps/bonotel/reservation/GetReservation.do

		// === Availability Check Started === /

		JavaHttpHandler handler = new JavaHttpHandler();
		HttpResponse Av_Response = handler.sendPOST(TestUrl, "xml=" + new AvailabilityRequestBuilder().buildRequest("Resources/RegessionSuite_AvailRequest_senarioID_" + ResRequest.getScenarioID() + ".xml", ResRequest));

		if (Av_Response.getRESPONSE_CODE() == 200) {
			AvailabilityResponse = new AvailabilityResponseReader().getResponse(Av_Response.getRESPONSE());
			if (AvailabilityResponse.getHotelCount() > 0) {
				results.setAttribute("Actual", "Results Available, Hotel Count :" + AvailabilityResponse.getHotelCount());
				// === Reservation Request Data Initiated === //
				Hotel hotel = AvailabilityResponse.getHotelList().entrySet().iterator().next().getValue();
				ResRequest.setCurrency(hotel.getRateCurrencyCode());

				Iterator<Entry<String, ArrayList<Room>>> itr = hotel.getRoomInfo().entrySet().iterator();

				Double TotalRate = 0.00;
				Double TotalTax = 0.00;
				int i = 0;
				while (itr.hasNext()) {
					Map.Entry<String, ArrayList<Room>> entry = (Map.Entry<String, ArrayList<Room>>) itr.next();
					// String roomnum = entry.getKey().trim();

					Room room = entry.getValue().get(0);
					room.setAdultsCount(ResRequest.getRoomlist().get(i).getAdultsCount());
					room.setChildCount(ResRequest.getRoomlist().get(i).getChildCount());
					room.setChildAges(ResRequest.getRoomlist().get(i).getChildAges());
					room.setConType(ResRequest.getRoomlist().get(i).getConType());
					Entry<String, RateplansInfo> plan = room.getRatesPlanInfo().entrySet().iterator().next();
					RateplansInfo planinfo = plan.getValue();
					room.setRatePlanCode(planinfo.getRatePlanCode());
					Double atax = planinfo.getTaxInfor().get("roomTax").getTaxAmount();
					Double btax = planinfo.getTaxInfor().get("salesTax").getTaxAmount();
					Double ctax = planinfo.getTaxInfor().get("otherCharges").getTaxAmount();
					TotalRate += planinfo.getTotalRate();
					TotalTax += (atax + btax + ctax);
					i++;
					ResRequest.addToRezRoomList(room);
					ResRequest.setConfType((room.getConType()));
				}

				ResRequest.setTotal(Double.toString(TotalRate));
				ResRequest.setTotalTax(Double.toString(TotalTax));
				// === Reservation Request Data Initiated === //
			} else {
				results.setAttribute("Actual", "Results not available Error Code :" + AvailabilityResponse.getErrorCode() + " Error Desc : " + AvailabilityResponse.getErrorDescription());
				Assert.fail("No Results Error Code : " + AvailabilityResponse.getErrorCode());
			}

		} else {
			results.setAttribute("Actual", "No Response recieved Code :" + Av_Response.getRESPONSE_CODE());
			Assert.fail("Invalid Response Code :" + Av_Response.getRESPONSE_CODE() + " ,No Response received");
		}
		// === Availability Check End === /

		HttpResponse Response = handler.sendPOST(TestUrlRes, "xml=" + new ReservationRequestBuilder().buildRequest("Resources/RegressionSuite_ReservationRequest_senarioID_" + ResRequest.getScenarioID() + ".xml", ResRequest));

		if (Response.getRESPONSE_CODE() == 200) {
			results.setAttribute("Actual", "Reservation Done:");
			ResResponse = new ReservationResponseReader().getResponse(Response.getRESPONSE());
			if ("Y".equalsIgnoreCase(ResResponse.getRreservationResponseStatus())) {
				results.setAttribute("Actual", "Reservation is  Done and Reservation number is  : " + ResResponse.getReferenceno());
			} else {
				results.setAttribute("Actual", "Reservation is not Done >>  Error Code : " + ResResponse.getErrorCode() + " Error Desc : " + ResResponse.getErrorDescription());
				Assert.fail("No Results Error Code : " + ResResponse.getErrorCode());
			}

		} else {
			results.setAttribute("Actual", "No Response recieved Code :" + Response.getRESPONSE_CODE());
			Assert.fail("Invalid Response Code :" + Response.getRESPONSE_CODE() + " ,No Response received");
		}

	}

	@Test(dependsOnMethods = "reservationTest")
	public synchronized void isAvailabileHotelNameInAviResponse() {

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

	@Test(dependsOnMethods = "reservationTest")
	public synchronized void testHotelCodeInAviResponse() {

		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing Hotel Code ");
		result.setAttribute("Expected", "CheckIN : " + ResRequest.getCode()[0]);

		if (AvailabilityResponse.getHotelList().containsKey(ResRequest.getCode()[0])) {
			result.setAttribute("Actual", AvailabilityResponse.getHotelList().entrySet().iterator().next().getKey());
		} else {
			result.setAttribute("Actual", "User entered Hotel Code is not in the Response : Actual Hotel Code is " + AvailabilityResponse.getHotelList().entrySet().iterator().next().getKey());
			Assert.fail("User entered Hotel Code is not in the Response:");
		}

	}

	@Test(dependsOnMethods = "reservationTest")
	public synchronized void testDayWiseRateAvailability() {

		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing Daily Rates having for Each date in the Response ");
		result.setAttribute("Expected", "System shouls contain the  Daily Rate for each day");
		try {

			Hotel hotelInResponse = AvailabilityResponse.getHotelList().get(ResRequest.getCode()[0]);
			Map<String, ArrayList<Room>> rooms = hotelInResponse.getRoomInfo();
			TreeMap<String, DailyRates> dailyRates = new TreeMap<String, DailyRates>();
			ArrayList<String> flag = new ArrayList<String>();

			for (Room room : rooms.entrySet().iterator().next().getValue()) {

				Map<String, RateplansInfo> RatesPlanInfos = room.getRatesPlanInfo();
				dailyRates = RatesPlanInfos.entrySet().iterator().next().getValue().getDailyRates();

				if (Integer.parseInt(ResRequest.getNoofNights()) == dailyRates.size()) {
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

	@Test(dependsOnMethods = "reservationTest")
	public synchronized void testNumOfRoomsAvailbility() {

		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing NumOfRooms Availbility in the Response ");
		result.setAttribute("Expected", "Response's room count should equal to requested Room count ");
		try {

			Hotel hotelInResponse = AvailabilityResponse.getHotelList().get(ResRequest.getCode()[0]);
			Map<String, ArrayList<Room>> rooms = hotelInResponse.getRoomInfo();
			if (Integer.parseInt(ResRequest.getNoOfRooms()) == rooms.entrySet().size()) {
				result.setAttribute("Actual", "No of Rooms are Correctly Exsist in the Availability Response");
			} else {
				result.setAttribute("Actual", "Response's Room count is not equal to the Reqested Room count");
				Assert.fail("Response's Room count is not equal to the Reqested Room count");
			}
		} catch (Exception e) {
			result.setAttribute("Actual", e);
			Assert.fail("This testNumOfRoomsAvailbility is Failed due to :", e);
		}
	}

	@Test(dependsOnMethods = "reservationTest")
	public synchronized void isReferenceNoAvailable() {

		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing Reservation no Availbility in the Reservation Response ");
		result.setAttribute("Expected", "If Reservation Response status is Y then Response should exsit the Reservation NO ");
		try {
			if (ResResponse.getReferenceno().contains("X")) {
				result.setAttribute("Actual", "Reservation number is availabile in the  Response  : " + ResResponse.getReferenceno());
			} else {
				result.setAttribute("Actual", "Reservation number is not availabile in the Reservation Response");
				Assert.fail("Reservation number is not availabile in the Reservation Response");
			}
		} catch (Exception e) {
			result.setAttribute("Actual", e);
			Assert.fail("This isReferenceNoAvailable is Failed due to :", e);
		}
	}

	@Test(dependsOnMethods = "reservationTest")
	public synchronized void isRoomReferenceNoAvailbility() {

		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing RoomReferenceNo Availbility for Each Room in the Reservation Response ");
		result.setAttribute("Expected", "If Reservation Response status is Y then Response should exsit the Reservation NO ");
		try {
			ArrayList<Room> bookedRooms = ResResponse.getRoomlist();
			ArrayList<String> flag = new ArrayList<String>();
			for (Room r : bookedRooms) {
				if (null != r.getRoomResNo() ? flag.add("True") : flag.add("False"))
					;
			}
			if (flag.contains("false")) {
				result.setAttribute("Actual", "Reservation number is not availabile in the Reservation Response");
				Assert.fail("Reservation number is not availabile in the Reservation Response");

			} else {
				result.setAttribute("Actual", "Room Reservation number is availabile in the  Response");
			}

		} catch (Exception e) {
			result.setAttribute("Actual", e);
			Assert.fail("This isReferenceNoAvailable is Failed due to :", e);
		}
	}

	@Test(dependsOnMethods = "reservationTest")
	public synchronized void testHotelCodeInReservationResponse() {
		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing Hotel Code is Correctly in the Reservation Response");
		result.setAttribute("Expected", "Hotel Code should be Same as the Resqested hotel Code  ");
		try {
			if (ResResponse.getBookedHotelCode().equals(ResRequest.getCode()[0])) {
				result.setAttribute("Actual", "Hotel Code is Same in the Reservation Response");
			} else {
				result.setAttribute("Actual", "Hotel code is Different or not in the Response");
				Assert.fail("Hotel code is not Different or not in the Response");
			}
		} catch (Exception e) {
			result.setAttribute("Actual", "testHotelCodeInReservationResponse is Failed due to : " + e);
			Assert.fail("Hotel code is not Different or not in the Response due to : ", e);
		}

	}

	@Test(dependsOnMethods = "reservationTest")
	public synchronized void testHotelConfirmationType() {
		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing ConfirmationType in the Reservation Response ");
		result.setAttribute("Expected", "This Booking should be in Confirmation Status");
		try {

			if (ConfirmationType.CON == ResResponse.getConfType()) {
				result.setAttribute("Actual", "Test is Failed passed due to Confiramtion Type is CON ");
			} else {
				result.setAttribute("Actual", "Test is Failed due to Confiramtion Type is : " + ResResponse.getReservationstatus());
				Assert.fail("Test is Failed due to Confiramtion Type is : " + ResResponse.getConfType());
			}

		} catch (Exception e) {
			result.setAttribute("Actual", " testHotelConfirmationType is Failed due to : " + e);
			Assert.fail(" testHotelConfirmationType is Failed due to : ", e);
		}
	}

	@Test(dependsOnMethods = "reservationTest")
	public synchronized void testTotalValue() {
		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Test Total value is Correctly Exsist in the Reservation Response");
		result.setAttribute("Expected", "Total rate should be same as to Reqested Total Value");
		try {
			if ((ResRequest.getTotal()).equals(ResResponse.getTotal())) {
				result.setAttribute("Actual", " Test Pased : Total rate equal to Reqested Total Value");
			} else {
				result.setAttribute("Actual", "Total value is Difference than requested.--->  Actual Value : " + ResResponse.getTotal() + " / Expected Value : " + ResRequest.getTotal());
				Assert.fail("Total value is Difference than requested ");
			}
		} catch (Exception e) {
			result.setAttribute("Actual", "testTotalValue  is Failed due to  : " + e);
			Assert.fail("testTotalValue  is Failed due to  : ", e);
		}
	}

	@Test(dependsOnMethods = "reservationTest")
	public synchronized void testTotalTaxValue() {
		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Test Total Tax value is Correctly Exsist in the Reservation Response");
		result.setAttribute("Expected", "Total Tax should be same as to Reqested Total Value");
		try {
			if ((ResRequest.getTotalTax()+"0").equals(ResResponse.getTotalTax())) {
				result.setAttribute("Actual", " Test Pased : Total Tax equal to Reqested Total Tax");
			} else {
				result.setAttribute("Actual", "Total Tax is Difference than requested > : Actual TAX Value : " + ResResponse.getTotalTax() + " / Expected Value : " + ResRequest.getTotalTax());
				Assert.fail("Total Tax is Difference than requested ");
			}
		} catch (Exception e) {
			result.setAttribute("Actual", "testTotalTaxValue  is Failed due to  : " + e);
			Assert.fail("testTotalTaxValue  is Failed due to  : ", e);
		}
	}

	@Test(dependsOnMethods = "reservationTest")
	public synchronized void isHotelOnRequestedAviResponse() {

		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing the Hotel is on OnRequest status For this Checkin Checkout dates ");
		result.setAttribute("Expected", "Should not be on OnRequest status For this Checkin Checkout dates ");
		try {
			Hotel hotelInResponse = AvailabilityResponse.getHotelList().get(ResRequest.getCode()[0]);
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

	public ReservationRequest getAvailabilityData() throws Exception {

		DataLoader loader = new DataLoader();
		ResRequest = loader.getReservationReqObjList(ExcelDataSingleton.getInstance("Resources/Full Regression Testing Checklist.xls", "Scenario").getDataHolder())[59][0];
		return ResRequest;

	}
}
