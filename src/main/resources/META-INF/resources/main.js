"use strict";

(async function () {
  let form = {
    files: [],
    features: [],
    target: null,
    force: false,
  };

  // DOM elements
  const submitBtn = document.getElementById("submit");
  const downloadBtn = document.getElementById("download");
  const timeToLiveNode = document.getElementById("time-to-live");
  const filesTable = document.getElementById("files");
  const featuresTable = document.getElementById("features");
  const spinner = document.getElementById("spinner");
  const responseTable = document.getElementById("response");
  const resultsSection = document.getElementById("resultsSection");
  const dropzone = document.getElementById("dropzone");
  const fileInput = document.getElementById("fileInput");

  // Initialize
  try {
    const { minSupportedJDK, maxSupportedJDK, features } = (
      await axios.get("/api/capabilities")
    ).data;

    renderTargetDropdown(minSupportedJDK, maxSupportedJDK);
    renderFeaturesGrid(features);
    addEventListeners();
  } catch (error) {
    console.error("Failed to initialize:", error);
  }

  function checkForm() {
    const hasFiles = form.files.length > 0;
    const hasFeatures = form.features.length > 0;
    const noDuplicates = !form.files.some(
      (e1) => form.files.filter((e2) => e1.name === e2.name).length > 1
    );
    submitBtn.disabled = !(hasFiles && hasFeatures && noDuplicates);
  }

  function renderTargetDropdown(min, max) {
    const dropdown = document.getElementById("target");
    dropdown.innerHTML = "";
    for (let i = min; i <= max; i++) {
      dropdown.innerHTML += `<option value="${i}">JDK ${i}</option>`;
    }
  }

  function renderFeaturesGrid(features) {
    const descriptions = {
      TryWithResources: "Automatic resource management",
      Lambda: "Lambda expressions & functional interfaces",
      Concat: "String concatenation optimization",
      NestMates: "Nest-based access control",
      Record: "Record classes (data carriers)",
    };

    featuresTable.innerHTML = "";
    Object.keys(features)
      .sort((a, b) => features[a] - features[b])
      .forEach((feature) => {
        form.features.push(feature);
        const desc = descriptions[feature] || feature;
        featuresTable.innerHTML += `
                <div class="feature-card active" data-feature="${feature}">
                    <div class="feature-header">
                        <span class="feature-name">${feature}</span>
                        <span class="feature-badge">Java ${features[feature]}+</span>
                    </div>
                    <div class="feature-description">${desc}</div>
                </div>
            `;
      });
  }

  function renderFilesTable() {
    filesTable.innerHTML = "";
    if (form.files.length === 0) return;

    form.files.forEach((file, index) => {
      const isDuplicate =
        form.files.filter((e) => e.name === file.name).length > 1;
      const sizeKB = (file.size / 1024).toFixed(1);
      const ext = file.name.split(".").pop().toUpperCase();

      filesTable.innerHTML += `
                <div class="file-item ${
                  isDuplicate ? "duplicate" : ""
                }" data-index="${index}">
                    <div class="file-info">
                        <div class="file-icon">${ext}</div>
                        <div class="file-details">
                            <div class="file-name">${file.name}</div>
                            <div class="file-size">${sizeKB} KB</div>
                        </div>
                    </div>
                    <button class="file-remove" data-index="${index}">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>
            `;
    });
  }

  function addEventListeners() {
    // Dropzone click
    dropzone.addEventListener("click", () => fileInput.click());

    // File input change
    fileInput.addEventListener("change", (e) => {
      form.files = [...form.files, ...Array.from(e.target.files)];
      renderFilesTable();
      checkForm();
    });

    // Drag and drop
    dropzone.addEventListener("dragover", (e) => {
      e.preventDefault();
      dropzone.classList.add("dragover");
    });

    dropzone.addEventListener("dragleave", () => {
      dropzone.classList.remove("dragover");
    });

    dropzone.addEventListener("drop", (e) => {
      e.preventDefault();
      dropzone.classList.remove("dragover");
      form.files = [...form.files, ...Array.from(e.dataTransfer.files)];
      renderFilesTable();
      checkForm();
    });

    // Remove file
    filesTable.addEventListener("click", (e) => {
      const btn = e.target.closest(".file-remove");
      if (btn) {
        const index = parseInt(btn.dataset.index);
        form.files.splice(index, 1);
        renderFilesTable();
        checkForm();
      }
    });

    // Feature selection
    featuresTable.addEventListener("click", (e) => {
      const card = e.target.closest(".feature-card");
      if (card) {
        card.classList.toggle("active");
        const feature = card.dataset.feature;
        if (card.classList.contains("active")) {
          if (!form.features.includes(feature)) {
            form.features.push(feature);
          }
        } else {
          form.features = form.features.filter((f) => f !== feature);
        }
        checkForm();
      }
    });

    // Submit button
    submitBtn.addEventListener("click", submitForm);
  }

  async function submitForm() {
    // Auto-clear previous results
    resultsSection.hidden = true;
    responseTable.innerHTML = "";

    spinner.hidden = false;
    submitBtn.disabled = true;
    downloadBtn.hidden = true;
    timeToLiveNode.hidden = true;

    // Show results section with loading state
    resultsSection.hidden = false;
    responseTable.innerHTML =
      '<div class="alert alert-info"><div class="alert-title">Processing</div>Transforming your files...</div>';

    const body = new FormData();
    form.files.forEach((file) => body.append("files[]", file));

    form.target = document.getElementById("target").value;
    form.force = document.getElementById("force").checked;
    body.append("target", form.target);
    body.append("force", form.force);
    body.append("features", form.features);

    const config = { headers: { "Content-Type": "multipart/form-data" } };

    try {
      const response = await axios.post("/api/env/", body, config);
      renderResponse(response.data);
    } catch (error) {
      handleError(error);
    } finally {
      spinner.hidden = true;
      submitBtn.disabled = false;
      responseTable.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  }

  function renderResponse(data) {
    const { success, ttl, envid, logs } = data;

    let html = "";

    if (success) {
      downloadBtn.hidden = false;
      downloadBtn.setAttribute("href", `api/env/${envid}`);
      const expireDate = new Date(Date.now() + ttl);
      const format = `${String(expireDate.getHours()).padStart(
        2,
        "0"
      )}:${String(expireDate.getMinutes()).padStart(2, "0")}`;
      timeToLiveNode.textContent = `Available until ${format}`;
      timeToLiveNode.hidden = false;

      html += `
                <div class="alert alert-success">
                    <div class="alert-title">Transformation Successful</div>
                    <p>Files backported to JDK ${form.target}</p>
                    <div class="stats">
                        <span class="stat-badge">${form.files.length} files</span>
                        <span class="stat-badge">${form.features.length} features</span>
                    </div>
                </div>
            `;
    } else {
      html += `
                <div class="alert alert-warning">
                    <div class="alert-title">Completed with Issues</div>
                    <p>Some features could not be transformed. Enable Force Mode to proceed anyway.</p>
                </div>
            `;
    }

    if (logs.length > 0) {
      html += `
                <table class="logs-table">
                    <thead>
                        <tr>
                            <th>Level</th>
                            <th>Message</th>
                        </tr>
                    </thead>
                    <tbody>
            `;

      logs.forEach((log) => {
        const levelClass = `log-${log.type.toLowerCase()}`;
        html += `
                    <tr>
                        <td><span class="${levelClass}">${log.type}</span></td>
                        <td>${log.message}</td>
                    </tr>
                `;
      });

      html += `
                    </tbody>
                </table>
            `;
    }

    responseTable.innerHTML = html;
  }

  function handleError(error) {
    let html = "";
    if (error.response) {
      const message = error.response.data.replace(/(?:\r\n|\r|\n)/g, "<br>");
      html = `
                <div class="alert alert-danger">
                    <div class="alert-title">Transformation Failed</div>
                    <p><strong>Error ${error.response.status}:</strong> ${error.response.statusText}</p>
                    <div>${message}</div>
                    <p class="form-hint">Try enabling Force Mode or check file compatibility.</p>
                </div>
            `;
    } else {
      html = `
                <div class="alert alert-danger">
                    <div class="alert-title">Request Failed</div>
                    <p>${error.message}</p>
                </div>
            `;
    }
    responseTable.innerHTML = html;
  }
})().catch((error) => {
  console.error("App initialization failed:", error);
});
