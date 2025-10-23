function initUpload() {

	const SUPPORTED_IMAGE_TYPES = ["image/jpeg", "image/jpg", "image/png", "image/bmp", "image/gif"];
	const MAX_IMAGE_SIZE_MB = 20;
	const fileInput = document.getElementById("file-input");
	const browseBtn = document.getElementById("browse-btn");
	const fileList = document.getElementById("file-list");
	const uploadBox = document.getElementById("upload-box");
	const email = document.querySelector('.email').textContent.trim();
	console.log('emailId:' + email);


	if (!fileInput || !browseBtn || !uploadBox) {
		console.warn("Upload elements not found — upload.js skipped.");
		return;
	}

	browseBtn.addEventListener("click", () => fileInput.click());

	fileInput.addEventListener("change", () => {
		for (const file of fileInput.files) {
			//validate filetype
			if (!isSupportedFile(file)) {
				showToast(`❌ ${file.name} is not a supported document/image type`);
				continue;
			}
			//validate image size
			if (SUPPORTED_IMAGE_TYPES.includes(file.type) && file.size > MAX_IMAGE_SIZE_MB * 1024 * 1024) {
				showToast(`❌ ${file.name} exceeds ${MAX_IMAGE_SIZE_MB} MB limit`);
				continue;
			}
			const li = createFileItem(file);
			fileList.appendChild(li);
			safeUpload(email, file, li);
		}
	});

	uploadBox.addEventListener("dragover", e => e.preventDefault());
	uploadBox.addEventListener("drop", e => {
		e.preventDefault();
		const files = e.dataTransfer.files;
		for (const file of files) {
			//validate filetype
			if (!isSupportedFile(file)) {
				showToast(`❌ ${file.name} is not a supported document/image type`);
				continue;
			}
			//validate image size
			if (SUPPORTED_IMAGE_TYPES.includes(file.type) && file.size > MAX_IMAGE_SIZE_MB * 1024 * 1024) {
				showToast(`❌ ${file.name} exceeds ${MAX_IMAGE_SIZE_MB} MB limit`);
				continue;
			}
			const li = createFileItem(file);
			fileList.appendChild(li);
			safeUpload(email, file, li);

		}
	});


	function createFileItem(file) {
		const li = document.createElement("li");
		li.classList.add("upload-item");

		const fileIcon = document.createElement("span");
		fileIcon.textContent = "📄";
		fileIcon.classList.add("file-icon");

		const fileName = document.createElement("span");
		fileName.textContent = file.name;
		fileName.classList.add("file-name");
		fileName.setAttribute("data-fullname", file.name);

		// Create a single tooltip div in the body
		const tooltip = document.createElement("div");
		tooltip.classList.add("file-tooltip");
		document.body.appendChild(tooltip);

		// Show tooltip on hover
		fileList.addEventListener("mouseover", e => {
			const target = e.target;
			if (target.classList.contains("file-name")) {
				tooltip.textContent = target.dataset.fullname || target.textContent;
				const rect = target.getBoundingClientRect();
				tooltip.style.top = `${rect.top - 28}px`; // above the text
				tooltip.style.left = `${rect.left}px`;
				tooltip.style.display = "block";
			}
		});

		fileList.addEventListener("mouseout", e => {
			if (e.target.classList.contains("file-name")) {
				tooltip.style.display = "none";
			}
		});


		const progress = document.createElement("progress");
		progress.max = 100;
		progress.value = 0;
		progress.classList.add("upload-progress");

		const status = document.createElement("span");
		status.classList.add("upload-status");
		status.textContent = "Waiting...";

		const deleteBtn = document.createElement("button");
		deleteBtn.innerHTML = "🗑";
		deleteBtn.classList.add("delete-btn");
		deleteBtn.addEventListener("click", () => li.remove());

		li.append(fileIcon, fileName, progress, status, deleteBtn);
		return li;
	}

}
function isSupportedFile(file) {
	const ext = file.name.split('.').pop().toLowerCase();
	const supportedExtensions = ["jpg", "jpeg", "png", "bmp", "gif", "tiff", "tif", "pdf"];
	return supportedExtensions.includes(ext);
}

