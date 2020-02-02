'use strict';

(async function() {
    const {
        minSupportedJDK, maxSupportedJDK, features
    } = (await axios.get('/api/capabilities')).data;

    const form = {
        files: [],
        features: [] 
    };

    const submitBtn = document.getElementById('submit');
    submitBtn.disabled = true;
    const downloadBtn = document.getElementById('download');
    downloadBtn.hidden = true;
    const timeToLiveNode = document.getElementById('time-to-live');
    timeToLiveNode.hidden = true;

    const filesTable = document.getElementById('files');
    const featuresTable = document.getElementById('features');
    const spinner = document.getElementById('spinner');
    const responseTable = document.querySelector("#response");

    const checkForm = () => {
        const canSubmit = 
            form.features.length &&
            form.files.length &&
            !form.files.some(e1 => {
                return form.files.filter(e2 => e1.name === e2.name).length > 1
            });
        submitBtn.disabled = !canSubmit;
    };

    const submitForm = async () => {
        const body = new FormData();
        form.files.forEach(file => {
            body.append("files[]", file);
        });
        const target = document.getElementById('target').value;
        body.append('target', form.target = target);
        const force = document.getElementById('force').checked;
        body.append('force', form.force = force);
        body.append('features', form.features);
        
        const config = { headers: { 'Content-Type': 'multipart/form-data' } };
        try {
            renderResponseTable((
                await axios.post('/api/env/', body, config)
            ).data);
        } catch (error) {
            responseTable.innerHTML = '';
            const message = error.response.data.replace(/(?:\\r\\n|\\r|\\n)/g, '<br>');
            if (error.response) { // server error catched by axios
                responseTable.innerHTML = `
                <div class="uk-alert-danger">
                    <b>${error.response.statusText}: </b> <br/>
                    ${message}
                </div>
                `;
            } else { // js error
                responseTable.innerHTML = error.message;
            }
        } finally {
            responseTable.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
            spinner.hidden = true;
        }
    };

    const renderResponseTable = (response) => {
        const {
            success,
            ttl,
            envid,
            logs
        } = response;

        spinner.hidden = false;
        if (success) {
            downloadBtn.hidden = false;
            downloadBtn.setAttribute('href', `api/env/${envid}`);
            const date = new Date(Date.now() + ttl);
            const format = `${date.getHours()}:${date.getMinutes()}:${date.getSeconds()}`;
            timeToLiveNode.innerHTML = '&nbsp;&nbsp; Expire Time: ' + format;
            timeToLiveNode.hidden = false;
        }
    
        const html = [];
        html.push("<table class='uk-accordion-content uk-table uk-table-small uk-table-responsive uk-table-striped uk-table-hover'>");
        html.push("<tbody>");
        const icons = {
            Info: '<span  uk-icon="icon: info"></span>',
            Warn: '<span  uk-icon="icon: warning"></span>',
            Error: '<span uk-icon="icon: warning"></span>'
        };
        for (const log of logs) {
            html.push(`
                <tr>
                    <td>${icons[log.type]}</td>
                    <td>${log.message}</td>
                </tr>`
            );
        }
        html.push("</tbody>");
        html.push("</table>");
        responseTable.innerHTML = html.join("\n");
    }

    const renderTargetDropdown = () => {
        const dropdown = document.getElementById('target');
        dropdown.innerHTML = '';
        for (let i = minSupportedJDK; i <= maxSupportedJDK; i++) {
            dropdown.innerHTML += `<option value="${i}">JDK ${i}</option>`;
        };
    }

    const renderFeaturesTable = async() => {
        featuresTable.innerHTML = '';
        const versions = Object.keys(features).sort((a, b) => {
            return features[a] - features[b];
        });
        for (const feature of versions) {
            form.features.push(feature);
            featuresTable.innerHTML +=  `
                <tr class="pointer">
                    <td>${feature}</td>
                    <td>Target < JDK ${features[feature]}</td>
                    <td>
                        <input
                            class="uk-checkbox"
                            type="checkbox"
                            name="${feature}"
                            id="${feature}"
                            data-feature="${feature}"
                            checked>
                    </td>
                </tr>
            `;
        }
    }

    const renderFilesTable = () => {
        filesTable.innerHTML = '';
        let i = 0;
        for (const file of form.files) {
            let className = '';
            let tooltip = 'Click to remove';
            if (form.files.filter(e => e.name === file.name).length > 1) {
                className = 'uk-alert-danger';
                tooltip = 'Duplicated file ! Click to remove';
            }
            filesTable.innerHTML += `
                <tr data-index="${i++}" class="pointer ${className}" uk-tooltip="${tooltip}">
                    <td>${file.name}</td>
                </tr>
            `;
        };
    };

    const addEventListeners = () => {
        submitBtn.addEventListener('click', submitForm);

        const fileInput = document.querySelector('input[type=file]');
        fileInput.addEventListener('change', () => {
            form.files = [...form.files, ...fileInput.files];
            renderFilesTable();
            checkForm();
        });

        // event delegation on tables instead of multiple listeners

        filesTable.addEventListener('click', (e) => {
            const target = e.target || e.srcElement;
            const row = target.parentNode;
            const index = row.getAttribute('data-index');
            form.files.splice(index, 1);
            row.remove();
            renderFilesTable();
            checkForm();
        });

        featuresTable.addEventListener('click', (e) => {
            let target = e.target || e.srcElement;
            if (target.tagName.toLowerCase() !== 'input') {
                target = target.parentNode.querySelector('input');
                target.checked = !target.checked;
            }
            if (target.checked) {
                if (!form.features.some(e => e === target.id)) {
                    form.features.push(target.id);
                }
            } else {
                form.features = form.features.filter(e => e !== target.id);
            }
            if (form.features.length === 0) {
                canSubmit = false;
            }
            checkForm();
        }, { capture: true });
    };

    renderTargetDropdown();
    renderFeaturesTable();
    addEventListeners();

})().catch(error => {
    console.log(error);
});
