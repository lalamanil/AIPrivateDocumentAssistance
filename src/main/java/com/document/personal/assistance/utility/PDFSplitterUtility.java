package com.document.personal.assistance.utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;

public class PDFSplitterUtility {

	private static final Logger LOGGER = Logger.getLogger(PDFSplitterUtility.class.getName());

	public static String getRawTextBySplitingPdfDocOCR(byte[] content, String contentType) {

		StringBuilder finalRawText = new StringBuilder();
		PdfReader pdfReader = null;
		PdfDocument pdfDocument = null;
		try {
			pdfReader = new PdfReader(new ByteArrayInputStream(content));
			pdfDocument = new PdfDocument(pdfReader);
			int totalPages = pdfDocument.getNumberOfPages();
			int start = 1;
			while (start <= totalPages) {
				int end = Math.min(start + 14, totalPages);
				PdfWriter pdfWriter = null;
				ByteArrayOutputStream baos = null;
				try {
					baos = new ByteArrayOutputStream();
					pdfWriter = new PdfWriter(baos);
					PdfDocument splitpdf = new PdfDocument(pdfWriter);
					pdfDocument.copyPagesTo(start, end, splitpdf);
					splitpdf.close();
					byte[] chunkBytes = baos.toByteArray();

					System.out.println("chunkbytes:" + chunkBytes.length);
					String chunkRawText = DocumentOCROnlineInferenceUtility.processDocument(chunkBytes, contentType);
					if (null != chunkRawText && !chunkRawText.trim().isEmpty()) {
						finalRawText.append(chunkRawText).append("\n");
					}
				} finally {
					if (null != pdfWriter) {
						try {
							pdfWriter.close();
							LOGGER.info("Closing the pdfwriter");
						} catch (IOException e) {
							// TODO: handle exception
							e.printStackTrace();
						}
					}

					if (null != baos) {
						try {
							baos.close();
							LOGGER.info("Closing byte array output stream..");
						} catch (IOException e) {
							// TODO: handle exception
							e.printStackTrace();
						}

					}
				}

				start = end + 1;
			}
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			if (null != pdfDocument) {
				pdfDocument.close();
			}
			if (null != pdfReader) {
				try {
					pdfReader.close();
					LOGGER.info("Closing the pdfReader..");
				} catch (IOException e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		}

		return finalRawText.toString();
	}

}
