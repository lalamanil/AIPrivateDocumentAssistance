/**
 * 
 */

function initChat() {

	const sendBtn = document.getElementById("send-query");
	const queryInput = document.getElementById("user-query");
	const chatMessages = document.getElementById("chat-messages");
	const docList = document.getElementById("chat-doc-list");
	const preview = document.getElementById("chat-doc-preview");
	const emailid = document.querySelector(".email").textContent.trim();

	const prevBtn = document.getElementById("chatprevPage");
	const nextBtn = document.getElementById("chatnextPage");
	const pageInfo = document.getElementById("chatpageInfo");


	const btnPreview = document.getElementById("btn-preview");
	const btnSummary = document.getElementById("btn-summary");
	const contentArea = document.getElementById("chat-doc-content");

	const pageSize = 10;
	let currentpage = 1;
	let documents = [];

	let selectedDoc = null;


	preview.style.display = "none";
	btnPreview.style.display = "none";
	btnSummary.style.display = "none";


	sendBtn.addEventListener("click", sendQuery);

	queryInput.addEventListener("input", e => {
		// Clear previous results whenever the input value changes
		docList.innerHTML = "";
		preview.style.display = "none";
		contentArea.innerHTML = "";
		// Hide buttons when query input changes
		btnPreview.style.display = "none";
		btnSummary.style.display = "none";

	});
	queryInput.addEventListener("keypress", e => {

		if (e.key == "Enter") {
			sendQuery();
		}
	});

	async function sendQuery() {
		const query = queryInput.value.trim();
		if (!query) {
			return;
		}
		appendMessage("user", query);
		queryInput.value = "";
		appendMessage("assistant", `<span class="processing-text">Analyzing documents...</span><div class="processing-dots"><span></span><span></span><span></span></div>`);
		const body = {
			userId: emailid,
			userPromptForDocSearch: query,
			relevanceCutoff: 0.4 // we can adjust this or make it configurable
		};
		try {
			const response = await fetch("/userdocsbyprompt", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify(body)
			});
			const docs = await response.json();
			if (!response.ok) {
				throw new Error(`HTTP ${response.status}`);
			}
			if (!Array.isArray(docs.documents) || docs.documents.length === 0) {
				chatMessages.lastChild.textContent = "No relevant documents found.";
				docList.innerHTML = "";
				preview.style.display = "none";
				contentArea.innerHTML = ""; //clear old preview	
				return;
			}
			chatMessages.lastChild.textContent = `Found ${docs.documents.length} matching documents. `;
			docs.documents.forEach(doc => {
				if (doc.shortReason && doc.documentId) {
					const parts = doc.documentId.split("/");
					const actualFileName = parts.length > 1 ? parts[1] : parts[0];
					if (actualFileName) {
						appendMessage("assistant", "<b>" + actualFileName + "</b> : " + doc.shortReason);
					} else {
						appendMessage("assistant", doc.shortReason);
					}
				}
			});
			documents = docs.documents;
			renderDocuments(currentpage);
		} catch (err) {
			console.error("Error retrieving docs:", err);
			chatMessages.lastChild.textContent = "❌ Error retrieving documents.";
		}

	}


	function renderDocuments(page) {
		docList.innerHTML = "";
		contentArea.innerHTML = `<div class="preview-placeholder"><div class="placeholder-icon">📄</div><p>Select a document to preview</p></div>`; //set Place holder
		preview.style.display = "block";
		btnPreview.style.display = "none"; // Keep hidden until a doc is selected
		btnSummary.style.display = "none";

		const start = (page - 1) * pageSize;
		const end = start + pageSize;
		const pageDocs = documents.slice(start, end);


		pageDocs.forEach(doc => {
			const li = document.createElement("li");
			const icon = document.createElement("span");
			icon.classList.add("file-icon");
			icon.textContent = doc.contentType.startsWith("image/") ? "🖼️"
				: "📄";
			const name = document.createElement("span");
			name.textContent = doc.documentId.split("/")[1];
			name.classList.add("file-name");
			li.append(icon, name);
			li.addEventListener("click", () => {
				document.querySelectorAll("#chat-doc-list li").forEach(item => item.classList.remove("active"));
				li.classList.add("active");
				selectedDoc = doc;
				btnPreview.style.display = "inline-block";
				btnSummary.style.display = "inline-block";
				btnPreview.classList.add("active");
				btnSummary.classList.remove("active");

				showPreview(selectedDoc);

			});
			docList.appendChild(li);

		});

		pageInfo.textContent = `Page ${currentpage} of ${Math.ceil(documents.length / pageSize)}`;
		prevBtn.disabled = currentpage === 1;
		nextBtn.disabled = end >= documents.length;
	}


	function showPreview(doc) {

		renderPreviewContent(doc);
	}

	// Function to render the PREVIEW content
	function renderPreviewContent(doc) {
		contentArea.innerHTML = ""; // Clear existing content
		if (doc.contentType.startsWith("image/")) {
			const img = document.createElement("img");
			img.src = doc.signedUrl;
			// The image will be sized by the CSS class .chat-doc-content img
			contentArea.append(img);
		} else if (doc.contentType === "application/pdf") {
			const iframe = document.createElement("iframe");
			iframe.src = doc.signedUrl;
			// The iframe will be sized by the CSS class .chat-doc-content iframe
			contentArea.appendChild(iframe);
		} else {
			contentArea.innerHTML = `<p>Preview not supported for this file type.</p>`;
		}
	}

	// Function to render the SUMMARY content
	function renderSummaryContent(doc) {
		contentArea.innerHTML = ""; // Clear existing content

		const summaryDiv = document.createElement("div");
		summaryDiv.className = "summary-text";

		if (doc.summaryText) {
			summaryDiv.innerHTML = doc.summaryText;
		} else {
			summaryDiv.textContent = "No summary available.";
		}
		contentArea.appendChild(summaryDiv);

		if (doc.audiosignedUrl) {
			const audio = document.createElement("audio");
			audio.controls = true;
			audio.src = doc.audiosignedUrl;
			contentArea.appendChild(audio);
		}
	}

	btnPreview.addEventListener("click", () => {
		if (!selectedDoc) return;
		btnPreview.classList.add("active");
		btnSummary.classList.remove("active");
		renderPreviewContent(selectedDoc);
	});

	btnSummary.addEventListener("click", () => {
		if (!selectedDoc) return;
		btnSummary.classList.add("active");
		btnPreview.classList.remove("active");
		renderSummaryContent(selectedDoc);
	});


	function appendMessage(role, text) {
		const msg = document.createElement("div");
		msg.className = role === "user" ? "chat-msg user" : "chat-msg bot";
		if (role === "assistant") {
			msg.innerHTML = text;
		} else {
			msg.textContent = text;
		}
		chatMessages.appendChild(msg);
		chatMessages.scrollTop = chatMessages.scrollHeight;
	}

	//Event Listener for pagination buttons

	prevBtn.addEventListener("click", () => {
		if (currentpage > 1) {
			currentpage--;
			renderDocuments(currentpage);
		}
	});

	nextBtn.addEventListener("click", () => {
		if (currentpage < Math.ceil(documents.length / pageSize)) {
			currentpage++;
			renderDocuments(currentpage);

		}
	});

}