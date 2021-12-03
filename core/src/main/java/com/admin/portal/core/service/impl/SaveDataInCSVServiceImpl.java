package com.admin.portal.core.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.admin.portal.core.service.SaveDataInCSVService;
import com.day.cq.dam.api.AssetManager;
import com.opencsv.CSVWriter;

@Component(service = SaveDataInCSVService.class, immediate = true)
public class SaveDataInCSVServiceImpl implements SaveDataInCSVService 
{
	private static final Logger LOG = LoggerFactory.getLogger(SaveDataInCSVServiceImpl.class);
	private static final String ERROR_CODE = "ERROR_CODE";
	private static final String ERROR_MSG = "ERROR_MSG";
	private static final String RESPONSE = "RESPONSE";
	private static final String DEFAULT_DAM_PATH = "/content/dam/adminportal";

	@Override
	public Map<String, String> uploadFileToDAMLocation(String filePath, ResourceResolver resourceResolver,String fileName) throws IOException 
	{
		HashMap<String, String> hashMap=new HashMap<>();

		LOG.debug("SaveDataInCSVServiceImpl::UploadFileToDAMLocation::filePath {} ", filePath);
		Session session;
		String finalFileName = null;

		File file = new File(filePath);

		LOG.debug("SaveDataInCSVServiceImpl::UploadFileToDAMLocation::getAbsolutePath{} ", file.getAbsolutePath());
		LOG.debug("SaveDataInCSVServiceImpl::UploadFileToDAMLocation::getCanonicalPath{} ", file.getCanonicalPath());

		try (FileInputStream fileInputStream = new FileInputStream(file))
		{
			if (resourceResolver != null) 
			{
				session = resourceResolver.adaptTo(Session.class);
				if (session != null) 
				{
					Node defaultNode = session.getNode(DEFAULT_DAM_PATH);
					Node transferFile = null;
					String innerNodes = "/SAVE_CSV";
					LOG.debug("INSIDE FOR LOOP innerNodes: {}", innerNodes);
					String[] innerNodesArray = innerNodes.split("/");
					for (String folderName : innerNodesArray) 
					{
						LOG.debug("INSIDE FOR LOOP folderName: {}", folderName);
						if (!checkValueIsNullOrEmpty(folderName)) 
						{
							if (defaultNode.hasNode(folderName)) 
							{
								LOG.debug("{} node is Already Present", folderName);
								defaultNode = defaultNode.getNode(folderName);
							} 
							else 
							{
								LOG.debug("{} Node Created", folderName);
								defaultNode = defaultNode.addNode(folderName, "sling:OrderedFolder");
							}
						}
					}
					if (defaultNode != null) 
					{
						transferFile = defaultNode.addNode(fileName, "dam:Asset");
						AssetManager assetMgr = resourceResolver.adaptTo(AssetManager.class);
						assert assetMgr != null;
						LOG.info("Transfer File Get Path() {}",transferFile.getPath());
						assetMgr.createAsset(transferFile.getPath(), fileInputStream, "text/csv", true);
					}
					finalFileName = transferFile!=null ? transferFile.getPath() : "";
					session.save();

					hashMap.put("FILE", finalFileName);
					hashMap.put(ERROR_CODE, "0");
					hashMap.put(ERROR_MSG, "Uploaded Sucessfully");
				}
			}
			else
			{
				hashMap.put("FILE", "");
				hashMap.put(ERROR_CODE, "1");
				hashMap.put(ERROR_MSG, "ResourceResolver is Null");
			}
		} 
		catch (Exception e) 
		{
			LOG.error("UploadFileToDAMLocation :: ERROR", e);
			hashMap.put("FILE", "");
			hashMap.put(ERROR_CODE, "1");
			hashMap.put(ERROR_MSG, "SYSTEM ERROR: "+e.getMessage());
		}
		return hashMap;
	}

