package com.document.personal.assistance.utility;
/**
@author ANIL LALAM
**/
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;
import com.document.personal.assistance.constants.ApplicationConstants;
import com.document.personal.assistance.exception.PrivateDocumentException;
import com.google.api.gax.rpc.PermissionDeniedException;
import com.google.api.gax.rpc.UnavailableException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Candidate;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.GenerativeModel;

public class VertexAIGeminiAPI {
	private static final Logger LOGGER = Logger.getLogger(VertexAIGeminiAPI.class.getName());
	private static VertexAI vertexAI;
	static {
		InputStream inputStream = null;
		inputStream = VertexAIGeminiAPI.class.getClassLoader().getResourceAsStream("AI-ServiceAccount.json");
		if (null != inputStream) {
			try {
				GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream)
						.createScoped("https://www.googleapis.com/auth/cloud-platform");
				if (null != credentials) {
					vertexAI = new VertexAI.Builder().setCredentials(credentials)
							.setProjectId(ApplicationConstants.PROJECT_ID).setLocation(ApplicationConstants.LOCATION_ID)
							.build();
				} else {
					LOGGER.info("GoogleCredentials Object is null. Please check application logs");
				}
			} catch (IOException e) {
				// TODO: handle exception
				LOGGER.info("Exception occured while creating Google credentials from ServiceAccount Inputstream:"
						+ e.getMessage());
				e.printStackTrace();
			}
		} else {
			LOGGER.info(
					"Inputstream is null. Please check Service account AI-ServiceAccount.json is present in src/main/resources");
		}

	}

	public static String reRankSummarizeDocuments(String prompttorerankSummarizedocuments) {
		if (null == vertexAI) {
			System.out.println("vertexAI object is null. Please check application logs.");
			throw new PrivateDocumentException("vertexAI object is null. Please check application logs", 500);
		}
		StringBuilder builder = new StringBuilder();
		GenerativeModel generativeModel = new GenerativeModel(ApplicationConstants.modelname, vertexAI);
		try {
			GenerateContentResponse generateContentResponse = generativeModel
					.generateContent(prompttorerankSummarizedocuments);
			if (null != generateContentResponse) {
				List<Candidate> candidateList = generateContentResponse.getCandidatesList();
				if (null != candidateList && !candidateList.isEmpty()) {
					for (Candidate candidate : candidateList) {
						Content content = candidate.getContent();
						if (null != content) {
							List<Part> partList = content.getPartsList();
							if (null != partList && !partList.isEmpty()) {
								for (Part part : partList) {
									builder.append(part.getText());
									builder.append("\r\n");
								}

							}

						}

					}
				}

			}

		} catch (UnavailableException e) {
			// TODO: handle exception
			LOGGER.info("VertexAI Gemini API is unavailable:" + e.getMessage());
			e.printStackTrace();
		} catch (PermissionDeniedException e) {
			// TODO: handle exception
			String message = e.getMessage();
			if (null != message) {
				if (message.contains("has not been used in project") || message.contains("it is disabled")) {
					LOGGER.info("❌ The Vertex API is **not enabled** for this project. Please enable it at:\n"
							+ "👉 https://console.developers.google.com/apis/api/aiplatform.googleapis.com/overview?");
				} else {
					if (message.contains("PERMISSION_DENIED: Permission 'aiplatform.endpoints.predict'")) {
						LOGGER.info(
								"❌ Access Denied: The service account does not have permission to use VertexAI.Missing Role:Vertex AI User (`roles/aiplatform.user`)");
						LOGGER.info(
								"👉 Reason: The service account is missing the necessary IAM role to perform predictions using Vertex AI models, such as Gemini.");
					} else {
						LOGGER.info("❗ Permission Denied: " + message);
						LOGGER.info("❗ Unrecognized permission error. Please check IAM and API status.");
					}
				}
			} else {
				LOGGER.info("❗ Permission denied but no error message was provided.");
				e.printStackTrace();
			}

		} catch (IOException e) {
			// TODO: handle exception
			LOGGER.info("if an I/O error occurs while making the API call:" + e.getMessage());
			e.printStackTrace();
		}

		return builder.toString();

	}

	public static void main_(String[] args) {

		String prompt = "You are a document relevance re-ranking model. Your task is to re-rank the following retrieved document chunks based on their semantic relevance to the user query.\n"
				+ "User query:\n" + "Please provide the documents related Hackathons Certificates\n"
				+ "Candidate documents:\n"
				+ "{\"documents\":[{\"id\":\"lalamanilbabu@gmail.com/IEEE Certificate of Appreciation - DevHack-participant-Anil Lalam.pdf\",\"text\":\"OF ELECTRICAL\\nINSTITUTE\\nIEEE\\nCERTIFICATE OF APPRECIATION\\nPresented To\\nAnil Lalam\\nin recognition and appreciation of your valued\\nservices and contributions as\\nIEEE 2025 AI-DEVHACK HACKATHON PARTICIPANT\\nAND\\nELECTRONICS\\nFOUNDED\\nNEW YORK\\n1884\\nENGINEERS\\nColle\\nGora DATTA, FHL7\\nChairman IEEE Orange County Computer Society\\nOct 19, 2025\\n\\n\"},{\"id\":\"lalamanilbabu@gmail.com/DocumentAssistantDemoScript.jpeg\",\"text\":\".b\\nTam ANIL LALAM. the project\\nI did for Hackathon is Al-powered\\npersonal o\\ndocument Assistant-\\nIn today's digital - first world,\\naccumulate\\nIndividuals & Organizatory.\\navast number of digital document.\\nThese documents are typically stored\\nPn Cloud drives\\nA\\nEmail attachments,\\nOr) local folders. While these storage\\nSolution provide accesibility,\\nthe\\nChallenge of finding the right\\ndowment at the night time remain\\nUnresolved.\\nAl powered document assistant\\nhelps users to upload, process &\\nretrive private documents using\\nHow Google Cloud At Series.\\nThe System automatically\\nExtract text, Create Vector Embedd\\n-ings &\\nallous Users to Query\\ndocuments in natural language.\\nDemo :-\\nThe problem statement\\nI am trying\\nSolve j\\nFinding the night document\\nUser\\nbased on\\nat the night time\\nin natural language.\\nQuery\\nLet me give you a\\nwhat motivated\\nVector\\nback ground\\nto do this project\\n\"},{\"id\":\"lalamanilbabu@gmail.com/I-200-22033-870840.pdf\",\"text\":\"§\\nNC00471966@TECHMAHINDRA.COM\\nL. U.S. Government Agency Use (ONLY)\\n2. First (given) name §\\nNAVEEN KUMAR\\nBy virtue of the signature below, the Department of Labor hereby acknowledges the following:\\n3. Middle initial\\nN/A\\n2/9/2022\\nThis certification is valid from\\nCertifying Officer\\n2/8/2025\\nto\\n2/9/2022\\nDepartment of Labor, Office of Foreign Labor Certification\\n1-200-22033-870840\\nCase number\\nCertification Date (date signed)\\nCertified\\nCase Status\\nThe Department of Labor is not the guarantor of the accuracy, truthfulness, or adequacy of a certified LCA.\\nM. Signature Notification and Complaints\\nThe signatures and dates signed on this form will not be filled out when electronically submitting to the Department of Labor for processing,\\nbut MUST be complete when submitting non-electronically. If the application is submitted electronically, any resulting certification MUST be\\nsigned immediately upon receipt from DOL before it can be submitted to USCIS for final processing.\"},{\"id\":\"lalamanilbabu@gmail.com/PetitionDocuments_1051779.pdf\",\"text\":\"agent) of this application.\\n1. Last (family) name §\\nREDDY\\n4. Firm/Business name §\\nTECH MAHINDRA (AMERICAS), INC\\n5. E-Mail address §\\nLR00821941@TECHMAHINDRA.COM\\nL. U.S. Government Agency Use (ONLY)\\n2. First (given) name §\\nLOKESH CHANGALRAI\\nBy virtue of the signature below, the Department of Labor hereby acknowledges the following:\\n7/11/2024\\nThis certification is valid from\\nCertifying Offer\\n7/10/2027\\nto\\n7/18/2024\\n3. Middle initial\\nN/A\\nDepartment of Labor, Office of Foreign Labor Certification\\nI-200-24193-188383\\nCase number\\nCertification Date (date signed)\\nCertified\\nCase Status\\nThe Department of Labor is not the guarantor of the accuracy, truthfulness, or adequacy of a certified LCA.\\nM. Signature Notification and Complaints\\nThe signatures and dates signed on this form will not be filled out when electronically submitting to the Department of Labor for processing,\\nbut MUST be complete when submitting non-electronically.\\n---\\nthat any supporting evidence submitted in support of this petition may be\\nverified by USCIS through any means determined appropriate by USCIS, including but not limited to, on-site compliance reviews.\\nIf filing this petition on behalf of an organization, I certify that I am authorized to do so by the organization.\\nI certify, under penalty of perjury, that I have reviewed this petition and that all of the information contained in the petition, including\\nall responses to specific questions, and in the supporting documents, is complete, true, and correct.\\n1.\\nName and Title of Authorized Signatory\\nFamily Name (Last Name)\\nShukla\\nTitle\\nGroup Manager - HR/Immigration\\n2.\\nSignature and Date\\nSignature of Authorized Signatory\\nМаши\\n3.\\nSignatory's Contact Information\\nDaytime Telephone Number\\nEmail Address (if any)\\n(214) 974-9907\\nUlpa.Shukla@TechMahindra.\\n---\\nto is the following documentation:\\nEducational Equivalency Evaluation\\nEducational Documentation\\n•\\nVarious Employment Letters evidencing career experience\\nTerms of Employment\\nBoth Tech Mahindra and Mr. Lalam understand the temporary nature of the offered\\nemployment. He will be compensated at an annual salary of at least $107,495. For the\\nreasons outlined above, we respectfully request that the enclosed H-1B petition be approved.\\nSincerely,\\nИвели\\nUlpa Shukla\\nGroup – Manager – HR/Immigration\\n\\nEDUCATION EQUIVALENCY\\nEVALUATION\\nm\\nMORNINGSIDE\\nEVALUATIONS\\nFebruary 1, 2022\\nName:\\nInstitution:\\nCountry:\\nDegree:\\nLength of Program:\\nDate of Qualification:\\nUS Academic Equivalent:\\nDEGREE:\\nLALAM ANIL BABU\\nAndhra University\\nIndia\\nBachelor of Technology\\nFour years\\n2014\\n000950987 BABU_594811\\nEVALUATION OF ACADEMIC CREDENTIALS\\nBACHELOR OF SCIENCE IN COMPUTER SCIENCE\\nGraduation from high school and competitive entrance examination scores are requirements for admission and\\nenrollment in Andhra University, an accredited institution o\"},{\"id\":\"lalamanilbabu@gmail.com/AI-Powered Personal Document Assistance System.pdf\",\"text\":\"\\\" failed with errors:\\\" + error);\\nLOGGER.info(\\\"Inserted \\\" + rowData.size() + \\\" rows to \\\" + tableId);\\n} catch (BigQuery Exception e) {\\n}\\n} else {\\n}\\n// TODO: handle exception\\ne.printStackTrace();\\nLOGGER.info(\\\"bigQuery object is null. Please check application logs\\\");\\nQuery & Retrieval\\n1. User → Enters natural-language query → Web UI\\nThe user submits a natural-language question via the web Ul.\\nA \\\"View Documents\\\" option displays previously uploaded files.\\nMenu\\nUpload Documents\\nView Documents\\nRetrieve Documents\\nWelcome to Private Document Assistant\\nYour Documents\\n=\\nDeploying a Spring B...\\nPrerequisite\\n1/11\\n100%\\nANIL LALAM\\nlalamanilbabu@gmail.com\\nb с\\n↓\\n594811_Btech_OD.pdf\\n594811 CMM.pdf\\nAlPowered VideoSummarization...\\nANILLALAMLatestPassport.pdf\\nBuilding Accident_Detection_S...\\nCANADARecentVisa.pdf\\nCanadaVisa.pdf\\nCarlnsurancedocuments.pdf\\nCarlnsurancequote.pdf\\nDeployingaSpringBootApplicati...\\n---\\n= Math.round((offset / totalSize) * 100);\\nsuccess = true;\\n} else {\\nconst errorBody = await res.text();\\nconsole.error('GCS Error Details:', errorBody);\\nthrow new Error(Chunk upload failed: HTTP ${res.status}`);\\n}\\n}\\nDocuments are stored in GCS Bucket\\n+\\n← Bucket details\\n□ documentassistance\\nLocation\\nStorage class\\nus-central1 (Iowa) Standard\\nPublic access\\nNot public\\nProtection\\nSoft Delete\\nObjects\\nConfiguration\\nPermissions\\nFolder browser\\ndocumentassistance\\nlalamanilbabu@gmail.com/\\nGo to path\\nCRefresh\\nLearn\\nProtection\\nLifecycle\\nObservability\\nInventory Reports\\nOperations\\nK\\nBuckets\\n> documentassistance >\\nlalamanilbabu@gmail.com\\nCreate folder Upload▾\\nTransfer data ▾\\nOther services ▾\\nFilter by name prefix only ▼\\nFilter Filter objects and folders\\nShow Live objects only ▼\\n☐\\nName\\n☐\\n☐\\nSize\\n416.8 KB\\n656.5 KB\\nType\\nPERSONALization\\n6 MB\\n☐\\n868.6 KB\\n25 MB\\n☐\\n293 KB\\n■ ConadoVico ndf\\n1.6 MB\\nDeployingaSpring BootApplication...\\n371.3 KB\\n708.2 KB\\n7.\\n---\\nUpload Documents\\nView Documents\\nWelcome to Private Document Assistant\\nMatched Documents\\nRetrieve Documents\\nPrivateDocumentSearchAssist...\\nBuilding Accident_Detection...\\nAlPowered VideoSummarizatio...\\nDeployingaSpringBootApplicat...\\nL Document Assistance.jpg\\nPlease provide the documents\\nrelated Al projects\\n1/28\\nFound 5 matching documents.\\nANIL LALAM\\nlalamanilbabu@gmail.com\\nF\\nAl-Powered Accident Detection System Using Java and Google Cloud Vertex\\nAl\\n1. Introduction\\nRoad accidents claim over a million lives every year worldwide, with countless\\nmore left injured. Timely accident detection can drastically reduce response times\\nfor emergency services, potentially saving lives and minimizing damage.\\nIn this article, I'll walk you through how I built an Al-powered accident detection\\nsystem-entirely using Java for preprocessing, cloud integration, and\\nprediction calls-combined with Google Cloud Vertex Al for training and\\ndeploying a custom object detection model.\"},{\"id\":\"lalamanilbabu@gmail.com/594811_Btech_OD.pdf\",\"text\":\"S.No. 006674\\nNAAC VERSITY\\nA-Grade\\nRegister No.\\n310126511026\\nSANDHRA UNIVERSITY\\nVERSIT/SO\\nవిశ్వకళ\\nDIVERSIT\\nUNIVER\\nపరిషత్\\nERSITY\\nANDHRA\\nఆం\\nANDHRA UNY\\nUNIVERSITY\\n899 0.45696, ANDHRA UNIVERSE BASGOES ANDHRA UNIVERSITY\\nపరిషత్\\nFACULTY OF ENGINEERING\\nOPES UNIVERSITY\\nఫ్యాకల్టీ ఆఫ్ ఇంజనీరింగ్ ANDHRA UN/Eaeir re\\nANDHRA UNIVERSITY Sead s0065 ANDHRA UNIVERSIT (80904pmases\\nSugod Stade ANDHRA UN This is to certify that 65 ANDHEA UNIVERSITY\\nఆంధ్ర\\nకాపరిషత్\\nధృవీకరణ\\nMr./Ms.\\nSon/Daughter of -\\nLALAM ANIL BABU\\nLALAM RAJA BABU\\nhas been duly admitted to the Degree of\\nin\\nBachelor of Technology\\nInformation Technology\\nBranch\\nin this University, he/she having been declared to have passed the Examination prescribed\\ntherefor in\\nAPRIL, 2014\\nFIRST CLASS\\nin\\nin English Medium.\\nఈ విశ్వవిద్యాలయం నుంచి బ్యాచులర్ ఆఫ్ టెక్నాలజీ పట్టా ప్రదానానికి అర్హత పొందినందున అతడు/ఆమె పట్టాకు నిర్దేశించిన పరీక్షలో\\nఉత్తీర్ణత పొందినట్లు ప్రకటించబడింది.\\nsogudda45 ANDHR Given under the Seal of the University\\nవిశ్వవిద్యాలయం అధికార ముద్రతో జారీచేయబడినది.\"},{\"id\":\"lalamanilbabu@gmail.com/594811_CMM.pdf\",\"text\":\"33\\nSerial No. 014607\\nOfficial Memo No. E VISup)/2014 ANDHRA UNIVERSITY\\nRegister No.\\n310126511026\\nB.E./B.Tech Degree Provisional Certificate cum Consolidated Memorandum of Grades\\nThis is to certify that Mr./Ms. LALAM ANIL BABU S/0/D/O LALAM RAJA BABU has qualified\\nhimself/herself for the Degree of Bachelor of Technology (Information Technology) award of this university, he/she\\nhaving been declared to have passed the examinations prescribed therefor held in APRIL, 2014 and that he/she has\\ndone all that is necessary for the formal presentation of the Degree.\\nThe following grades were awarded to the candidate :\\nENGLISH\\nMATHEMATICS-1\\nMATHEMATICS-11\\nCOMPUTER PROG.\"},{\"id\":\"lalamanilbabu@gmail.com/I-200-24193-188383.pdf\",\"text\":\"Last (family) name §\\n2. First (given) name §\\nLOKESH CHANGALRAI\\n3. Middle initial\\nN/A\\nREDDY\\n4. Firm/Business name §\\nTECH MAHINDRA (AMERICAS), INC\\n5. E-Mail address §\\nLR00821941@TECHMAHINDRA.COM\\nL. U.S. Government Agency Use (ONLY)\\nBy virtue of the signature below, the Department of Labor hereby acknowledges the following:\\nThis certification is valid from\\nto\\nDepartment of Labor, Office of Foreign Labor Certification\\nI-200-24193-188383\\nCase number\\nCertification Date (date signed)\\nIn Process\\nCase Status\\nThe Department of Labor is not the guarantor of the accuracy, truthfulness, or adequacy of a certified LCA.\\nM. Signature Notification and Complaints\\nThe signatures and dates signed on this form will not be filled out when electronically submitting to the Department of Labor for processing,\\nbut MUST be complete when submitting non-electronically.\"},{\"id\":\"lalamanilbabu@gmail.com/AIPoweredVideoSummarizationandMultilingualNarration.pdf\",\"text\":\" this project for **non-commercial purposes**.\\n- You must **give appropriate credit** to the author.\\n- For commercial inquiries or licensing, please contact the author.\\nView full license: [CC BY-NC 4.0](https://creativecommons.org/licenses/by-\\nnc/4.0/)\\nAuthor: Anil Lalam\\n\\n\"}]}\n"
				+ "Instructions:1.Carefully read the query and each document text.\n"
				+ "2.Rank all documents from most relevant to least relevant.\n"
				+ "3. Provide output in **JSON** format with the following structure:\n"
				+ "{“rankedDocuments\": [{\"id\": \"<document id>\",\"relevanceScore\": <float between 0 and 1>,\"shortReason\": \"<brief reason for the score>\" }]}\n"
				+ "The higher the relevanceScore, the more semantically relevant the document is to the query.";

		String response = reRankSummarizeDocuments(prompt);

		System.out.println(response);

	}

}
