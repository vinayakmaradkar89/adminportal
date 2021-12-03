package com.admin.portal.core.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
				LOG.info("Asset Not Avaliable");
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
}
