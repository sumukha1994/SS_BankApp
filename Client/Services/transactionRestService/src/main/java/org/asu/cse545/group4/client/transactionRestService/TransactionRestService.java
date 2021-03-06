package org.asu.cse545.group4.client.transactionRestService;

import java.security.Principal;
import org.asu.cse545.group4.server.transactionservice.service.TransactionService;
import org.asu.cse545.group4.server.transactionservice.service.TransactionJson;
import org.asu.cse545.group4.server.transactionservice.service.AccountUser;
import org.asu.cse545.group4.server.transactionservice.service.RequestUser;
import org.asu.cse545.group4.server.sharedobjects.TblTransaction;
import org.asu.cse545.group4.server.sharedobjects.TblAccount;
import org.asu.cse545.group4.server.sharedobjects.TblEventLog;
import org.asu.cse545.group4.server.sharedobjects.TblUser;
import org.asu.cse545.group4.server.sharedobjects.TblRequest;
import org.asu.cse545.group4.server.sharedobjects.TblUserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.json.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.asu.cse545.group4.client.utils.AccountAppointmentStrategy;
import org.asu.cse545.group4.client.utils.UserExclusionStrategy;
import java.io.IOException;
import java.util.List;
import org.asu.cse545.group4.server.eventservice.service.EventService;
import org.asu.cse545.group4.server.requestservice.service.RequestService;
@Controller
public class TransactionRestService
{	

	@Autowired
	private TransactionService transactionService;
	@Autowired
	private EventService eventService;
	@Autowired
	private RequestService reqService;
	private final static String TRANSACTION_ADDED = "New Transaction by User: ";
	private final static String TRANSACTION_APPROVED = "Transaction approval by User: ";
	private final static String TRANSACTION_DECLINED = "Transaction decline by User: ";
	private final static String NEW_ACCOUNT_REQUEST = "Request for new account ";
	private final static String EVENT_ACCOUNT_UPDATED_STR = "Account updated by User: ";
	private final static String EVENT_ACCOUNT_DELETED_STR = "Account deleted by User: ";
	private final static String EVENT_ACCOUNT_NEW_REQ_STR = "New Account requested by User: ";
	private final static String EVENT_ACCOUNT_NEW_APPROVED_STR = "New Account approved by User: ";


	private final static int NEW_TRANSACTION = 2;
	private final static int APPROVE_TRANSACTION = 3;
	private final static int DECLINE_TRANSACTION = 4;
	private final static int EVENT_UPDATE_ACCOUNT = 5;
	private final static int EVENT_DELETE_ACCOUNT = 6;
	private final static int EVENT_CREATE_ACCOUNT_REQUEST = 7;
	private final static int EVENT_CREATE_ACCOUNT_APPROVED = 8;


	private static final int FINANCE = 1;
	private static final int NEW_ACCOUNT =5;


	private void logEvent(String message, int userId, String response, int eventType)
	{
		TblUser dbUser = transactionService.getUser(new TblUser(userId));
		if (dbUser == null) {
			return;
		}
		TblUserProfile profile = dbUser.getTblUserProfile();
		TblEventLog event = new TblEventLog();
		event.setEventType(eventType);
		StringBuilder sb = new StringBuilder();
		sb.append(message);
		sb.append(" ");
		sb.append(profile.getFirstName());
		sb.append(" ");
		sb.append(profile.getLastName());
		sb.append(":").append(response);
		event.setEventName(sb.toString());
		eventService.logEvent(event);
	}

	@PostMapping(value="/getTransaction",consumes="application/json",produces="application/json")
	public @ResponseBody String getTransaction(@RequestBody TransactionJson newTransaction) throws IOException
	{
		// check for user auth
		TblTransaction transaction = newTransaction.getTransactionObj();
		TblTransaction ret =  this.transactionService.getTransaction(transaction);
		Gson gson = new GsonBuilder().setExclusionStrategies(new UserExclusionStrategy()).create();
		return gson.toJson(ret);
	}

	@PostMapping(value="/transaction",consumes="application/json",produces="application/json")
	  public @ResponseBody String transaction(@RequestBody TransactionJson newTransaction) throws Exception
	   {
	  	TblTransaction transaction = null;
	  	try
	  	{
	  		transaction = newTransaction.getTransactionObj();	  		
	  		// TODO
	  		// check for User authorization
			String response = this.transactionService.addTransaction(transaction , newTransaction.getUserId() , FINANCE);
			logEvent(TRANSACTION_ADDED, newTransaction.getUserId() , response, NEW_TRANSACTION);
			return response;
		}
		catch(Exception e)
		{
			// handle 
			throw e;
		}
	  }

	  @PostMapping(value="/getAllAccounts",consumes="application/json",produces="application/json")
		public @ResponseBody String getAllAccounts(@RequestBody TblUser user) {
		System.out.println("inside getAllAccounts");
		Gson gson = new GsonBuilder().setExclusionStrategies(new UserExclusionStrategy()).create();
		if (reqService.isCustomer(user) || reqService.isMerchant(user) )
		{
			return gson.toJson("fail");
		}
		String allAccounts = this.transactionService.getAllAccounts();
		return allAccounts;
	}


	  @PostMapping(value="/approveTransaction", consumes = "application/json" , produces = "application/json")
	  public @ResponseBody String approveTransaction(@RequestBody String requestString)
	  {
	  		JSONObject request = new JSONObject(requestString);
	  		if (!request.has("transaction_id") || !request.has("approved_by")) {
	  			return "INVALID_REQUEST";
	  		}
	  		String response = this.transactionService.approveTransaction(request.getInt("transaction_id") , request.getInt("approved_by"));
	  		logEvent(TRANSACTION_APPROVED,request.getInt("approved_by") , response , APPROVE_TRANSACTION);
	  		return response;
	  }