	@Override
	public Map<String, String> updateFileToDAMLocation(String filePath, ResourceResolver resourceResolver, String fileName, String damFilePath) throws IOException
	{	
		HashMap<String, String> hashMap=new HashMap<>();

		try(FileInputStream fileInputStream = new FileInputStream(new File(filePath)))
		{
			if (resourceResolver != null) 
			{
				String finalFileName = null;
				Session session = resourceResolver.adaptTo(Session.class);		
				Binary assetBinary = session.getValueFactory().createBinary(fileInputStream);			
				AssetManager assetMgr = resourceResolver.adaptTo(AssetManager.class);
				assert assetMgr != null;
				assetMgr.createOrUpdateAsset(damFilePath, assetBinary, "text/csv", false);
				finalFileName = damFilePath;
				session.save();

				hashMap.put("FILE", finalFileName);
				hashMap.put(ERROR_CODE, "0");
				hashMap.put(ERROR_MSG, "Updated Sucessfully");
			}
			else
			{
				hashMap.put("FILE", "");
				hashMap.put(ERROR_CODE, "1");
				hashMap.put(ERROR_MSG, "ResourceResolver is Null");
			}
		} 
		catch (RepositoryException e) 
		{
			LOG.error("updateFileToDAMLocation :: ERROR", e);
			hashMap.put("FILE", "");
			hashMap.put(ERROR_CODE, "1");
			hashMap.put(ERROR_MSG, "SYSTEM ERROR: "+e.getMessage());
		}
		return hashMap;
	}
	private boolean checkValueIsNullOrEmpty(String folderName) 
	{
		return (folderName == null || folderName.isEmpty());
	}

	@Override
	public String createAndWriteDataInCSV( List<String[]> data) throws IOException 
	{
		// first create file object for file placed at temp location specified by filepath
		File file = File.createTempFile("tmp", ".csv");
		try 
		{
			// create FileWriter object with file as parameter
			FileWriter outputfile = new FileWriter(file);

			// create CSVWriter object filewriter object as parameter
			CSVWriter writer=new CSVWriter(outputfile);

			writer.writeAll(data);

			// closing writer connection
			writer.close();
		}
		catch (IOException e)
		{
			LOG.error("createAndWriteDataInCSV : Exception: ", e);
		}
		return file.getAbsolutePath();
	}

	@Override
	public String appendDatatoCSVFile(List<String[]> data, String existingCsvFile) throws IOException
	{
		try
		{
			FileWriter fileWriter = new FileWriter(existingCsvFile, true);

			CSVWriter writer = new CSVWriter(fileWriter);

			writer.writeNext(data.get(0));

			writer.close();
		}
		catch (IOException e)
		{
			LOG.error("appendDatatoCSVFile : Exception: ", e);
		}
		return existingCsvFile;
	}

	@Override
	public Map<String, String> readFileFromDamLocation(String fileName, ResourceResolver resourceResolver, String damFileName) throws IOException
	{
		HashMap<String, String> hashMap=new HashMap<>();
		LOG.info("Requested File Name: {}",fileName);
		try 
		{
			Resource assetResource = resourceResolver.getResource(fileName+"/jcr:content/renditions/original");
			if (assetResource != null)
			{		
				LOG.info("Resource type: {}",assetResource.getResourceType());
				LOG.info("Resource Path: {}",assetResource.getPath());

				Node imageNode = assetResource.adaptTo(Node.class);
				Node contentNode = imageNode.getNode("jcr:content");
				Binary assetBinary = contentNode.getProperty("jcr:data").getBinary();
				InputStream inputStream = assetBinary.getStream(); 

				String fileNameToGenerate=System.getProperty("java.io.tmpdir")+damFileName;
				FileUtils.copyInputStreamToFile(inputStream, new File(fileNameToGenerate));

				hashMap.put("FILE", fileNameToGenerate);
				hashMap.put(ERROR_CODE, "0");
			}
			else
			{
				hashMap.put("FILE", "");
				hashMap.put(ERROR_CODE, "1");
				LOG.info("Asset Not Available");
			}
		} 
		catch (Exception e) 
		{
			LOG.error("readFileFromDamLocation : Exception: ", e);
			hashMap.put("FILE", "");
			hashMap.put(ERROR_CODE, "1");
		}
		return hashMap;
	}

