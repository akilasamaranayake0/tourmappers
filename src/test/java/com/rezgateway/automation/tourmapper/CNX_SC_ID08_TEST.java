package com.rezgateway.automation.tourmapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import com.rezgateway.automation.builder.request.CancellationRequestBuilder;
import com.rezgateway.automation.builder.request.ReservationRequestBuilder;
import com.rezgateway.automation.enu.ConfirmationType;
import com.rezgateway.automation.pojo.AvailabilityResponse;
import com.rezgateway.automation.pojo.BookingPolicy;
import com.rezgateway.automation.pojo.CancellationRequest;
import com.rezgateway.automation.pojo.CancellationResponse;
import com.rezgateway.automation.pojo.DailyRates;
import com.rezgateway.automation.pojo.Hotel;
import com.rezgateway.automation.pojo.HttpResponse;
import com.rezgateway.automation.pojo.RateplansInfo;
import com.rezgateway.automation.pojo.ReservationRequest;
import com.rezgateway.automation.pojo.ReservationResponse;
import com.rezgateway.automation.pojo.Room;
import com.rezgateway.automation.reader.response.AvailabilityResponseReader;
import com.rezgateway.automation.reader.response.CancellationResponseReader;
import com.rezgateway.automation.reader.response.ReservationResponseReader;
import com.rezgateway.automation.reports.ExtentTestNGReportBuilderExt;
import com.rezgateway.automation.xmlout.utill.DataLoader;
import com.rezgateway.automation.xmlout.utill.ExcelDataSingleton;

////Cancellation scenario Confirmed within policy TYPE ANY
public class CNX_SC_ID08_TEST extends ExtentTestNGReportBuilderExt {
	
	AvailabilityResponse AvailabilityResponse = null;
	ReservationRequest ResRequest = null;
	ReservationResponse ResResponse = null;
	CancellationResponse cnxR = null;

