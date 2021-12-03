package com.admin.portal.core.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

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
@SlingServletPaths(
		value = {"/bin/uploadUserinfo","/adminportal/uploadUserinfo"}
		)

public class UploadUserInfoServlet extends SlingAllMethodsServlet
{

	private static final long serialVersionUID = -42410662428286902L;
	private static final Logger LOG = LoggerFactory.getLogger(UploadUserInfoServlet.class);
	private static final String FILE_NAME = "UserInfo.csv";
	private static final String DAM_FILE_PATH = "/content/dam/adminportal/SAVE_CSV/"+FILE_NAME;
	private static final String ERROR_CODE = "ERROR_CODE";
	private static final String ERROR_MSG = "ERROR_MSG";
	
	@Reference
	private SaveDataInCSVService saveDataInCSVService;
	
	@Override
	protected void doPost(SlingHttpServletRequest req, SlingHttpServletResponse resp) throws ServletException, IOException {
		JSONObject responseJsonObject = new JSONObject();
		try
		{
			ResourceResolver resourceResolver = req.getResourceResolver();
			
			String id = req.getRequestParameter("id").getString();
			String category = req.getRequestParameter("category").getString();
			String name = req.getRequestParameter("name").getString();
			String trackingCode = req.getRequestParameter("trackingCode").getString();
			String brand = req.getRequestParameter("brand").getString();
			String nfosHeader = req.getRequestParameter("nfosHeader").getString();
			String nfosDesc = req.getRequestParameter("nfosDesc").getString();

			LOG.info("Received Details id: {} category: {} Name:{} Tracking_code:{}", id, category, name, trackingCode);
			LOG.info("Received Details Brand: {} NFOS_Header: {} NFOS_Desc:{} ", brand, nfosHeader, nfosDesc);		

			List<String[]> data = new ArrayList<>();
			
			Map<String, String> avaliableFile =  saveDataInCSVService.readFileFromDamLocation(DAM_FILE_PATH, resourceResolver,FILE_NAME);
			LOG.info("avaliableFile HAshMap: {}",avaliableFile);
			Map<String, String> damResponse;
			if(avaliableFile.get(ERROR_CODE).equals("0"))
			{
				data.add(new String[]{id, category, name, trackingCode, brand, nfosHeader, nfosDesc});
				String fileNameFromDam = avaliableFile.get("FILE");
				fileNameFromDam = saveDataInCSVService.appendDatatoCSVFile(data, fileNameFromDam);
				damResponse = saveDataInCSVService.updateFileToDAMLocation(fileNameFromDam, resourceResolver,FILE_NAME,DAM_FILE_PATH);
			}
			else
			{
				data.add(new String[]{"ID", "Category", "Name", "Tracking_code", "Brand", "NFOS_Header", "NFOS_Desc"});
				data.add(new String[]{id, category, name, trackingCode, brand, nfosHeader, nfosDesc});

				String filePath = saveDataInCSVService.createAndWriteDataInCSV(data);
				LOG.info("CSV LOCAL File path:{} ",filePath);
				damResponse = saveDataInCSVService.uploadFileToDAMLocation(filePath, resourceResolver,FILE_NAME);
			}
			
			if(damResponse.get(ERROR_CODE).equals("0")) 
			{
				responseJsonObject.put(ERROR_MSG, "DATA SAVED : Request Processed Successfully.");
				responseJsonObject.put(ERROR_CODE, "0");
			}
			else
			{
				responseJsonObject.put(ERROR_MSG, "Unable to Save Data.");
				responseJsonObject.put(ERROR_CODE, "1");
			}
			LOG.info("damResponse: {}",damResponse);
		}
		catch (Exception e)
		{
			LOG.error("ERROR IN REQUEST: ", e);
			try 
			{
				responseJsonObject.put(ERROR_MSG, "SYSTEM ERROR : Unable to Save Data.");
				responseJsonObject.put(ERROR_CODE, "1");
			} 
			catch (JSONException e1) 
			{
				e1.printStackTrace();
			}
		}
		PrintWriter out = resp.getWriter();
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		out.print(responseJsonObject);
		out.flush(); 
	}
	
	private JSONObject readRawJson(SlingHttpServletRequest request)
	{
		JSONObject jsonObject = new JSONObject(); 		
		LOG.debug("Inside the readRawJson method");
		StringBuilder jsonBuff = new StringBuilder();
		String line = null;
		try 
		{
			BufferedReader reader = request.getReader();
			while ((line = reader.readLine()) != null)
			{
				jsonBuff.append(line);
			}
			jsonObject = new JSONObject(jsonBuff.toString());
		} 
		catch (Exception e) 
		{ 
			LOG.error("readRawJson : ",e);
		}
		return jsonObject;
	}
}
