package com.document.personal.assistance.utility;
/**
@author ANIL LALAM
**/
import java.util.ArrayList;
import java.util.List;

public class TextChunkerUtility {

	public static List<String> chunkText(String text, int maxChars, int overlap) {
		List<String> chunkList = new ArrayList<String>();
		int start = 0;

		int minChunckSize = maxChars / 2;

		while (start < text.length()) {
			if ((text.length() - start) <= maxChars) {
				chunkList.add(text.substring(start));
				break;
			}
			int end = Math.min(start + maxChars, text.length());
			int lastboundary = Math.max(Math.max(text.lastIndexOf(".", end - 1), text.lastIndexOf("!", end - 1)),
					text.lastIndexOf("?", end - 1));
			if (lastboundary > start + minChunckSize) {
				end = lastboundary + 1;
			}
			String chunk = text.substring(start, end).trim();
			if (!chunk.isEmpty()) {
				chunkList.add(chunk);
			}
			start = Math.max(end - overlap, start + 1);
		}
		return chunkList;
	}

}
