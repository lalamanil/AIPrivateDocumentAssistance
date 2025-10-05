package com.document.personal.assistance.utility;

public class NotNullEmptyUtility {

	public static boolean notNullEmptyCheck(String input) {
		if (null != input && !input.trim().isEmpty() && !"null".equalsIgnoreCase(input.trim())) {

			return true;
		}
		return false;

	}

}