function showToast(message, type = "error") {
    // Find the upload container
    const uploadContainer = document.querySelector(".upload-container");
    if (!uploadContainer) return;

    const toast = document.createElement("div");
    toast.className = `toast ${type}`;
    toast.textContent = message;

    // Append **after** the upload container
    uploadContainer.insertAdjacentElement("afterend", toast);

    // Optional: fade in
    requestAnimationFrame(() => toast.classList.add("show"));

    // Remove after 4 seconds
    setTimeout(() => toast.remove(), 5000);
}

async function safeUpload(email, file, li) {
	try {
		await uploadFileWithResumableGCS(email, file, li);
	} catch (e) {
		console.error("Unexpected upload error:", e);
		const status = li.querySelector(".upload-status");
		if (status) {
			status.textContent = "❌ Upload failed (unexpected)";
		}

	}

}


async function uploadFileWithResumableGCS(email, file, li) {
	const progress = li.querySelector("progress");
	const status = li.querySelector(".upload-status");
	try {
		const encodedObjectName = encodeURIComponent(email + "/" + file.name);
		const encodedType = encodeURIComponent(file.type);
		const resumableUrl = `/initiateResumableUpload?objectName=${encodedObjectName}&contentType=${encodedType}`;

		console.log(resumableUrl);
		const response = await fetch(resumableUrl);
		console.log('status:' + response.status)
		if (!response.ok) {
			status.textContent = "❌ Not uploaded (session URI failed)";
			return;
		}
		const data = await response.json();
		console.log('response json:' + data);
		const uploadUrl = data.uploadUrl;
		console.log('uploadUrl:' + uploadUrl);
		// upload the chunks

		if (!uploadUrl) {
			status.textContent = "❌ Not uploaded (session URI failed)";
			return;
		}

		status.textContent = "Uploading...";

		const chunkSize = 5 * 1024 * 1024; //5MB
		let offset = 0;
		while (offset < file.size) {
			const chunk = file.slice(offset, offset + chunkSize);
			const chunkStart = offset;

			const chunkEnd = Math.min(offset + chunkSize - 1, file.size - 1);
			const totalSize = file.size;

			let success = false;
			let attempts = 0;
			const maxRetries = 3;

			while (!success && attempts < maxRetries) {


				try {
					const res = await fetch("/uploadChunks", {
						method: "POST",
						headers: {
							"Content-Type": 'application/octet-stream',
							"SessionUrl": uploadUrl,
							"Content-Length": chunk.size.toString(),
							"Content-Range": `bytes ${chunkStart}-${chunkEnd}/${totalSize}`,
						},
						body: chunk
					});
					if (res.status === 200) {
						const finalMetadata = await res.text();
						console.log('upload Completed! final metadata received:', finalMetadata);
						progress.value = 100;
						offset = file.size;
						success = true;
					} else {
						if (res.status === 308) {
							offset = offset + chunk.size;
							progress.value = Math.round((offset / totalSize) * 100);
							success = true;
						} else {

							const errorBody = await res.text();
							console.error('GCS Error Details:', errorBody);
							throw new Error(`Chunk upload failed: HTTP ${res.status}`);
						}
					}
				} catch (err) {
					alert(err);
					attempts++;
					console.warn(`Retry ${attempts}/${maxRetries} for chunk at offset ${offset}`);
					if (attempts < maxRetries) {
						await new Promise(r => setTimeout(r, 2000)); //wait before retry
					} else {
						status.textContent = "❌ Upload failed (chunk retry limit)";
						return;
					}
				}
			}
		}
		progress.value = 100;
		status.textContent = "✅ Uploaded successfully";
	} catch (error) {
		console.error("Upload error:", error);
		status.textContent = "❌ Upload failed (network/unknown)";
	}

}



