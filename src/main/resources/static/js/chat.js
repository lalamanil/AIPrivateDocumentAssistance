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

	const pageSize = 10;
	let currentpage = 1;
	let documents = [];


	preview.style.display = "none";

	sendBtn.addEventListener("click", sendQuery);

	queryInput.addEventListener("input", e => {
		// Clear previous results whenever the input value changes
		docList.innerHTML = "";
		preview.style.display = "none";
		preview.innerHTML = "";
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
		appendMessage("assistant", "Searching your documents...");
		const body = {
			userId: emailid,
			userPromptForDocSearch: query,
			relevanceCutoff: 0.37 // we can adjust this or make it configurable
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
				preview.innerHTML = ""; //clear old preview	
				return;
			}
			chatMessages.lastChild.textContent = `Found ${docs.documents.length} matching documents.`;
			documents = docs.documents;
			renderDocuments(currentpage);
		} catch (err) {
			console.error("Error retrieving docs:", err);
			chatMessages.lastChild.textContent = "❌ Error retrieving documents.";
		}

	}


	function renderDocuments(page) {
		docList.innerHTML = "";
		preview.innerHTML = `<div class="preview-placeholder"><div class="placeholder-icon">📄</div><p>Select a document to preview</p></div>`; //set Place holder
		preview.style.display = "block";

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
				preview.innerHTML = ""; //clear old preview
				preview.style.display = "block";
				if (doc.contentType.startsWith("image/")) {
					const img = document.createElement("img");
					img.src = doc.signedUrl;
					preview.appendChild(img);
				} else {
					if (doc.contentType === "application/pdf") {
						const useGoogleViewer = false; //toggel here if needed
						const iframe = document.createElement("iframe");
						iframe.src = useGoogleViewer ? `https://docs.google.com/gview?url=${encodeURIComponent(doc.signedUrl)}&embedded=true` : doc.signedUrl;
						preview.appendChild(iframe);
					} else {
						preview.innerHTML = `<p>Preview not supported for this file type: ${doc.mimeType}</p>`
					}
				}
			});
			docList.appendChild(li);

		});

		pageInfo.textContent = `Page ${currentpage} of ${Math.ceil(documents.length / pageSize)}`;
		prevBtn.disabled = currentpage === 1;
		nextBtn.disabled = end >= documents.length;
	}


	function appendMessage(role, text) {
		const msg = document.createElement("div");
		msg.className = role === "user" ? "chat-msg user" : "chat-msg bot";
		msg.textContent = text;
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