package coco_pdfhandling;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import org.apache.pdfbox.multipdf.Overlay;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import system.proxies.FileDocument;

public class PDFHandling {
	public static boolean mergePDF(IContext context,List<FileDocument> documents,  IMendixObject mergedDocument ){
		int i = 0;
		PDFMergerUtility  mergePdf = new  PDFMergerUtility();
		for(i=0; i < documents.size(); i++)
		{
		    FileDocument file = documents.get(i);
		    InputStream content = Core.getFileDocumentContent(context, file.getMendixObject());
		    mergePdf.addSource(content);            
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		mergePdf.setDestinationStream(out);
		try {
			mergePdf.mergeDocuments(null);
		} catch (IOException e) {
			throw new RuntimeException("Failed to merge documents" + e.getMessage(), e);
			
		}
		 
		Core.storeFileDocumentContent(context, mergedDocument, new ByteArrayInputStream(out.toByteArray()));

		out.reset();
		documents.clear();
		
		return true;	
	}
	

	/**
	 * Overlay a generated PDF document with another PDF (containing the company stationary for example)
	 * @param context
	 * @param generatedDocumentMendixObject The document to overlay
	 * @param overlayMendixObject The document containing the overlay
	 * @return boolean
	 * @throws IOException
	 */
	public static boolean overlayPdf(IContext context, IMendixObject generatedDocumentMendixObject, IMendixObject overlayMendixObject) throws IOException {	
		ILogNode logger = Core.getLogger("OverlayPdf"); 
		logger.trace("Retrieve generated document");
		PDDocument inputDoc = PDDocument.load(Core.getFileDocumentContent(context, generatedDocumentMendixObject));
		
		logger.trace("Overlay PDF start, retrieve overlay PDF");
		PDDocument overlayDoc = PDDocument.load(Core.getFileDocumentContent(context, overlayMendixObject));
				
		logger.trace("Perform overlay");
		Overlay overlay = new Overlay();
		overlay.setInputPDF(inputDoc);
		overlay.setDefaultOverlayPDF(overlayDoc);
		overlay.setOverlayPosition(Overlay.Position.BACKGROUND);
		
		logger.trace("Save result in output stream");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		overlay.overlay(new HashMap<Integer, String>()).save(baos);
		
		logger.trace("Duplicate result in input stream");
		InputStream overlayedContent = new ByteArrayInputStream(baos.toByteArray());
		
		logger.trace("Store result in original document");
		Core.storeFileDocumentContent(context, generatedDocumentMendixObject, overlayedContent);
		
		logger.trace("Close PDFs");
		overlayDoc.close();
		inputDoc.close();
		
		logger.trace("Overlay PDF end");
		return true;
	}
}
