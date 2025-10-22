/**
 * 
 */

function loadUserDocument() {
	const userEmailId = document.querySelector('.email').textContent.trim();
	const listContainer = document.getElementById("doc-items");
	const previewContainer = document.getElementById("doc-preview");
	const placeholder = document.getElementById("preview-placeholder");

	const prevBtn = document.getElementById("prevPage");
	const nextBtn = document.getElementById("nextPage");
	const pageInfo = document.getElementById("pageInfo");

	const pageSize = 10;
	let currentPage = 1;
	let documents = [];

	function renderPage(page) {
		listContainer.innerHTML = "";
		const start = (page - 1) * pageSize;
		const end = start + pageSize;
		const pageDocs = documents.slice(start, end);
		pageDocs.forEach(doc => {
			const li = document.createElement("li");
			//adding icon based on the file type
			const icon = document.createElement("span");
			icon.classList.add("file-icon");
			icon.textContent = doc.mimeType.startsWith("image/")
				? "🖼️"
				: "📄";
			const name = document.createElement("span");
			name.textContent = doc.name;
			name.classList.add("file-name");
			li.append(icon, name);
			li.addEventListener("click", () => {
				document.querySelectorAll("#doc-items li").forEach(item => item.classList.remove("active"));
				li.classList.add("active");
				placeholder.style.display = "none";
				previewContainer.innerHTML = ""; //clear old prview
				previewContainer.style.display = "block";
				if (doc.mimeType.startsWith("image/")) {
					const img = document.createElement("img");
					img.src = doc.signedUrl;
					previewContainer.appendChild(img);
				} else {
					if (doc.mimeType === "application/pdf") {
						const useGoogleViewer = false; //toggel here if needed
						const iframe = document.createElement("iframe");
						iframe.src = useGoogleViewer ? `https://docs.google.com/gview?url=${encodeURIComponent(doc.signedUrl)}&embedded=true` : doc.signedUrl;
						//							iframe.width = "100%";
						//							iframe.height = "100%";
						//							iframe.style.border = "none";
						previewContainer.appendChild(iframe);
					} else {
						previewContainer.innerHTML = `<p>Preview not supported for this file type: ${doc.mimeType}</p>`;
					}
				}
			});
			listContainer.appendChild(li);
		});

		//Update pagination buttons
		pageInfo.textContent = `Page ${currentPage} of ${Math.ceil(documents.length / pageSize)}`;
		prevBtn.disabled = currentPage === 1;
		nextBtn.disabled = end >= documents.length;
	}

	//Event Listener for pagination buttons
	prevBtn.addEventListener("click", () => {
		if (currentPage > 1) {
			currentPage--;
			renderPage(currentPage);
		}
	});

	nextBtn.addEventListener("click", () => {
		if (currentPage < Math.ceil(documents.length / pageSize)) {
			currentPage++;
			renderPage(currentPage);
		}
	});


	//Back end API call to pull documents from backend
	fetch(`/listuserdocs?userid=${encodeURIComponent(userEmailId)}`)
		.then(response => response.json())
		.then(docs => {
			documents = docs;
			if (documents.lenght == 0) {
				listContainer.innerHTML = "<li>No documents found.</li>";
				return;
			}
			renderPage(currentPage);

		}).catch(err => {
			console.error("Error loading documents:", err);
			listContainer.innerHTML = "<li>Error loading documents</li>";
		});
}