	@Parameters({ "TestUrlRes", "TestUrl", "TestUrlCnx" })
	@Test(priority = 0)
	public synchronized void cancellationTesting(String TestUrlRes, String TestUrl, String TestUrlCnx) throws Exception {

		getAvailabilityData();
		
		String Scenario     = ResRequest.getScenarioID();
		String HotelCode    = Arrays.toString(ResRequest.getCode());
		
		//Within Cancellation period less than 10
		  DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
		   LocalDateTime now = LocalDateTime.now();
		   now= now.plusDays(90);//cancellation policy TYPE ANY
		   String newcheckin=dtf.format(now);
		   now= now.plusDays(2);
		   String newcheckout=dtf.format(now);
		   
		   ResRequest.setCheckin(newcheckin);
		   ResRequest.setCheckout(newcheckout);
		   
		   
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
					
					for (int j = 0; j < entry.getValue().size(); j++) {
						
					//for (Room  r : entry.getValue()) {
						//System.out.println("asdsdsdsdsdsd"+r.getRoomType());
						if("Harborfront".equals(entry.getValue().get(j).getRoomType())){
							 room = entry.getValue().get(j);
						}
					}
	
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
			ResResponse = new ReservationResponseReader().getResponse(Response.getRESPONSE());

			CancellationRequest cnx = new CancellationRequest();
			LocalDateTime date = LocalDateTime.now();
			DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

			cnx.setCancellationRequestTimestamp(date.format(formatter));
			cnx.setUserName(ResRequest.getUserName());
			cnx.setPassword(ResRequest.getPassword());
			cnx.setSupplierReferenceNo(ResResponse.getReferenceno());
			cnx.setCancellationNotes("Cancellation_note_senarioID__" + ResRequest.getScenarioID());
			cnx.setCancellationReason("Cancellation_Reason_senarioID__ " + ResRequest.getScenarioID());

			HttpResponse cnxResponse = handler.sendPOST(TestUrlCnx, "xml=" + new CancellationRequestBuilder().buildRequest("Resources/RegressionSuite_CancellationRequest_senarioID_" + ResRequest.getScenarioID() + ".xml", cnx));
			cnxR = new CancellationResponseReader().getResponse(cnxResponse.getRESPONSE());

			if (cnxResponse.getRESPONSE_CODE() == 200) {

				if (("Y").equals(cnxR.getCancellationResponseStatus())) {
					results.setAttribute("Actual", "Cancellation Done : " + ResResponse.getReferenceno() + " / " + cnxR.getCancellationNo() + " : " + cnxR.getCanellationFee());
				} else {
					results.setAttribute("Actual", "Cancellation is not done due to  :" + cnxR.getErrorCode() + " / " + cnxR.getErrorDescription());
					Assert.fail("Cancellation is not done due to  :" + cnxR.getErrorCode() + " / " + cnxR.getErrorDescription());
				}

			} else {
				results.setAttribute("Actual", "No Response recieved Code :" + cnxResponse.getRESPONSE_CODE());
				Assert.fail("Invalid Response Code : " + cnxResponse.getRESPONSE_CODE() + " / " + cnx.getCancellationReason() + " ,No Response received");
			}

		} else {
			results.setAttribute("Actual", "No Response recieved Code :" + Response.getRESPONSE_CODE());
			Assert.fail("Invalid Response Code :" + Response.getRESPONSE_CODE() + " ,No Response received");
		}

	}



	
	// currently this test doing for static data
	@Test(dependsOnMethods = "cancellationTesting" ,priority = 2)
	public synchronized void testCancellationPolicy() {

		ITestResult result = Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing is applying Cancellation policy");
		result.setAttribute("Expected", "System should display cnx policy in Checkin Checkout dates ");

		try {
			Hotel hotelInResponse = AvailabilityResponse.getHotelList().get(ResRequest.getCode()[0]);
			Map<String, ArrayList<Room>> rooms = hotelInResponse.getRoomInfo();
			ArrayList<String> flag = new ArrayList<String>();

			for (Room r : rooms.entrySet().iterator().next().getValue()) {

				
				if(r.getRoomPolicy()==null)
				{
					result.setAttribute("Actual", " policy not found ");
					flag.add("False");
					Assert.fail("Cancellation policy not found ");
				}
				
				for(BookingPolicy bockingPolicy : r.getRoomPolicy() ){
		
					
					if(r.getRoomType().equals("King")){
						if(r.getRoomPolicy().size()==3){
							
							BookingPolicy bookingPolicy=r.getRoomPolicy().get(0);
							BookingPolicy bookingPolicy1=r.getRoomPolicy().get(0);
							BookingPolicy bookingPolicy2=r.getRoomPolicy().get(0);
							BookingPolicy bookingPolicy3=r.getRoomPolicy().get(0);
							
							for (int i = 0; i < 3; i++) {
								bookingPolicy=r.getRoomPolicy().get(i);
								
								if(70==(bookingPolicy.getArrivalRangeValue())){
									bookingPolicy1 = r.getRoomPolicy().get(i);
								}
								else if (50==(bookingPolicy.getArrivalRangeValue())){
									bookingPolicy2 = r.getRoomPolicy().get(i);
								}
								
								else 
									bookingPolicy3 = r.getRoomPolicy().get(i);
								
							}
							
							//BookingPolicy bookingPolicy1 = r.getRoomPolicy().get(1);
							
							if(("2019-02-14".equals(bookingPolicy1.getPolicyFrom())&&("2020-02-14".equals(bookingPolicy1.getPolicyTo())))? flag.add("PolicyFromAndTo_True") : flag.add("PolicyFromAndTo_False"));
							if("Cancel".equals(bookingPolicy1.getAmendmentType())? flag.add("AmendmentType_True") : flag.add("AmendmentType_False"));
							if("percentage".equals(bookingPolicy1.getPolicyBasedOn())? flag.add("PolicyBasedOn_True") : flag.add("PolicyBasedOn_False"));
							if("50".equals(bookingPolicy1.getPolicyBasedOnValue())? flag.add("PolicyBasedOnValue_True"):flag.add("PolicyBasedOnValue_False"));
							if("Less Than".equals(bookingPolicy1.getArrivalRange())? flag.add("ArrivalRange_True"):flag.add("ArrivalRange_False"));
							if(70==(bookingPolicy1.getArrivalRangeValue()) ? (flag.add("getArrivalRangeValue_True")):(flag.add("getArrivalRange_False")));
							if("$262.5".equals(bookingPolicy1.getPolicyFee()) ? flag.add("PolicyFee_True") : flag.add("PolicyFee_True"));
							if("$262.50".equals(bookingPolicy1.getNoShowPolicyFee()) ? flag.add("NoShowPolicyFee_True") : flag.add("NoShowPolicyFee_False")  );
							
							//BookingPolicy bookingPolicy2 = r.getRoomPolicy().get(0);
							
							if(("2019-02-14".equals(bookingPolicy2.getPolicyFrom())&&("2020-02-14".equals(bookingPolicy2.getPolicyTo())))? flag.add("PolicyFromAndTo_True") : flag.add("PolicyFromAndTo_False"));
							if("Cancel".equals(bookingPolicy2.getAmendmentType())? flag.add("AmendmentType_True") : flag.add("AmendmentType_False"));
							if("percentage".equals(bookingPolicy2.getPolicyBasedOn())? flag.add("PolicyBasedOn_True") : flag.add("PolicyBasedOn_False"));
							if("100".equals(bookingPolicy2.getPolicyBasedOnValue())? flag.add("PolicyBasedOn_True"):flag.add("PolicyBasedOn_False"));
							if("Less Than".equals(bookingPolicy2.getArrivalRange())? flag.add("ArrivalRange_True"):flag.add("ArrivalRange_False"));
							if(50==(bookingPolicy2.getArrivalRangeValue()) ? (flag.add("getArrivalRangeValue_True")):(flag.add("getArrivalRange_False")));
							if("$525.0".equals(bookingPolicy2.getPolicyFee()) ? flag.add("PolicyFee_True") : flag.add("PolicyFee_True"));
							if("$525.00".equals(bookingPolicy2.getNoShowPolicyFee()) ? flag.add("NoShowPolicyFee_True") : flag.add("NoShowPolicyFee_False")  );
						
							//BookingPolicy bookingPolicy3 = r.getRoomPolicy().get(2);
							
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
							
							BookingPolicy bookingPolicyr=r.getRoomPolicy().get(0);
							BookingPolicy bookingPolicyr1=r.getRoomPolicy().get(0);
							BookingPolicy bookingPolicyr2=r.getRoomPolicy().get(0);
							
							for (int i = 0; i < 2; i++) {
								bookingPolicyr=r.getRoomPolicy().get(i);
								
								if(30==(bookingPolicyr.getArrivalRangeValue())){
									bookingPolicyr1 = r.getRoomPolicy().get(i);
								}
								else
									bookingPolicyr2 = r.getRoomPolicy().get(i);
							}
							
							//BookingPolicy bookingPolicyr1 = r.getRoomPolicy().get(1);
							
							if(("2019-02-14".equals(bookingPolicyr1.getPolicyFrom())&&("2020-02-14".equals(bookingPolicyr1.getPolicyTo())))? flag.add("PolicyFromAndTo_True") : flag.add("PolicyFromAndTo_False"));
							if("Cancel".equals(bookingPolicyr1.getAmendmentType())? flag.add("AmendmentType_True") : flag.add("AmendmentType_False"));
							if("nights".equals(bookingPolicyr1.getPolicyBasedOn())? flag.add("PolicyBasedOn_True") : flag.add("PolicyBasedOn_False"));
							if("1".equals(bookingPolicyr1.getPolicyBasedOnValue())? flag.add("PolicyBasedOnValue_True"):flag.add("PolicyBasedOnValue_False"));
							if("Less Than".equals(bookingPolicyr1.getArrivalRange())? flag.add("ArrivalRange_True"):flag.add("ArrivalRange_False"));
							if(30==(bookingPolicyr1.getArrivalRangeValue()) ? (flag.add("getArrivalRangeValue_True")):(flag.add("getArrivalRange_False")));
							if("$315.0".equals(bookingPolicyr1.getPolicyFee()) ? flag.add("PolicyFee_True") : flag.add("PolicyFee_True"));
							if("$630.00".equals(bookingPolicyr1.getNoShowPolicyFee()) ? flag.add("NoShowPolicyFee_True") : flag.add("NoShowPolicyFee_False")  );
							
							//BookingPolicy bookingPolicyr2 = r.getRoomPolicy().get(0);
							
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
				
			}
			
			String check = flag.toString();
			
			if (check.contains("False")) {
				result.setAttribute("Actual", " Cancellation policy is wrong ");
				Assert.fail("Cancellation policy is wrong ");
				System.out.println(check);
			} else {
				result.setAttribute("Actual", "Cancellation policy is Correct ");
			}

		} catch (Exception e) {
			System.out.println(e);
			result.setAttribute("Actual", e);
			Assert.fail("This testCancellationPolicy is Failed due to :", e);
		}

	}

	@Test(dependsOnMethods= "cancellationTesting")
	public synchronized void testCancellationFee(){
		
		ITestResult result= Reporter.getCurrentTestResult();
		result.setAttribute("TestName", "Testing the Cancellation Fee is correctly applying when do the Cancellation");
		result.setAttribute("Expected", "Cancellation fee Should be Applyed according to the CNX policy");
	
	try {
			if(("100").equals(cnxR.getCanellationFee())){
				result.setAttribute("Actual", " Standard Cancellation Fee is Correct ");
			}else{
				result.setAttribute("Actual", " Standard Cancellation Fee is inCorrect ");
				Assert.fail("Standard Cancellation Fee is inCorrect ");
			}
				
		} catch (Exception e) {
			System.out.println(e);
			result.setAttribute("Actual", e);
			Assert.fail("This testCancellationPolicy is Failed due to :", e);
		}
			

	}
	
	

	@Test(dependsOnMethods = "cancellationTesting",priority = 1)
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
		ResRequest = loader.getReservationReqObjList(ExcelDataSingleton.getInstance("Resources/Full Regression Testing Checklist.xls", "Scenario").getDataHolder())[70][0];
		return ResRequest;

	}

}
