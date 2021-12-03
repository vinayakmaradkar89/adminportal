package com.admin.portal.core.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;

public interface SaveDataInCSVService
{
	public Map<String, String> uploadFileToDAMLocation(String filePath, ResourceResolver resourceResolver,String fileName) throws IOException;
    public String createAndWriteDataInCSV( List<String[]> data) throws IOException;
    String appendDatatoCSVFile(List<String[]> data, String existingCsvFile) throws IOException;
	public Map<String, String> readFileFromDamLocation(String fileName, ResourceResolver resourceResolver, String damFileName) throws IOException;
	public Map<String, String> updateFileToDAMLocation(String filePath, ResourceResolver resourceResolver, String fileName, String damFilePath) throws IOException;
	public Map<String, Object> readFileData(String filePath, String id);
	public Map<String, Object> readAndUpdateFileData(String filePath, String id, Map<String, String> dataToUpdate);

}
