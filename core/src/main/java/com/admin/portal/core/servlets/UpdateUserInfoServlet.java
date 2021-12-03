package com.admin.portal.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.admin.portal.core.service.SaveDataInCSVService;

@Component(service = Servlet.class)
@SlingServletPaths(value = {"/bin/updateUserInfo","/adminportal/updateUserInfo"})
public class UpdateUserInfoServlet extends SlingAllMethodsServlet 
{

	private static final long serialVersionUID = 1259303003494790576L;
	private static final Logger LOG = LoggerFactory.getLogger(UpdateUserInfoServlet.class);
	private static final String ERROR_CODE = "ERROR_CODE";
	private static final String RESPONSE = "RESPONSE";
	private static final String ERROR_MSG = "ERROR_MSG";
	private static final String FILE_NAME = "UserInfo.csv";
	private static final String DAM_FILE_PATH = "/content/dam/adminportal/SAVE_CSV/"+FILE_NAME;

	@Reference
	private SaveDataInCSVService saveDataInCSVService;

	@Override
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException 
	{
		JSONObject responseJsonObject = new JSONObject();
		ResourceResolver resourceResolver = request.getResourceResolver();
		try 
		{		
			String id = request.getRequestParameter("id").getString();
			String category = request.getRequestParameter("category").getString();
			String name = request.getRequestParameter("name").getString();
			String trackingCode = request.getRequestParameter("trackingCode").getString();
			String brand = request.getRequestParameter("brand").getString();
			String nfosHeader = request.getRequestParameter("nfosHeader").getString();
			String nfosDesc = request.getRequestParameter("nfosDesc").getString();
			
			LOG.info("Received Details id: {} category: {} Name:{} Tracking_code:{}", id, category, name, trackingCode);
			LOG.info("Received Details Brand: {} NFOS_Header: {} NFOS_Desc:{} ", brand, nfosHeader, nfosDesc);		


			Map<String, String> avaliableFile = saveDataInCSVService.readFileFromDamLocation(DAM_FILE_PATH, resourceResolver, FILE_NAME);
			if(avaliableFile.get(ERROR_CODE).equals("0"))
			{
				Map<String, String> dataToUpdate=new HashMap<>();
				
				dataToUpdate.put("CATEGORY", category);
				dataToUpdate.put("NAME", name);
				dataToUpdate.put("TRACKING_CODE", trackingCode);
				dataToUpdate.put("BRAND", brand);
				dataToUpdate.put("NFOS_HEADER", nfosHeader);
				dataToUpdate.put("NFOS_DESC", nfosDesc);
				
				Map<String, Object> map = saveDataInCSVService.readAndUpdateFileData(avaliableFile.get("FILE"), id, dataToUpdate);
				String errorCode =  map.get(ERROR_CODE).toString();
				if(errorCode.equals("0"))
				{
					@SuppressWarnings("unchecked")
					List<String[]> list = (List<String[]>) map.get(RESPONSE);
					
					LOG.info("UpdateUserInfoServlet : GET LIST SIZE: {}",list.size());
					
					String filePath = saveDataInCSVService.createAndWriteDataInCSV(list);
					LOG.info("UpdateUserInfoServlet:filePath:  {}",filePath);
					Map<String, String> damResponse = saveDataInCSVService.updateFileToDAMLocation(filePath, resourceResolver,FILE_NAME,DAM_FILE_PATH);
					if(damResponse.get(ERROR_CODE).equals("0"))
					{
						responseJsonObject.put(ERROR_MSG, "DATA UPDATED : Request Processed Successfully.");
						responseJsonObject.put(ERROR_CODE, "0");
					}
					else
					{
						responseJsonObject.put(ERROR_MSG, "Unable to update Data.");
						responseJsonObject.put(ERROR_CODE, "1");
					}
				}
				else
				{
					responseJsonObject.put(RESPONSE, new JSONObject());
					responseJsonObject.put(ERROR_MSG, "No Record Available to update");
					responseJsonObject.put(ERROR_CODE, "0");
				}
			}
		} 
		catch (Exception e) 
		{
			LOG.error("ERROR IN REQUEST: ", e);
			try 
			{
				responseJsonObject.put(RESPONSE, new JSONObject());
				responseJsonObject.put(ERROR_MSG, "SYSTEM ERROR : Unable to update Data.");
				responseJsonObject.put(ERROR_CODE, "1");
			} 
			catch (JSONException e1) 
			{
				e1.printStackTrace();
			}
		}
		PrintWriter out = response.getWriter();
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		out.print(responseJsonObject);
		out.flush(); 
	}
}