	  @PostMapping(value="/declineTransaction" , consumes = "application/json" , produces = "application/json")
	  public @ResponseBody String declineTransaction(@RequestBody String requestString)
	  {
	  		JSONObject request = new JSONObject(requestString);
	  		if (!request.has("transaction_id") || !request.has("declined_by")) {
	  			return "INVALID_REQUEST";
	  		}
	  		String response = this.transactionService.declineTransaction(request.getInt("transaction_id") , request.getInt("declined_by"));
	  		logEvent(TRANSACTION_DECLINED, request.getInt("declined_by") , response, DECLINE_TRANSACTION);
	  		return response;
	  }

	  @PostMapping(value="/searchAccount", consumes="application/json" , produces = "application/json")
	  public @ResponseBody String searchAccount(@RequestBody TblUserProfile profile)
	  {
	  	return this.transactionService.searchAccount(profile);
	  }

	  @PostMapping(value="/accountTransactions",consumes="application/json", produces="application/json")
	  public @ResponseBody String accountTransactions(@RequestBody TblAccount account)
	  {
	  		List<TblTransaction> transactions = this.transactionService.getTransactionsForAccount(account);
	  		Gson gson = new GsonBuilder().setExclusionStrategies(new UserExclusionStrategy()).create();
			return gson.toJson(transactions);
	  }

	  @PostMapping(value="/userAccounts",consumes="application/json", produces="application/json")
	  public @ResponseBody String userAccounts(@RequestBody TblUser user)
	  {
	  		List<TblAccount> accounts = this.transactionService.getAccountsForUser(user);
	  		Gson gson = new GsonBuilder().setExclusionStrategies(new UserExclusionStrategy()).create();
			return gson.toJson(accounts);
	  }


	  @PostMapping(value="/updateAccount",consumes="application/json", produces="application/json")
	  public @ResponseBody String updateAccount(@RequestBody AccountUser accUser)
	  {
	  		TblAccount account = accUser.getAccount();
	  		account = this.transactionService.updateAccount( account , accUser.getUser());
	  		if (account != null) {
	  			logEvent(EVENT_ACCOUNT_UPDATED_STR, accUser.getUser().getUserId() , "OK", EVENT_UPDATE_ACCOUNT);
	  		}
	  		Gson gson = new GsonBuilder().setExclusionStrategies(new UserExclusionStrategy()).create();
			return gson.toJson(account);
	  }

	  @PostMapping(value="/deleteAccount",consumes="application/json", produces="application/json")
	  public @ResponseBody String deleteAccount(@RequestBody  AccountUser accUser)
	  {
	  		// TODO 
	  		// authorize check
	  		TblAccount account = accUser.getAccount();
	  		String resp = this.transactionService.deleteAccount(account, accUser.getUser());
	  		if (resp.equals("SUCCESS")) {
	  			logEvent(EVENT_ACCOUNT_DELETED_STR, accUser.getUser().getUserId() , "OK", EVENT_DELETE_ACCOUNT);
	  		}
	  		return resp;	  		
	  }
	  
	  @PostMapping(value="/accountByAccountParams",consumes="application/json", produces="application/json")
	  public @ResponseBody String userAccounts(@RequestBody TblAccount account)
	  {
	  		List<TblAccount> accounts = this.transactionService.searchAccountByAccountParams(account);
	  		Gson gson = new GsonBuilder().setExclusionStrategies(new AccountAppointmentStrategy()).create();
			return gson.toJson(accounts);
	  }



	  @PostMapping(value="/account",consumes="application/json",produces="application/json")
	  public @ResponseBody String account(@RequestBody RequestUser reqUser) 
	  {
	  	try
	  	{
	  		TblUser user = reqUser.getUser();
	  		TblRequest request = new TblRequest(); 
	  		request.setTblUserByRequestedBy(user);
	  		request.setTypeOfRequest(NEW_ACCOUNT);
	  		request.setTypeOfAccount(reqUser.getRequest().getTypeOfAccount());
	  		reqService.addRequest(request);
			logEvent(EVENT_ACCOUNT_NEW_REQ_STR, user.getUserId() , "OK", NEW_ACCOUNT);
			return "OK";
		}
		catch(Exception e)
		{
			// handle 
			return e.toString();
		}
	  }

	  @PostMapping(value="/approveNewAccount",consumes="application/json",produces="application/json")
	  public @ResponseBody String approveAccountCreate(@RequestBody RequestUser reqUser)
	  {
	  	try
	  	{
	  		// return ""+reqUser.getRequest().getRequestId() + reqUser.getApprover().getUserId() ;
	  		TblAccount account = this.transactionService.createAccount( reqUser.getRequest() , reqUser.getUser() );
	  		if (account != null) {
	  			logEvent(EVENT_ACCOUNT_NEW_APPROVED_STR , reqUser.getUser().getUserId() , "OK" , EVENT_CREATE_ACCOUNT_APPROVED);
	  		}
	  		if (account == null) {
	  			return "ERROR";
	  		}
	  		Gson gson = new GsonBuilder().setExclusionStrategies(new UserExclusionStrategy()).create();
			return gson.toJson(account);
		}
		catch(Exception e)
		{
			// handle 
			throw e;

		}
	  }
}
