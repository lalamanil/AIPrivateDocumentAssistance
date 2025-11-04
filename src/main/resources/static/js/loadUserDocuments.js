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
	
	
  const summaryContainer = document.getElementById("summary-view");
  const summaryContent = document.getElementById("summary-content");
  const summaryAudio = document.getElementById("summary-audio");
  
  const toggleContainer = document.getElementById("toggle-container");
  const previewToggle = document.getElementById("previewToggle");
  const summaryToggle = document.getElementById("summaryToggle");

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
				
				toggleContainer.style.display="flex";
				previewToggle.classList.add("active");
				summaryToggle.classList.remove("active");
				previewContainer.style.display = "block";
                summaryContainer.style.display = "none";
					
				//logic to render preview
				const renderPreview = () => {
					previewContainer.innerHTML = "";
					if (doc.mimeType.startsWith("image/")) {
					const img = document.createElement("img");
					img.src = doc.signedUrl;
					previewContainer.appendChild(img);
				} else {
					if (doc.mimeType === "application/pdf") {
						const useGoogleViewer = false; //toggel here if needed
						const iframe = document.createElement("iframe");
						iframe.src = useGoogleViewer ? `https://docs.google.com/gview?url=${encodeURIComponent(doc.signedUrl)}&embedded=true` : doc.signedUrl;
						
						  // ensure iframe fills the viewer area
                          iframe.style.width = "100%";
                          iframe.style.height = "100%";
						previewContainer.appendChild(iframe);
					} else {
						previewContainer.innerHTML = `<p>Preview not supported for this file type: ${doc.mimeType}</p>`;
					}
				}		
				};
				
				
	const renderSummary = () => {
          summaryContainer.style.display = "flex";
          previewContainer.style.display = "none";

      if(doc.summaryText ){
	    summaryContent.innerHTML =doc.summaryText ;
      }else{
	   summaryContent.textContent="No summary available.";
      }
          summaryAudio.innerHTML = "";

          if (doc.audiosignedUrl) {
            const audio = document.createElement("audio");
            audio.controls = true;
            audio.src = doc.audiosignedUrl;
            summaryAudio.appendChild(audio);
          } else {
            summaryAudio.innerHTML = `<p style="color:gray;">Audio not available</p>`;
          }
        };
				//default: show preview
				renderPreview();
				
	  previewToggle.onclick = () => {
          previewToggle.classList.add("active");
          summaryToggle.classList.remove("active");
          summaryContainer.style.display = "none";
          previewContainer.style.display = "block";
          renderPreview();
        };

      summaryToggle.onclick = () => {
          summaryToggle.classList.add("active");
          previewToggle.classList.remove("active");
          renderSummary();
        };
				
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

	//Get spinner reference
	const spinner = document.getElementById("doc-spinner");

	//show spinner before API call
	spinner.style.display = "flex";
	listContainer.style.display = "none";



	//Back end API call to pull documents from backend
	fetch(`/listuserdocs?userid=${encodeURIComponent(userEmailId)}`)
		.then(response => response.json())
		.then(docs => {
			spinner.style.display = "none"; //hide spinner
			listContainer.style.display = "block"; //show list
			documents = docs;
			if (documents.lenght == 0) {
				listContainer.innerHTML = "<li>No documents found.</li>";
				return;
			}
			renderPage(currentPage);

		}).catch(err => {
			spinner.style.display = "none";
			listContainer.style.display = "block";
			console.error("Error loading documents:", err);
			listContainer.innerHTML = "<li>Error loading documents</li>";
		});
}