	@Override
	public Map<String, Object> readFileData(String filePath, String id) 
	{
		JSONObject jsonObject=new JSONObject();
		HashMap<String, Object> hashMap=new HashMap<>();
		try(BufferedReader br = new BufferedReader(new FileReader(filePath)))
		{  
			String line;
			while ((line = br.readLine()) != null)  
			{
				String[] userDetails = line.split(",");  

				String userid = rmQot(userDetails[0]);
				if(userid.equals(id))
				{
					LOG.info("CSV id: {} category: {} Name:{} Tracking_code:{} Brand: {} NFOS_Header: {} NFOS_Desc:{}", userDetails[0], userDetails[1], userDetails[2], userDetails[3], userDetails[4], userDetails[5], userDetails[6]);

					jsonObject.put("ID", rmQot(userDetails[0]));
					jsonObject.put("CATEGORY", rmQot(userDetails[1]));
					jsonObject.put("NAME", rmQot(userDetails[2]));
					jsonObject.put("TRACKING_CODE", rmQot(userDetails[3]));
					jsonObject.put("BRAND", rmQot(userDetails[4]));
					jsonObject.put("NFOS_HEADER", rmQot(userDetails[5]));
					jsonObject.put("NFOS_DESC", rmQot(userDetails[6]));

					hashMap.put(RESPONSE, jsonObject);
					hashMap.put(ERROR_CODE, "0");
					hashMap.put(ERROR_MSG, "User Found");
					return hashMap;
				}
				else
				{
					hashMap.put(RESPONSE, jsonObject);
					hashMap.put(ERROR_CODE, "1");
					hashMap.put(ERROR_MSG, "User Not Found");
				}
			}  
		}   
		catch (IOException | JSONException e)   
		{  
			LOG.error("readFileData : Exception: ", e);

			hashMap.put(RESPONSE, jsonObject);
			hashMap.put(ERROR_CODE, "0");
			hashMap.put(ERROR_MSG, "User Not Found");  
		}  
		return hashMap;	
	}

	@Override
	public Map<String, Object> readAndUpdateFileData(String filePath, String id, Map<String, String> dataToUpdate)
	{
		LOG.info("readAndUpdateFileData id:{} ",id);
		HashMap<String, Object> hashMap=new HashMap<>();
		List<String[]> data = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(filePath)))
		{  
			String line;
			while ((line = br.readLine()) != null)  
			{
				String[] userDetails = line.split(",");  

				String userid = rmQot(userDetails[0]);
				if(userid.equals(id))
				{
					data.add(new String[]{id, dataToUpdate.get("CATEGORY"), dataToUpdate.get("NAME"), dataToUpdate.get("TRACKING_CODE"),
							dataToUpdate.get("BRAND"), dataToUpdate.get("NFOS_HEADER"), dataToUpdate.get("NFOS_DESC")});
					
					LOG.info("UPDATING CSV id: {} category: {} Name:{} Tracking_code:{} Brand: {} NFOS_Header: {} NFOS_Desc:{}", userDetails[0], userDetails[1], userDetails[2], userDetails[3], userDetails[4], userDetails[5], userDetails[6]);
				}
				else
				{
					LOG.info("CSV id: {} category: {} Name:{} Tracking_code:{} Brand: {} NFOS_Header: {} NFOS_Desc:{}", userDetails[0], userDetails[1], userDetails[2], userDetails[3], userDetails[4], userDetails[5], userDetails[6]);
					data.add(new String[]{
							rmQot(userDetails[0]),rmQot(userDetails[1]),rmQot(userDetails[2]),rmQot(userDetails[3]),
							rmQot(userDetails[4]),rmQot(userDetails[5]),rmQot(userDetails[6])});
				}
			}  
			
			hashMap.put(RESPONSE, data);
			hashMap.put(ERROR_CODE, "0");
			hashMap.put(ERROR_MSG, "User Found");		
			LOG.info("data: SIZE: {}",data.size());
		}   
		catch (IOException e)   
		{  
			LOG.error("readFileData : Exception: ", e);
			hashMap.put(RESPONSE, data);
			hashMap.put(ERROR_CODE, "0");
			hashMap.put(ERROR_MSG, "User Not Found");  
		}  
		return hashMap;	
	}
	 
	private String rmQot(String word) 
	{
		return word.replaceAll("^\"|\"$", "");
	}
}
